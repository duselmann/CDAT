package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.PipeStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;

// TODO better name?
// TODO impl start/stop fail return true/false and the Actor supervisor



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
public class SCManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 *  singleton pattern, each user session will have a session worker
	 */
	static final SCManager instance;
	static {
		instance = new SCManager();
	}
	public static SCManager session() {
		return instance;
	}
	// TODO one instance per application, then need a session level actor for user workers
	// TODO session should be disposable and light weight
	// TODO abandoned sessions and workers should be closed and disposed cleanly
	
	
	/**
	 *  like a thread pool but workers are not tied to a thread
	 */
	final ActorSystem workerPool;
	/**
	 *  something that does work like an ETL, Query, or Session
	 */
	final Registry workers;

	
	/**
	 *  private constructor for singleton
	 *  because it is a thread pool system where 
	 *  multiple instances is incongruous.
	 */  
	private SCManager() {
		workers = new Registry();
        // Create the 'CDAT' akka actor system
        workerPool = ActorSystem.create("CDAT"); // TODO doc structure
        // TODO test that container is same instance or new instance - we need know
        // TODO need a session system for multiple users
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
	 * @param workerName String name - IMPORTANT: Names must be unique this returns a new name 
	 * 					if the is not then it return a new name that is unique.
	 * 					you MUST maintain a reference to the new name to submit actions to your worker
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @return the new unique string that is used to send messages to submitted pipe
	 */
	public String addWorker(String workerName, PipeStream pipe) { // TODO QueryWorker name change?
        // Create the akka service actor
        final ActorRef actor = workerPool.actorOf(Props.create(
        		gov.cida.cdat.service.combined.Service.class, pipe), workerName);
        // returns a unique name from the given name
        return workers.put(workerName, actor); // TODO dispose of actor?
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
        // send a message
		ActorRef worker = workers.get(workerName);		
		if (worker == null) {
			return null; // TODO decide if this is appropriate and sufficient
		}
		
	    return Patterns.ask(worker, message, 1000); // TODO config timeout , doc the logic
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
	 * @param onComplete
	 * @return
	 */
	public Future<Object> send(String workerName, Control ctrl, final Callback onComplete) {
		Future<Object> response = send(workerName, ctrl);
		OnComplete<Object> wrapper = new OnComplete<Object>() {
			public void onComplete(Throwable t, Object repsonse) throws Throwable {
				onComplete.onComplete(t, (Message)repsonse);
			}
		};
	    response.onComplete(wrapper, workerPool.dispatcher());
		return response;
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
		workerPool.scheduler().scheduleOnce( FiniteDuration.create(20, "seconds"),
			new Runnable() {
				@Override
				public void run() {
				    logger.info("shutdown");
				    workerPool.shutdown();
				    logger.info("awaitTermination");
				    workerPool.awaitTermination();

				}
		}, workerPool.dispatcher());		
	}
}
