package gov.cida.cdat.service;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.stream.Registry;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;

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
	public static final Object AUTOSTART = "autostart";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	

	private boolean autoStart;
	
	/**
	 *  contains the lookup of something that does work like an ETL, Query, or Session
	 */
	final Registry delegates;
	public Session() {
		delegates = new Registry();
	}
	
	
	// TODO impl start/stop fail return true/false and the Actor supervisor
	SupervisorStrategy supervisor = new OneForOneStrategy(10, // TEN errors in duration
			SCManager.MINUTE, // TODO make configure
			new Function<Throwable, Directive>() {
		@Override
		public Directive apply(Throwable t) {
			logger.warn("session receved an exception");
			
			// TODO proper handling - this is to inspect how this API works
			if (t instanceof Exception) {
				logger.warn("session receved an exception, resuming");
				return resume();
			} else if (t instanceof Throwable) {
				return stop();
			} else if (t instanceof IllegalArgumentException) {
				return restart();
			} else {
				return escalate();
			}
		}
	});
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisor;
	}
	
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg == null) {
			return;
		}
		
		if (msg instanceof AddWorkerMessage) {
			onReceive((AddWorkerMessage)msg);
			return;
		} else if (msg instanceof Message) {
			onReceive((Message)msg);
			return;
		} else if (msg instanceof Terminated) {
			Status status = Status.isDisposed;
			Terminated ref = (Terminated)msg;
//			if (false) { // TODO check for exception in delegate
//				status = Status.isError;
//			}
			delegates.setStatus(ref.actor().path().name(), status);
			return;
		} else {
			unhandled(msg);
		}
		
		sender().tell(Message.create("UnknowMessageType", msg.getClass().getName()),self());
	}
	void onReceive(final Message msg) throws Exception {
		logger.trace("Session recieved message {}", msg);
		
		if (msg.contains(AUTOSTART)) {
			autoStart = "true".equals( msg.get(AUTOSTART) );
		}
		
		String workerName = msg.get(Naming.WORKER_NAME);
		ActorRef worker = delegates.get(workerName);
		
		if (worker != null) {
			logger.trace( worker.path().toString() );
		}
		
		if (worker == null) {
			logger.warn("Failed to find worker named {} on session {}", workerName, self().path());
			unhandled(msg);
			return;
			
		} else {
			// workers only read message keys that pertain to them
			// it is okay to pass the original message along
			// forward is better than telling in this case. worker.tell(msg, self());
			if (msg.contains(Control.Start)) {
				delegates.setStatus(workerName, Status.isStarted);
			} 
			worker.forward(msg, context());
		}
	}
	void onReceive(AddWorkerMessage addWorker) throws Exception {
		addWorker.setAutoStart(autoStart);
		logger.trace("Session recieved new worker {}", addWorker.getName());
		addWorker(addWorker);
        Message msg = Message.create(Naming.WORKER_NAME,addWorker.getName());
        sender().tell(msg, self());
	}
	
	
	/**
	 * <p>submits a worker for an ETL stream (pipe), 
	 * </p>
	 * Example:<br>
	 * String final NWIS_SEDIMENT = "Fetch sediment from NWIS";<br>
	 * SCManager manager = SCManager.instance();<br>
	 * String workerName = manager.addWorker(NWIS_SEDIMENT, nwisRequest);<br>
	 * <p> The name now equals "Fetch sediment from NWIS", or "Fetch sediment from NWIS-1", etc.
	 * </p>
	 * @param workerLabel String name - IMPORTANT: Names must be unique, this returns a new name 
	 * 					if the is not then it return a new name that is unique.
	 * 					you MUST maintain a reference to the new name to submit actions to your worker
	 * @param pipe the full ETL flow from input stream producer (extractor) to the output stream consumer (loader).
	 * 					transformers are stream that inject themselves in the consumer flow
	 * @return the new unique string that is used to send messages to submitted pipe
	 */
	void addWorker(AddWorkerMessage worker) {
        // Create the AKKA service actor
		logger.trace("Adding a worker with name: {}", worker.getName());
        ActorRef delegate = context().actorOf(Props.create(Delegator.class, worker), worker.getName());
        context().watch(delegate);
        delegates.put(worker.getName(), delegate);
	}
	
	/**
	 * Place holder for potential implementation.
	 * We will put any session specific cleanup here.
	 */
	@Override
	public void postStop() throws CdatException {
		logger.trace("Session stopped.");
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
