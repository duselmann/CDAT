package gov.cida.cdat.service;

import static akka.actor.SupervisorStrategy.*;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;

import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Option;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.japi.Function;


public class Session extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	

	private boolean autoStart;
	
	/**
	 *  contains the lookup of something that does work like an ETL, Query, or Session
	 */
	final Registry delegates;
	
	/**
	 * Constructor is simple and should only be used by SCManager only.
	 */
	public Session() {
		delegates = new Registry();
	}
	
	
	SupervisorStrategy supervisor = new OneForOneStrategy(10, // TEN errors in duration // TODO make configure
			Time.MINUTE, // TODO make configure
			new Function<Throwable, Directive>() {
		@Override
		public Directive apply(Throwable t) {
			logger.warn("session receved an exception from worker");
			// could be resume(), restart(), escalate() or stop()
			return stop();
		}
	});
	@Override
	public SupervisorStrategy supervisorStrategy() {
		logger.info("SupervisorStrategy fetched {}", supervisor);
		return supervisor;
	}
	
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
		if (msg instanceof AddWorkerMessage) {
			addWorker((AddWorkerMessage)msg);
			return;
		} else if (msg instanceof Message) {
			onReceive((Message)msg);
			return;
		} else if (msg instanceof Terminated) {
			onReceive((Terminated)msg);
			return;
		} else {
			unhandled(msg);
		}
		
		// this will dead letter if the sender is not waiting for a reply - this is ok
		sender().tell(Message.create("UnknowMessageType", msg.getClass().getName()),self());
	}
	/**
	 * This helper method manages the termination status of delegate workers
	 * @param ref the termination message
	 */
	void onReceive(Terminated ref) {
		String workerName = ref.actor().path().name();
		logger.trace("termination recieved for {}", workerName);
		Status status = Status.isDisposed;
		
//		if (false) { // TODO check for something to set this status
//			status = Status.isError;
//		}
		
		delegates.setStatus(workerName, status);
	}
	/**
	 * The helper method that manages the exposed framework messages.
	 * @param msg the message to act on
	 */
	void onReceive(final Message msg) {
		logger.trace("Session recieved message {}", msg);
		Message response = null;
		
		if (msg.contains(SCManager.AUTOSTART)) {
			autoStart = "true".equals( msg.get(SCManager.AUTOSTART) );
		}
		if ( SCManager.SESSION.equals( msg.get(Control.Stop) ) ) {
			stopSession();
			return;
		}
		
		String workerName = msg.get(Naming.WORKER_NAME);

		if ( null != workerName && ! delegates.isAlive(workerName) ) {
			// the session must handle these if the worker is not alive to respond
			if (msg.contains(Status.isAlive)) {
				// if the session is handling this then it it not alive
				response = Message.create(Status.isAlive, false);
				
			} else if (msg.contains(Status.isDone)) {
				response = Message.create(Status.isDone, true);
				
			} else if (msg.contains(Status.isDisposed)) {
				response = Message.create(Status.isDisposed, true);
				
			} else if (msg.contains(Status.CurrentStatus)) {
				String currentSatus = delegates.getStatus(workerName);
				response = Message.create(Status.CurrentStatus, currentSatus);
			}
			if (response != null) {
				sender().tell(response, self());
			}
			return; // a dead worker cannot respond to a forwarded message
		}
		
		ActorRef worker = delegates.get(workerName);
		// if there was no worker found then the we have no delegate to whom to send a message
		if (worker == null) {
			logger.warn("Failed to find worker named {} on session {}", workerName, self().path());
			unhandled(msg);
			return;
			
		} else {
			logger.trace("worker full name: {}", worker.path());
			// workers only read message keys that pertain to them
			// it is okay to pass the original message along
			if (msg.contains(Control.Start)) {
				delegates.setStatus(workerName, Status.isStarted);
			} 
			// forward is better than telling in this case. worker.tell(msg, self());
			worker.forward(msg, context());
		}
	}
	
	/**
	 * Stops the session after all workers have been given time to finish.
	 * @see SCManager.close()
	 */
	void stopSession() {
		try {
			int delegates = delegateCount();
			long endTime = Time.later(Time.HOUR); // TODO make configurable
			while (delegates>0  &&  Time.now()<endTime) {
				Thread.sleep( Time.HALF_MIN.toMillis() ); // TODO make configurable
				delegates = delegateCount();
			}
		} catch (Exception e) {
			// if there is an issue then stop now
		} finally {
			context().stop( self() );
		}		
	}
	
	/**
	 * Counts all delegates that have a Status.isAlive - a special status
	 * that is related to !isDone, !isDisposed, and !isError 
	 * This relies on the delegate properly reporting when it completes.
	 * @return the count of delegates that have not completed yet
	 */
	int delegateCount() {
		int delegateCount = 0;

		for (WeakReference<ActorRef> delegate : delegates.workers.values()) {
			// only count delegates that are not done yet
			if (delegate!=null && delegate.get()!=null 
					&& delegates.isAlive(delegate.get().path().name())) {
				delegateCount++;
			}
		}
		return delegateCount;
	}


	/**
	 * <p>submits a worker for an ETL stream (pipe), 
	 * </p>
	 * Example:<pre>
	 * final String NWIS_SEDIMENT = "Fetch sediment from NWIS";
	 * SCManager session = SCManager.open();
	 * String workerName = manager.addWorker(NWIS_SEDIMENT, nwisRequest);
	 * </pre>
	 * <p> The name now equals "Fetch sediment from NWIS", or "Fetch sediment from NWIS-1", etc.
	 * </p>
	 * @param workerLabel String name - IMPORTANT: Names must be unique, this returns a new name 
	 * 					if the is not then it return a new name that is unique.
	 * 					you MUST maintain a reference to the new name to submit actions to your worker
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @return the new unique string that is used to send messages to submitted pipe
	 */
	void addWorker(AddWorkerMessage addWorker) {
		String workerName = addWorker.getName();
		logger.trace("Session adding a worker with name: {}", workerName);

		// send the unique name back to the requester
		Message msg = Message.create(Naming.WORKER_NAME, workerName);
        sender().tell(msg, self()); // this will fall into the dead letter queue if there is no callback waiting

		addWorker.setAutoStart(autoStart); // pass along the session automatic start state
		
        // Create the AKKA service actor
        ActorRef delegate = context().actorOf(Props.create(Delegator.class, addWorker), workerName);
        
        context().watch(delegate); // watch the delegate for termination handling
        
        delegates.put(workerName, delegate); // maintain a convenient reference
	}
	
	/**
	 * Place holder for potential implementation.
	 * We will put any session specific cleanup here.
	 */
	@Override
	public void postStop() throws CdatException {
		logger.trace("Session stopped. {} ", self().path());
		try {
			super.postStop();
		} catch (Exception e) {
			throw new CdatException("Error cleaning up session: " + self().path(), e);
		}
	}
	
	/**
	 * Place holder (for the most part) for potential implementation.
	 * We will put any session specific cleanup here pertaining to restart.
	 * For now this is a sample implementation from AKKA documentation.
	 */
	@Override
	public void preRestart(Throwable reason, Option<Object> message) 
			throws CdatException {
		
		for (ActorRef child : getContext().getChildren()) {
			context().unwatch(child); // stop watching all the children
			context().stop(child);
		}
		postStop();
	}
}
