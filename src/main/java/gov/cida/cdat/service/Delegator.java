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
 * This is the 'threaded' via AKKA delegate to run workers.
 * Implementation should extend Worker with specific behavior.
 * 
 * @author duselman
 *
 */
public class Delegator extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The message name for processing more or CONTINUE
	 */
	private static final String  PROCESS_MORE = "process.more";
	/**
	 * The message to continue processing more data
	 */
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
	private Exception currentError;
	
	/**
	 * If the worker should start automatically then this should be set to true on worker create.
	 */
	private boolean autoStart;

	/**
	 * The list of Actors interested in the completion of this delegate of work.
	 */
	private final List<ActorRef> onComplete = new LinkedList<ActorRef>();
	
	
	/**
	 * Constructor that takes the AddWorkerMessage payload and assigns all values to the new delegate.
	 * @param worker the AddWorkerMessage payload
	 */
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
		logger.trace("Delegator recieved message {} ", msg);
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
	 * @param msg the incoming message from the session manager
	 * @throws Exception AKKA requires it a raw Exception
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
		
		}
		// handle status requests
		if (msg.contains(Status.isNew)) {
			response = createStatusMessage(Status.isNew);
		} else if (msg.contains(Status.isStarted)) {
			response = createStatusMessage(Status.isStarted);
		} else if (msg.contains(Status.isAlive)) {
			response = Message.create(Status.isAlive, true);
		} else if (msg.contains(Status.isDone)) {
			response = createStatusMessage(Status.isDone);
		} else if (msg.contains(Status.CurrentStatus)) {
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
		/* Message workerResponse = */ worker.onReceive(msg);
		
		// This causes an infinite message loop and I am not sure why
//		if (null == response) {
//			response = workerResponse;
//		}
		
		// respond back if there is one
		if (null != response) {
			sender().tell(response, self());
		}
	}

	/**
	 * status check helper method to make the code a bit DRYer.
	 * 
	 * @param isStatus the status to check
	 * @return a message containing the state of the delegate to the give status
	 */
	Message createStatusMessage(Status isStatus) {
		return Message.create(isStatus, this.status.equals(isStatus));
	}

	/**
	 * AKKA method called when a worker is issued. 
	 * We use it for automatically starting this worker.
	 */
	@Override
	public void preStart() throws Exception {
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
			// call end if this is the first stop call
			if (Status.isStarted.equals(status)) {
				worker.end(); // so that the owner can clean up resources
			}
		} catch (Exception e) {
			setCurrentError(e);
		} finally {
			// once the delegate is stopped it is no longer accessible
			// therefore this is a bit more about ideals than it is useful
			setStatus(Status.isDisposed);
		}
	}


	/**
	 * @return the unique name or this delegate
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the worker Id - part of the initial spec but currently unused
	 */
	public long getId() {
		return worker.getId();
	}
	
	
	/**
	 * When start is issued the receiver will call this method.
	 * It keeps the receiver method clean
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @return Returns a message of Success:True on finish of no errors or
	 *         False if there was any exception thrown.
	 *         
	 * @throws CdatException
	 */
	Message start() throws CdatException {
		if ( ! Status.isNew.equals(status) ) {
			logger.debug("Ignoring multistart worker {}", name);
			setCurrentError(new StreamInitException("May only start new workers"));
			return Message.create(Control.Start,false);
		}
		setStatus(Status.isStarted);
		
		Message msg;
		try {
			worker.begin();
			process();
			msg = Message.create("Success", "True");
			logger.trace("Worker {} started", name);
		} catch (Exception e) {
			msg = Message.create("Success", "False");
			setCurrentError(e);
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
			setCurrentError(e);
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
			setCurrentError(e);
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
		if (currentError != null) {
			value = "error";
			String exceptionMessage = createExceptionMessage(currentError);
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
	/**
	 * framework accessor to set the current status. It will not allow
	 * the status to over write once in error status.
	 * @param newStatus the current status
	 */
	void setStatus(Status newStatus) {
		// preserve error status
		if (Status.isError.equals(status)) {
			logger.trace("status isError -> NOT setting status: {}", newStatus);
			return;
		}
		logger.trace("setting status: {}", newStatus);
		status = newStatus;
	}
	
	/**
	 * Helper method to properly set the latest exception and update the status
	 * @param currentError
	 */
	void setCurrentError(Exception error) {
		logger.debug("Exception running worker", error);
		setStatus(Status.isError);
		this.currentError = error;
		worker.setCurrentError(error);
		if ( worker.isTerminateOnError(error) ) {
			context().stop( self() );
		}
	}
}
