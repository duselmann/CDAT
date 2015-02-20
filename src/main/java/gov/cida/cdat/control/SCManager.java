package gov.cida.cdat.control;

import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.service.DeadLetterLogger;
import gov.cida.cdat.service.Naming;
import gov.cida.cdat.service.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;


/**
 * <p>This is the top level manager for all submitted ETL query workers. Under the covers it uses AKKA to ensure
 * thread safe processing. Each worker will perform an ETL action via the pipe construct.
 * </p>
 * <p>It was asked if it would be possible to submit a blocking message. this is against the AKKA model and is not recommended.
 * Could it be possible to make status/control on submitted messages? They are not workers (AKKA Actors); hence, it is unlikely.
 * </p>
 * <p>Here is a sample worker hierarchy.
 * </p>
 * <pre>
 *             CDAT
 *          /    |   \
 *        /      |     \
 *      /        |       \
 * SESSION-1  SESSION-5  SESSION-9
 *    |          |        |      \
 * worker-2    job-1      |        \
 *                    siteCount-3   siteSelect-3
 * </pre>
 * 
 * @author duselman
 */
public class SCManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 *  This is used to send an auto start message to the session..
	 */
	public static final Object AUTOSTART = "AUTOSTART";
	/**
	 * This is used to send to the session a message for the session rather than the worker
	 */
	public static final String SESSION   = "SESSION";

	private static final String TOKEN    = "012345678910";  // TODO make configurable
	
	/**
	 *  singleton pattern, each user session will have a session worker
	 */
	static final SCManager instance;
	static {
		instance = new SCManager();
	}
	/**
	 * <p>A convenience method to access the instance since
	 * calling openSession twice seems counter intuitive
	 * </p>
	 * 
	 * @return the current SC manager instance
	 */
	public static SCManager instance() {
		return instance;
	}
	/**
	 * <p>"Creates" a session instance for thread safe actions.
	 * </p>
	 * <p>Actually, the session() method creates the session if none exists.
	 * This method just looks nice as in the following example.
	 * </p>
	 * <p>Example:</p>
	 * <pre>
	 *	SCManager session = SCManager.open();
	 *	try {
	 *		Worker helloWorld = new Worker() {
	 *			public boolean process() {
	 *				System.out.println("Hello World");
	 *				return false; // Answers the question: Is there more?
	 *			}
	 *		};
	 *		String name = session.addWorker("HelloWorld", helloWorld);
	 *		session.send(name, Control.Start);
	 *	} finally {
	 *		session.close();
	 *	}		
	 * </pre>
	 * @return the current SC manager instance
	 */
	public static SCManager open() {
		return instance();
	}
	public static SCManager open(String adminToken) {
		instance().token.set(adminToken);
		return instance();
	}
	
	/**
	 * When done with the session call close to release the session when all jobs complete.
	 */
	public void close() {
		close(false);
	}
	/**
	 * When done with the session call close to release the session
	 */
	public void close(boolean force) {
		try {
			// this tells the session to stop processing workers
			setAutoStart(false); // this is for completeness // TODO make configurable
			
			if (force) {
				// hard stop
				workerPool.stop(session());
			} else {
				// soft stop
				Message stop = Message.create(Control.Stop, SCManager.SESSION);
				session().tell(stop, ActorRef.noSender());
			}
		} finally {
			// this removes the session from the thread 
			// a new one will be issued upon the next request
			session.remove();
			token.remove();
		}
	}
	
	/**
	 * This is the session worker instance. Each thread is given its own instance.
	 */
	private ThreadLocal<ActorRef> session = new ThreadLocal<ActorRef>();
	private ThreadLocal<String>   token   = new ThreadLocal<String>();
	
	/**
	 *  like a thread pool but workers are not tied to a thread
	 *  Package access for testing.
	 */
	final ActorSystem workerPool;
	
	/**
	 * This is a special worker for naming to ensure that threads do not compete for unique names
	 */
	private final ActorRef naming;

	/**
	 * This worker is used to log all dead letters. It is only active while in TRACE mode.
	 */
	private final ActorRef deadLetterLogger;
	
	/**
	 *  private constructor for singleton pattern because it is a 
	 *  thread pool system where multiple instances would be incongruous.
	 */  
	private SCManager() {
        // Create the 'CDAT' akka actor system
        workerPool = ActorSystem.create("CDAT");

        naming = workerPool.actorOf( Props.create(Naming.class, new Object[0]), "Naming");

        // listen to all dead letters for custom logging
        deadLetterLogger = workerPool.actorOf( Props.create(DeadLetterLogger.class, new Object[0]), "DeadLetterLogger");
        workerPool.eventStream().subscribe(deadLetterLogger, DeadLetter.class);
	}

	/**
	 * For thread testing and AKKA direct access custom implementation.
	 * @return the name of the session the current manager is using.
	 */
	String sessionName() {
		return session().path().name();
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
	// TODO abandoned sessions should be stopped
	
	ActorRef session(String workerName) {
		// if we are an admin session
		if (TOKEN.equals(token.get())) {
			ActorRef session = null;

            try {
    			// find the worker by name
            	String path = "akka://CDAT/user/*/"+workerName;
    			logger.trace("searching for session for worker {}", path);
    			Future<ActorRef> future = workerPool.actorSelection(path).resolveOne(Time.SECOND.duration);
				ActorRef worker  = Await.result(future, Time.SECOND.duration);
				logger.trace("found worker {}", worker.path());
				// find the session the worker is running
				future  = workerPool.actorSelection(worker.path().parent()).resolveOne(Time.SECOND.duration);
				session = Await.result(future, Time.SECOND.duration);
				logger.trace("found session {} for worker {}", session.path(), worker.path());
				
			} catch (Exception e) {
				e.printStackTrace();
				// since we want to return null if not found we need not do anything else
			}
			return session;
		}
		return session();
	}
	
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
		Future<Object> future = Patterns.ask(naming, label, Time.MS.asTimeout());
		try {
			logger.trace("waiting for name from label '{}'", label);
			// this stops blocking as soon as a result is returned. this should be plenty of time
			Object result = Await.result(future, Time.SECOND.duration); // TODO make configurable
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
	public String addWorker(String workerLabel, Worker worker) {
		AddWorkerMessage msg = createAddWorkerMessage(workerLabel, worker);
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
	 */
	public void addWorker(String workerLabel, Worker worker, Callback onComplete) {
		AddWorkerMessage msg = createAddWorkerMessage(workerLabel, worker);
		// this will stop blocking as soon as the worker finishes and returns an onComplete message
		Future<Object> future = Patterns.ask(session(), msg, Time.DAY.asTimeout()); // TODO make configurable
		wrapCallback(future, workerPool.dispatcher(), onComplete);
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
	AddWorkerMessage createAddWorkerMessage(String workerLabel, Worker worker) {
		String workerName = createNameFromLabel(workerLabel);
		AddWorkerMessage msg = AddWorkerMessage.create(workerName, worker);
		return msg;
	}
	
	// TODO abstract the AKKA and SCALA frameworks out with wrapper classes. see Callback
	/**
	 * <p>Sends a message to the given worker name. It swallows the scala future to
	 * abstract the AKKA framework away from callers. 
	 * </p>
	 * Example:<br>
	 * SCManager manager = SCManager.get();<br>
	 * Message message = Message.create(Control.start);<br>
	 * manager.send(workerName, message);<br>
	 * 
	 * @param workerName the worker name to respond to the message (must be the unique name return from addWorker)
	 * @param message the message the worker will receive. status or control
	 */
	public void send(String workerName, Message message) {
		sendWithFuture(workerName,message);
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
	Future<Object> sendWithFuture(String workerName, Message message) {
		return send(workerName,message,Time.HALF_MINUTE); // TODO make configurable
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
	Future<Object> send(String workerName, Message message, Time waitTime) {
		message = message.extend(Naming.WORKER_NAME, workerName);
	    return Patterns.ask(session(workerName), message, waitTime.asTimeout());
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
	 * @see SCManager.send(String workerName, Message message)
	 */
	public void send(String workerName, Control ctrl) {
		Message msg = Message.create(ctrl);
		send(workerName, msg);
	}
	public Message request(String workerName, Message msg) {
		Future<Object> future = sendWithFuture(workerName, msg);
		Object result = null;
		try {
			result = Await.result(future, Time.SECOND.duration); // TODO make configure
		} catch (Exception e) {
			result = Message.create("error",e.getMessage());
		}
		return (Message)result;
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
	public void send(String workerName, Control ctrl, final Callback callback) {
		send(workerName, Message.create(ctrl), callback);
	}
	/**
	 * <p>Enumerated status message (blocking of a limited time)</p>
	 * <p>A convenience send for Control enum standard messages. It saves the user from requiring
	 * construction of messages for standard messages in the Control and Status enum classes.
	 * </p>
	 * <p>Example: manager.send(workerName, Status.isAlive);
	 * </p>
	 * @param workerName the unique work name to receive the message
	 * @param ctrl an instance of the Status enum name
	 * @return a future containing a return message as to how the action executed
	 * @see SCManager.send(String workerName, Message message)
	 */
	public Message send(String workerName, Status status) {
		Message msg = Message.create(status);
		return request(workerName, msg);
	}
	/**
	 * <p>Enumerated status message (non-blocking)</p>
	 * <p>A convenience send for Control enum standard messages. It saves the user from requiring
	 * construction of messages for standard messages in the Control and Status enum classes.
	 * </p>
	 * <p>Example: manager.send(workerName, Status.isAlive);
	 * </p>
	 * @param workerName the unique work name to receive the message
	 * @param ctrl an instance of the Status enum name
	 * @param callback the method to call when the status is processed
	 * @see SCManager.send(String workerName, Message message)
	 */
	public void send(String workerName, Status status, final Callback callback) {
		send(workerName, Message.create(status), callback);
	}
	public void send(String workerName, Message msg, final Callback callback) {
		Future<Object> future = sendWithFuture(workerName, msg);
		wrapCallback(future, workerPool.dispatcher(), callback);
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
	static void wrapCallback(Future<Object> response, ExecutionContext context, final Callback callback) {
		if (callback == null) {
			return;
		}
//		logger.trace("wrapping onComplete with typed cast (Message) response {}", callback);
		
		// this is wrapper in order to allow the user a typed Message callback
		OnComplete<Object> wrapper = new OnComplete<Object>() {
			public void onComplete(Throwable t, Object response) throws Throwable {
				callback.onComplete(t, (Message)response);
			}
		};
		
	    response.onComplete(wrapper, context);
	}
	
	/**
	 * <p>Issues a shutdown on the AKKA concurrency framework. This should not be called from a user session.</p>
	 * 
	 * <p>It is intended to be called by the application upon container shutdown to ensure all workers are closed.</p>
	 * 
	 * TODO need a force/wait versions of this for the option to wait for workers to finish
	 * TODO investigate a means to have session NOT able to call this - not likely - I would like only the container to call this on shutdown
	 */
	public static void shutdown() {
		final ActorSystem workerPool = instance().workerPool;
		final Logger logger = LoggerFactory.getLogger(instance().getClass());
		
		workerPool.scheduler().scheduleOnce( Time.SECOND.duration, // TODO make configurable
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
	// TODO make autoStart DEFAULT state configurable
	public SCManager setAutoStart(boolean value) {
		Message msg = Message.create(SCManager.AUTOSTART, value);
		session().tell(msg, ActorRef.noSender());
		return this; // method chaining
	}
	
	/**
	 * <p>Submits an onComplete message and waits for it to return for tomcat request thread
	 * waiting/blocking. If the tomcat thread completes before the worker then the streams
	 * tomcat manages will be closed prematurely and the user will not receive data.
	 * </p>
	 * <p>After the given wait time it will wait up to another 10 seconds for possible unforeseeable
	 * time variation. It will wait a maximum of 100 times for 100 ms each.
	 * </p>
	 * @param workerName the worker to wait for complete
	 * @param waitTime how many milliseconds to wait
	 * @return the actual time waited
	 */
	public long waitForComplete(String workerName, long waitTime) {
		return waitForComplete(workerName, waitTime, 100);
	}
	/**
	 * <p>Submits an onComplete message and waits for it to return for tomcat request thread
	 * waiting/blocking. If the tomcat thread completes before the worker then the streams
	 * tomcat manages will be closed prematurely and the user will not receive data.
	 * </p>
	 * <p>After the given wait time it will wait up to another 100 times the subsequaent wait
	 * for possible unforeseeable time variation. If you have worker that runs for 20 min then
	 * an appropriate subsequent wait might be one minute or thirty seconds. However, for short
	 * jobs a much shorter time my be ideal.
	 * </p>
	 * @param workerName the worker to wait for complete
	 * @param initialWait how many milliseconds to wait initially
	 * @param subsequentWait how many milliseconds to wait after initial wait
	 * @return the actual time waited
	 */
	public long waitForComplete(String workerName, long initialWait, long subsequentWait) {
		logger.trace("waiting {}ms for {} to complete", initialWait, workerName);

		// capture the start time for duration math
		long start = Time.now();
		// this is an repository for the complete signal
		final Message[] isComplete = new Message[1];
		
		SCManager.instance().send(workerName, Control.onComplete, new Callback(){
			@Override
			public void onComplete(Throwable t, Message signal) {
				isComplete[0] = signal;
			}
		});
		
		// initially wait for the expected job length
		try {
			Thread.sleep(initialWait);
		} catch (InterruptedException e) {
			logger.trace("initial wait for {} interrupted, waited for {} ms", workerName, Time.duration(start));
		}

		// then wait for it to complete with a smaller cycle but not forever
		int count=100;
		while (null==isComplete[0] && count++ < 100) {
			try {
				Thread.sleep(subsequentWait);
			} catch (InterruptedException e) {}
		}
		
		// return the duration to the call in case they are interested
		long duration = Time.duration(start);
		logger.trace("waited {}ms for {} to complete", duration, workerName);
		return duration;
	}
}
