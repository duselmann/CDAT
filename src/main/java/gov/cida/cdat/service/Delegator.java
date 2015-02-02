package gov.cida.cdat.service;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;


/**
 * This is the 'threaded' delegate to run workers.
 * Implementation should extend Worker with specific behavior.
 * 
 * @author duselman
 *
 */
public class Delegator extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String  PROCESS_MORE = "process.more";
	private static final Message CONTINUE     = Message.create(PROCESS_MORE);

	/**
	 * This is the unique name given when the delegate worker.
	 * It is used for both the AKKA system and this framework Registry.
	 */
	private final String name;
		
	/**
	 * The implementation to delegate work.
	 * 
	 * The AKKA API recommends this pattern in order to ensure that
	 * the java 'this' is not used. The AKKA API uses self() instead
	 * to ensure thread safe actions on the instance.
	 */
	private final Worker worker;
	
	/**
	 * The current state of this instance. Has it started, stopped, etc.
	 * @see Status
	 */
	private Status status;
	
	/**
	 * Holds the instance of the latest exception. It is used to communicate to the worker owner.
	 */
	private Exception lastError;
	
	/**
	 * If the worker should start automatically then this should be set to true on worker create.
	 */
	private boolean autoStart;

	/**
	 * The list of Actors interested in the completion of this delegate of work.
	 */
	private final List<ActorRef> onComplete = new LinkedList<ActorRef>();
	
	public Delegator(AddWorkerMessage worker) {
		this.name      = worker.getName();
		this.worker    = worker.getWorker();
		this.autoStart = worker.isAutoStart();
		setStatus(Status.isNew);
		logger.trace("new Delegator for worker:{} with autostart:{}", name, autoStart);
	}
	
	
	/**
	 *  AKKA framework generic messages receiver
	 */
	public void onReceive(Object msg) throws Exception {
		logger.trace("Delegator recieved message {}", msg);
		if (msg instanceof Message) {
			onReceive((Message)msg);
			return;
		}
	}
	/**
	 * CDAT framework specific message receiver. It brokers the messages.
	 * Start will commence the worker.
	 * Stop will issue a stop request to the delegate after calling done on the worker.
	 * onComplete will maintain the sender in a list that want a callback on finishing
	 * 
	 * TODO need to pass messages to the worker for custom actions.
	 * 
	 * @param msg
	 * @throws Exception
	 */
	public void onReceive(Message msg) throws Exception {
		Message response = null;
				
		// handle the start message
		if (msg.contains(Control.Start)) {
			logger.trace("Delegator recieved message {}", Control.Start);
			start();

		// handle the stop message
		} else if (msg.contains(Control.Stop)) {
			logger.trace("Delegator recieved message {}",  Control.Stop);
			try {
				done( msg.get(Control.Stop) );
			} finally {
				context().stop( self() );
			}
			response = Message.create(Control.Stop, true);
		
		// handle continue processing requests
		} else if (status!=null && status.is(Status.isStarted)) {
			if ( msg.contains(PROCESS_MORE) ) {
				logger.trace("delegator is continuing to process more");
				process();
			}
			
		// handle status requests
		} else if (msg.contains(Status.isStarted)) {
			response = Message.create(Status.isStarted, Status.isStarted.equals(status));
		} else if (msg.contains(Status.isAlive)) {
			response = Message.create(Status.isAlive, true);
		} else if (msg.contains(Status.isDone)) {
			response = Message.create(Status.isDone, Status.isDone.equals(status));
		}
		if (msg.contains(Status.CurrentStatus)) {
			response = Message.create(Status.CurrentStatus, status);
		}
		
		// handle onComplete requests
		if (msg.contains(Control.onComplete)) {
			logger.trace("Delegator recieved message onComplete");
			if ( worker.isComplete() ) {
				sendCompleted(sender());
			} else {
				onComplete.add( sender() );
			}
		}
		
		// now give the work a chance to react
		Message workerResponse = worker.onReceive(msg);
		
		// this causes an infinite message loop
//		if (null == response) {
//			response = workerResponse;
//		}
		
		// respond back if there is one
		if (null != response) {
			sender().tell(response, self());
		}
	}


	/**
	 * AKKA method called when a worker is issued. 
	 * We use it for autostarting this worker.
	 */
	@Override
	public void preStart() throws Exception {
		super.preStart(); // TODO not sure if this is necessary
		if (autoStart) {
			logger.debug("Delegate AUTOSTART worker");
			start();
		}
	}
	/**
	 * AKKA method called when stop is issued. 
	 * We use it to signal that the worker has been disposed.
	 * Of course this might not be accessible when disposed.
	 */
	@Override
	public void postStop() throws Exception {
		logger.trace("worker delegate stoped: {}", name);
		try {
			if (Status.isStarted.equals(status)) {
				worker.end(); // so that the owner can clean up resources
			}
		} catch (Exception e) {
			setLastError(e);
			// TODO figure out how to manage status for disposed workers
		} finally {
			setStatus(Status.isDisposed);
			super.postStop();
		}
	}

	
	public String getName() {
		return name;
	}
	public long getId() {
		return worker.getId();
	}
	
	
	/**
	 * When start is issued the receiver will call this method.
	 * It keeps the receiver method clean
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * TODO impl CdatException here and handling in the Session supervisor
	 * 
	 * @return Returns a message of Success:True on finish of no errors or
	 *         False if there was any exception thrown.
	 *         
	 * @throws CdatException
	 */
	Message start() throws CdatException {
		if ( ! Status.isNew.equals(status) ) {
			logger.trace("Ignoring multistart worker {}", name);
			setLastError(new StreamInitException("May only start new workers"));
			return Message.create(Control.Start,false);
		}
		setStatus(Status.isStarted);
		
		Message msg;
		try {
			// TODO need to release every so often during transfer and
			// TODO query wait ensure control and status messages are processed
			worker.begin();
			process();
			msg = Message.create("Success", "True");
			logger.trace("Worker {} started", name);
		} catch (Exception e) {
			logger.error("Exception opening pipe",e);
			msg = Message.create("Success", "False");
			setLastError(e);
		}
		return msg;
	}
	
	/**
	 * This is the action to process a token of work
	 * @throws CdatException
	 */
	void process() throws CdatException {
		logger.trace("delegator proccessing worker");
		
		try {
			boolean isMore = worker.process();
			
			// this allows the status and control message in on the action
			if (isMore) {
				logger.trace("delegator sending message to CONTINUE");
				self().tell(CONTINUE, self()); // .noSender() ?
			} else {
				done(" called from process when there was no more");
			}
		} catch (Exception e) {
			setLastError(e);
			done("error");
		}
	}
	
	/**
	 * When stop is issued the receiver will call this method.
	 * Also, when the worker finishes it is called.
	 * It keeps the receiver method clean
	 * 
	 * It calls all actors that registered for the onComplete
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @param qualifier
	 */
	void done(String qualifier) {
		logger.debug("delegator done called with: {}", qualifier);
		try {
			worker.end();
			setStatus(Status.isDone);
		} catch (Exception e) {
			setLastError(e);
		} finally {
			for (ActorRef needToKnow : onComplete) {
				sendCompleted(needToKnow);
			}
			onComplete.clear();
		}
	}
	
	/**
	 * Helper method that creates an onComplete:done/error message and sends it
	 * to all those who need to know.
	 * 
	 * Self "documenting" helper method. The call to this from done() is simply
	 * to make that method cleaner.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @param needsToKnow
	 */
	void sendCompleted(ActorRef needsToKnow) {
		String value = "done";
		Message completed = Message.create(Naming.WORKER_NAME, name);
		if (lastError != null) {
			value = "error";
			String exceptionMessage = createExceptionMessage(lastError);
			completed = Message.extend(completed, Status.isError, exceptionMessage);
		}
		completed = Message.extend(completed, Control.onComplete, value);
		needsToKnow.tell(completed, self());
	}

	/**
	 * Helper method to construct a message of the exception tree.
	 * It recursively calls itself to walk the cause tree.
	 * @param t an exception to process. this is Throwable because of getCasue.
	 * @return a string of all messages in the exception tree
	 */
	String createExceptionMessage(Throwable t) {
		String msg = t.getMessage();
		if (t.getCause() != null) {
			msg += ":" + createExceptionMessage(t.getCause());
		}
		return msg;
	}	
	
	
	
	/**
	 * access life cycle status
	 * @ see Status
	 * @return the current life cycle status
	 */
	public Status getStatus() {
		return status;
	}
	void setStatus(Status newStatus) {
		if (Status.isError.equals(status)) {
			logger.trace("status isError -> NOT setting status: {}", newStatus);
			return;
		}
		logger.trace("setting status: {}", newStatus);
		status = newStatus;
	}
	
	/**
	 * Helper method to properly set the latest exception and update the status
	 * @param lastError
	 */
	void setLastError(Exception lastError) {
		setStatus(Status.isError);
		this.lastError = lastError;
	}
}
