package gov.cida.cdat.services;

import gov.cida.cdat.control.AddWorker;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.control.Registry;

import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	
	final Registry workers = new Registry();
	private boolean isJoined;
	

	@SuppressWarnings("unchecked")
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof AddWorker) {
			addWorker((AddWorker) msg);
		}
		if (msg instanceof Map) {
			onReceive((Map<String,String>)msg);
		}
	}
	public void onReceive(Map<String,String> msg) throws Exception {
		System.out.println("Service recieved message " + msg);
		
		if (msg.containsKey(Control.Stop.toString())) {
			System.out.println("Service recieved message " + Control.Stop);
			stop(msg.get(Control.Stop.toString()));
		}
		if (msg.containsKey(Control.Start.toString())) {
			System.out.println("Service recieved message " + Control.Start);
			start();
		}
	}
	
	
	private void addWorker(AddWorker addWorker) {
		String type    = addWorker.getType().toString();
		Worker worker  = addWorker.getWorker();
		ActorRef actor = getContext().actorOf(Props.create(Delegator.class, worker), type);
		workers.put(type, actor);
		join();
	}
	
	
	private void join() {
		if (workers.size() != 3) {
			return;
		}

		// TODO do join streams
		
		isJoined = true;
	}

	
	public boolean sendControl(String serviceName, Map<String,String> msg) {
        // send a message
		ActorRef actor = workers.get(serviceName);		
		if (actor == null) {
			return false; // TODO decide if this is appropriate and sufficient
		}
		
		actor.tell(msg, getSelf());
		
		return true;
	}
	
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
	}
	
	
	private void start() {
		if ( ! isJoined ) {
			throw new RuntimeException("unjoined service");
		}
		sendControl(AddWorker.Type.Producer.toString(), Message.create(Control.Start));
	}
	private void stop(String force) {
		sendControl(AddWorker.Type.Producer.toString(), Message.create(Control.Stop));
		context().stop(getSelf());
	}

}
