package gov.cida.cdat.service;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Option;

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
	 * The list of Actors interested in the completion of this delegate of work.
	 */
	private final List<ActorRef> onComplete = new LinkedList<ActorRef>();
	
	public Delegator(String name, Worker worker) {
		this.name   = name;
		this.worker = worker;
	}
	
	
	/**
	 *  AKKA framework generic messages receiver
	 */
	public void onReceive(Object msg) throws Exception {
		logger.trace("Delegator recieved message " + msg);
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
		if (msg.contains(Control.Start)) {
			logger.trace("Delegator recieved message " + Control.Start);
			start();
		} else if (msg.contains(Control.Stop)) {
			logger.trace("Delegator recieved message " + Control.Stop);
			try {
				done( msg.get(Control.Stop) );
			} finally {
				context().stop( self() );
			}
		}
		if (msg.contains(Control.onComplete)) {
			logger.trace("Delegator recieved message onComplete");
			if ( worker.isComplete() ) {
				sendCompleted(sender());
			} else {
				onComplete.add( sender() );
			}
		}
		
	}


	/**
	 * AKKA method for future impl
	 */
	@Override
	public void preStart() throws Exception {
		super.preStart();
	}
	/**
	 * AKKA method for future impl
	 */
	@Override
	public void postRestart(Throwable reason) throws Exception {
		super.postRestart(reason);
	}
	/**
	 * AKKA method for future impl
	 */
	@Override
	public void preRestart(Throwable reason, Option<Object> message)
			throws Exception {
		super.preRestart(reason, message);
	}
	/**
	 * AKKA method for future impl
	 */
	@Override
	public void postStop() throws Exception {
		// TODO Auto-generated method stub
		super.postStop();
	}

	
	public String getName() {
		return name;
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
		Message msg;
		try {
			worker.begin();
			msg = Message.create("Success", "True");
		} catch (Exception e) {
			logger.error("Exception opening pipe",e);
			msg = Message.create("Success", "False");
		} finally {
			done(" from delegator start");
		}
		return msg;
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
		worker.end();
		for (ActorRef needToKnow : onComplete) {
			sendCompleted(needToKnow);
		}
		onComplete.clear();
	}
	
	/**
	 * Helper method that creates an onComplete:done message and sends it
	 * to all those who need to know.
	 * 
	 * Self documenting helper method. The call to this from done() is simply
	 * to make that method cleaner.
	 * 
	 * It is package access for testing, would be private otherwise.
	 * 
	 * @param needsToKnow
	 */
	void sendCompleted(ActorRef needsToKnow) {
		Message completed = Message.create(Control.onComplete, "done");
		needsToKnow.tell(completed, self());
	}
}
