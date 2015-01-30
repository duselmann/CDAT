package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.DeadLetterLogger;
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
import akka.actor.DeadLetter;
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
	
	private static final String SESSION = "session";
	
	public static final FiniteDuration MILLIS    = Duration.create(100, "milliseconds");
	public static final FiniteDuration SECOND    = Duration.create(  1, "second");
	public static final FiniteDuration HALF_MIN  = Duration.create( 30, "seconds");
	public static final FiniteDuration MINUTE    = Duration.create(  1, "minute");
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
	// TODO abandoned sessions should be closed and disposed cleanly
	
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

	private final ActorRef deadLetterLogger;
	
	/**
	 *  private constructor for singleton pattern
	 *  because it is a thread pool system where 
	 *  multiple instances is incongruous.
	 */  
	private SCManager() {
        // Create the 'CDAT' akka actor system
        workerPool = ActorSystem.create("CDAT"); // TODO doc structure

        naming = workerPool.actorOf( Props.create(Naming.class, new Object[0]), "Naming");

        // listen to all dead letters for custom logging
        deadLetterLogger = workerPool.actorOf( Props.create(DeadLetterLogger.class, new Object[0]), "DeadLetterLogger");
        workerPool.eventStream().subscribe(deadLetterLogger, DeadLetter.class);
	}

	/**
	 * This is the helper session accessor. However, since it has a side effect of creating
	 * an instance if there is not one, it is not getSession.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * I think this is as thread safe a we can be. There be a race condition or optimization within the if blocks
	 * 
	 * @return a thread safe specific session
	 */
	ActorRef session() {
		if (session.get() == null) {
			String sessionName = createNameFromLabel(SESSION);
	        ActorRef sessionRef = workerPool.actorOf(
	        		Props.create(Session.class, new Object[0]), sessionName);
			if (session.get() == null) {
				session.set(sessionRef);
			}
		}

		return session.get();
	}
	// TODO abandoned sessions and workers should be closed and disposed cleanly
	// TODO what I mean is that upon session exiting scope in the container it should dispose of its current workers
	// TODO we should also reset autostart to what ever default we desire
	
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
			// this stops blocking as soon as a result is returned. this should be plenty of time
			Object result = Await.result(future, SECOND); // TODO make configurable
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
	 * <p>The onComplete message contains the worker.name and onComplete:done
	 * 
	 * 
	 * @param workerLabel String workerLabel - IMPORTANT: Names must be unique. This returns a name 
	 * 					from the suggested label. Names are used to ensure thread worker isolation from the spawning code
	 * 					Users MUST maintain a reference to the new name to submit actions to the worker.
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @param onComplete the callback when the worker has completed.
	 * @return a future for interacting, if necessary, with the the response
	 */
	public Future<Object> addWorker(String workerLabel, DataPipe pipe, Callback onComplete) {
		AddWorkerMessage msg = createAddWorkerMessage(workerLabel, pipe);
		// this will stop blocking as soon as the worker finishes and returns an onComplete message
		Future<Object> response = Patterns.ask(session(), msg, new Timeout(DAY)); // TODO make configurable
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
		return send(workerName,message,new Timeout(HALF_MIN)); // TODO make configurable
	}
	/**
	 * This is a similar method with a custom wait time.
	 * 
	 * @see SCManager.send(String workerName, Message message)
	 * 
	 * @param workerName the worker name to respond to the message (must be the unique name return from addWorker)
	 * @param message the message the worker will receive. status or control
	 * @param waitTime custom time to wait if you have a longer possible wait time
	 * @return a future that contains a Message response from the worker upon completion or exception
	 */
	public Future<Object> send(String workerName, Message message, Timeout waitTime) {
		message = Message.extend(message, Naming.WORKER_NAME, workerName);
	    return Patterns.ask(session(), message, waitTime);
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
	 * <p>Enumerated control message</p>
	 * <p>A convenience send for Control enum standard messages. It saves the user from requiring
	 * construction of messages for standard messages in the Control and Status enum classess.
	 * </p>
	 * <p>Example: manager.send(workerName, Control.start);
	 * </p>
	 * @param workerName the unique work name to receive the message
	 * @param ctrl an instance of the Control enum name
	 * @param callback the action to take when the worker completes
	 * @return a future containing a return message as to how the action executed
	 */
	public Future<Object> send(String workerName, Control ctrl, final Callback callback) {
		Future<Object> response = send(workerName, ctrl);
		wrapCallback(response, callback);
		return response;
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
	 * TODO need a force/wait versions of this for the option to wait for workers to finish
	 * TODO investigate a means to have session NOT able to call this - not likely - I would like only the container to call this on shutdown
	 */
	public void shutdown() {
		workerPool.scheduler().scheduleOnce( HALF_MIN, // TODO make configurable
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
	
	
	/**
	 * Initial attempt to set the session to automatically start workers.
	 * This could use some improvement.
	 * 
	 * @param value
	 */
	// TODO when a session is 'done' it should reset the autoStart state to DEFAULT.
	// TODO make autoStart DEFAULT state configurable
	public void setAutoStart(boolean value) {
		Message msg = Message.create(Session.AUTOSTART, value);
		session().tell(msg, ActorRef.noSender());
	}
}
