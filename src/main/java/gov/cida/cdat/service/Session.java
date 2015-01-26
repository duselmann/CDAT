package gov.cida.cdat.service;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.io.stream.Registry;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;


public class Session extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 *  contains the lookup of something that does work like an ETL, Query, or Session
	 */
	final Registry delegates;
	public Session() {
		delegates = new Registry();
	}
	
	
	// TODO impl start/stop fail return true/false and the Actor supervisor
	SupervisorStrategy supervisor = new OneForOneStrategy(10, // TEN errors in duration
			Duration.create("1 minute"), // TODO check the proper duration
			new Function<Throwable, Directive>() {
		@Override
		public Directive apply(Throwable t) {
			logger.warn("session receved an exception");
			
			if (t instanceof Exception) {
				// TODO proper handling
				logger.warn("session receved an exception, resuming");
				return resume();
			} else if (t instanceof NullPointerException) {
				return restart();
			} else if (t instanceof IllegalArgumentException) {
				return stop();
			} else {
				return escalate();
			}
		}
	});
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisor;
	}
	
	
	
	// TODO session should have a dispose all delegates for when the session is done
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof AddWorkerMessage) {
			onReceive((AddWorkerMessage)msg);
			return;
		} else if (msg instanceof Message) {
			onReceive((Message)msg);
			return;
		} else {
			unhandled(msg);
		}
		sender().tell(Message.create("listens for Message class only"),self());
	}
	void onReceive(final Message msg) throws Exception {
		logger.trace("Session recieved message {}", msg);
		
		String workerName = msg.get(Naming.WORKER_NAME); // TODO smell
		ActorRef worker = delegates.get(workerName);
		
		if (worker != null) {
			logger.trace( worker.path().toString() );
		}
		// handle the return message from the child that onComplete has triggered
		if (msg.contains(Control.onComplete)) {
			if ( !"done".equals( msg.get(Control.onComplete) ) ) {
//				final ActorRef onCompleteSender = sender();
//				Future<Object> response = Patterns.ask(worker, msg, 50000);
//				OnComplete<Object> onCompleteWorker = new OnComplete<Object>() {
//					public void onComplete(Throwable t, Object response) throws Throwable {
//						logger.trace("Session recieved RESPONSE trigger for onComplete {}", msg);
//						Message extended = Message.extend((Message)response, "ReturnFromSessionFuture", null);
//						onCompleteSender.tell(extended, self());
//					}
//				};
//			    response.onComplete(onCompleteWorker, context().dispatcher());
				worker.forward(msg, context());
			}
		    return;
		}
		
//		ActorRef worker2 = context().child(workerName).get();
//		logger.trace("ActorRefs for worker '{}' are {} equal.", workerName, worker!=worker2?"NOT":"");
		
		if (worker == null) {
			logger.warn("Failed to find worker named {} on session {}", workerName, self().path());
			unhandled(msg);
			return;
		} else {
			// workers only read message keys that pertain to them
			// it is okay to pass the original message along
			worker.tell(msg, self());
		}
	}
	void onReceive(AddWorkerMessage addWorker) throws Exception {
		logger.trace("Session recieved new worker {}", addWorker.getName());
		String uniqueName = addWorker(addWorker);
		Message msg = Message.create(Naming.WORKER_NAME,uniqueName);
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
	String addWorker(AddWorkerMessage worker) { // TODO QueryWorker name change?
        // Create the AKKA service actor
		logger.trace("Adding a worker with name: {}", worker.getName());
        ActorRef delegate = context().actorOf(Props.create(Delegator.class, worker), worker.getName());
		
        // TODO this is not isolated - need to refactor as a return message
        // returns a unique name from the given name
        return delegates.put(worker.getName(), delegate); // TODO dispose of actor?
	}
	
	
	@Override
	public void postStop() throws Exception {
		logger.trace("Session stopped.");
		super.postStop();
	}
}
