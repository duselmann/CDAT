package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.Naming;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;


/**
 * <p>This is the top level manager for all submitted ETL query workers. Under the covers it uses AKKA to ensure
 * thread safe processing. Each worker will perform an ETL action via the pipe construct.
 * </p>
 * <p>It was asked if it would be possible to submit a blocking message. this is against the AKKA model and is not recommended.
 * Could it be possible to make status/control on submitted messages? They are not workers (AKKA Actors); hence, it is unlikely.
 * </p>
 * @author duselman
 *
 */
//TODO better name?
public class SCManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	// TODO maybe these should be Durations rather than Timeout
	public static final FiniteDuration MILLIS    = Duration.create(100, "milliseconds");
	public static final FiniteDuration SECOND    = Duration.create(  1, "second");
	public static final FiniteDuration HALF_MIN  = Duration.create( 30, "seconds");
	public static final FiniteDuration HOUR      = Duration.create(  1, "hour");
	public static final FiniteDuration DAY       = Duration.create(  1, "day");	
	
	/**
	 *  singleton pattern, each user session will have a session worker
	 */
	static final SCManager instance;
	static {
		instance = new SCManager();
	}
	public static SCManager instance() {
		return instance;
	}
	// TODO one instance per application, then need a session level actor for user workers
	// TODO session should be disposable and light weight
	// TODO abandoned sessions and workers should be closed and disposed cleanly
	
	// TODO is this container safe - does each session live on a thread?
	// tests suggest it is safe, however there is no guarantee that subsequent tasks run on the same thread
	private ThreadLocal<ActorRef> session = new ThreadLocal<ActorRef>();
	
	/**
	 *  like a thread pool but workers are not tied to a thread
	 */
	private final ActorSystem workerPool;
	
	/**
	 * This is a special worker for naming to ensure that threads do not compete for unique names
	 */
	private final ActorRef naming;

	
	/**
	 *  private constructor for singleton pattern
	 *  because it is a thread pool system where 
	 *  multiple instances is incongruous.
	 */  
	private SCManager() {
        // Create the 'CDAT' akka actor system
        workerPool = ActorSystem.create("CDAT"); // TODO doc structure
        // TODO test that when in tomcat that it same instance or new instance - we need know
        naming = workerPool.actorOf( Props.create(Naming.class, new Object[0]), "Naming");
	}

	/**
	 * This is the helper session accessor. However, since it has a side effect of creating
	 * an instance if there is not one, it is not getSession.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @return a thread safe specific session
	 */
	// TODO is this thread safe? can there be a race condition within the if block
	ActorRef session() {
		if (session.get() == null) {
			String sessionName = createNameFromLabel("session");
	        ActorRef sessionRef = workerPool.actorOf(
	        		Props.create(Session.class, new Object[0]), sessionName);
	        session.set(sessionRef);
		}

		return session.get();
	}
	// TODO abandoned sessions and workers should be closed and disposed cleanly
	
	/**
	 * This helper method messages the Naming worker to ensure unique names.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * For example:
	 * String nameA = createNameFromLabel("worker");
	 * String nameB = createNameFromLabel("other");
	 * String nameC = createNameFromLabel("worker");
	 * // then nameA would be worker-1 while nameC could be worker-2 
	 * // however nameB could be other-1
	 * 
	 * @param label the suggested name from the user
	 * @return a unique name for the given label
	 */
	String createNameFromLabel(String label) {
		String name = label;
		
		logger.trace("sending message to creating name from label '{}'", label);
		Future<Object> future = Patterns.ask(naming, label, new Timeout(MILLIS));
		try {
			logger.trace("waiting for name from label '{}'", label);
			Object result = Await.result(future, MILLIS);
			if (result instanceof Message) {
				name = ((Message)result).get(Naming.WORKER_NAME);
			}
		} catch (Exception e) {
			logger.error("error creating name from label '{}'", label, e);
			// TODO What do we do now? Not sure
		}
		
		return name;
	}
	
	
	/**
	 * <p>submits a worker for an ETL stream (pipe), 
	 * </p>
	 * Example:<br>
	 * String final NWIS_SEDIMENT = "Fetch sediment from NWIS";<br>
	 * SCManager manager = SCManager.get();<br>
	 * String workerName = manager.addWorker(NWIS_SEDIMENT, nwisRequest);<br>
	 * <p> The name now equals "Fetch sediment from NWIS", or "Fetch 'data' from NWIS-01", etc.
	 * </p>
	 * @param workerLabel String workerLabel - IMPORTANT: Names must be unique. This returns a name 
	 * 					from the suggested label. Names are used to ensure thread worker isolation from the spawning code
	 * 					Users MUST maintain a reference to the new name to submit actions to the worker.
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @return the new unique name string that is used to send messages to submitted pipe
	 */
	public String addWorker(String workerLabel, DataPipe pipe) { 
		AddWorkerMessage msg = createAddWorkerMessage(workerLabel, pipe);
		session().tell(msg, ActorRef.noSender());
		return msg.getName();
	}
	
	/**
	 * <p>submits a worker for an ETL stream (pipe) with an onComplete callback.
	 * </p>
	 * Example:<pre>
	 * String final NWIS_SEDIMENT = "Fetch sediment from NWIS";
	 * SCManager manager = SCManager.get();
	 * Future&lt;Object&gt; future = manager.addWorker(NWIS_SEDIMENT, nwisRequest, new Callback(){
	 *       public void onComplete(Throwable t, Message response) {
	 *           doSomething(response);		
	 *       }
	 *   });
	 *   </pre>
	 * <p> The name now equals "Fetch sediment from NWIS", or "Fetch 'data' from NWIS-01", etc.
	 * </p>
	 * @param workerLabel String workerLabel - IMPORTANT: Names must be unique. This returns a name 
	 * 					from the suggested label. Names are used to ensure thread worker isolation from the spawning code
	 * 					Users MUST maintain a reference to the new name to submit actions to the worker.
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @param onComplete the callback when the worker has completed.
	 * @return a future for interacting, if necessary, with the the response
	 */
	// TODO test that the onComplete message contains the worker.name
	public Future<Object> addWorker(String workerLabel, DataPipe pipe, Callback onComplete) {
		AddWorkerMessage msg = createAddWorkerMessage(workerLabel, pipe);		
		Future<Object> response = Patterns.ask(session(), msg, 1000);
		wrapCallback(response, onComplete);
		return response;
	}

	/**
	 * Helper method for creating a message to add new worker.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @param workerLabel the suggested name for the worker, the unique name will be in the message
	 * @param pipe the pipe to execute on the new worker
	 * @return the message for a new worker
	 */
	AddWorkerMessage createAddWorkerMessage(String workerLabel, DataPipe pipe) {
		String workerName = createNameFromLabel(workerLabel);
		AddWorkerMessage msg = AddWorkerMessage.create(workerName, new PipeWorker(pipe));
		return msg;
	}
	
	/**
	 * <p>Sends a message to the given worker name and returns the future to obtain the results.
	 * It returns a result of the message not the data returned by the pipe. The pipe is
	 * constructed to send the data to the destination: the user http response, a database table, etc.
	 * </p>
	 * Example:<br>
	 * SCManager manager = SCManager.get();<br>
	 * Message message = Message.create(Control.start);<br>
	 * Future<Object> nwisMessageResult = manager.send(workerName, message);<br>
	 * 
	 * <p>The future will return a message containing a key, 'start', with a string value like "True", "Failed to start because of xyz exception", etc 
	 * </p>
	 * @param workerName the worker name to respond to the message (must be the unique name return from addWorker)
	 * @param message the message the worker will receive. status or control
	 * @return a future that contains a Message response from the worker upon completion or exception
	 */
	public Future<Object> send(String workerName, Message message) {
		message = Message.extend(message, Naming.WORKER_NAME, workerName);
	    return Patterns.ask(session(), message, 50000);
		// TODO should this one return a future
	    // TODO config timeout and create a method sig for custom timeout
	}
	
	/**
	 * <p>Enumerated control message</p>
	 * <p>A convenience send for Control enum standard messages. It saves the user from requiring
	 * construction of messages for standard messages in the Control and Status enum classess.
	 * </p>
	 * <p>Example: manager.send(workerName, Control.start);
	 * </p>
	 * @param workerName the unique work name to receive the message
	 * @param ctrl an instance of the Control enum name
	 * @return a future containing a return message as to how the action executed
	 * @see SCManager.send(String workerName, Message message)
	 */
	public Future<Object> send(String workerName, Control ctrl) {
		Message msg = Message.create(ctrl);
		return send(workerName, msg);
	}
	/**
	 * <p>Enumerated status message</p>
	 * <p>A convenience send for Control enum standard messages. It saves the user from requiring
	 * construction of messages for standard messages in the Control and Status enum classess.
	 * </p>
	 * <p>Example: manager.send(workerName, Status.isAlive);
	 * </p>
	 * @param workerName the unique work name to receive the message
	 * @param ctrl an instance of the Status enum name
	 * @return a future containing a return message as to how the action executed
	 * @see SCManager.send(String workerName, Message message)
	 */
	public Future<Object> send(String workerName, Status status) {
		Message msg = Message.create(status);
		return send(workerName, msg);
	}
	
	
	/**
	 * <p>Initial testing of a callback on a future. It is wrapped like this to ensure the user
	 * receives a instance of Message instance rather then a plain Object. 
	 * </p>
	 * <p>TODO do we eventually want all message to have a callback option?
	 * </p>
	 * <p>TODO Example
	 * </p>
	 * @param workerName
	 * @param ctrl
	 * @param callback
	 * @return
	 */
	public Future<Object> send(String workerName, Control ctrl, final Callback callback) {		
		Future<Object> response = send(workerName, ctrl);
		wrapCallback(response, callback);
		return response;
	}
	
	/**
	 * Helper method that wraps the callback into the AKKA Object general
	 * to the CDAT specific Message based callback.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @param response the future to add append the callback
	 * @param callback the callback instance to attach to the Future.onComplete
	 */
	void wrapCallback(Future<Object> response, final Callback callback) {
		if (callback == null) {
			return;
		}
		logger.trace("wrapping onComplete with typed cast (Message) response {}", callback);
		
		// this is wrapper in order to allow the user a typed Message callback
		OnComplete<Object> wrapper = new OnComplete<Object>() {
			public void onComplete(Throwable t, Object response) throws Throwable {
				callback.onComplete(t, (Message)response);
			}
		};
		
	    response.onComplete(wrapper, workerPool.dispatcher());
	}
	
	/**
	 * <p>Issues a shutdown on the AKKA concurrency framework. This should not be called from a user session.</p>
	 * 
	 * <p>It is intended to be called by the application upon container shutdown to ensure all workers are closed.</p>
	 * 
	 * <p>TODO need a force on this and a wait for workers to finish
	 * </p>
	 * <p>TODO investigate a means to have session not able to call this - not likely
	 * </p>
	 */
	public void shutdown() {
		workerPool.scheduler().scheduleOnce( HALF_MIN,
			new Runnable() {
				@Override
				public void run() {
					// isTerminated had be deprecated for watch
				    logger.info("shutdown {}", workerPool.isTerminated());
				    workerPool.shutdown();
				    logger.info("shutdown {}", workerPool.isTerminated());
				    logger.info("awaitTermination {}", workerPool.isTerminated());
				    workerPool.awaitTermination();
				    logger.info("awaitTermination {}", workerPool.isTerminated());
				}
		}, workerPool.dispatcher());		
	}
}
