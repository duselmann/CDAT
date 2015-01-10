package gov.cida.cdat.service.distributed;

import gov.cida.cdat.control.Control;

import java.util.Map;

import scala.Option;
import akka.actor.UntypedActor;

public class Delegator extends UntypedActor {

	final Worker worker;
	
	public Delegator(Worker worker) {
		this.worker = worker;
	}
	
	
	@SuppressWarnings("unchecked")
	// AKKA framework generic messages
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Map) {
			onReceive((Map<String,String>)msg);
		}		
	}
	// CDAT framework specific messages
	public void onReceive(Map<String,String> msg) throws Exception {
		System.out.println("Service recieved message " + msg);
		
		if (msg.containsKey(Control.Start.toString())) {
			System.out.println("Delegator recieved message " + Control.Start);
			worker.begin();
		}
		if (msg.containsKey(Control.Stop.toString())) {
			System.out.println("Delegator recieved message " + Control.Stop);
			context().stop(getSelf());
		}
	}


	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		//worker.begin();
	}
	@Override
	public void postStop() throws Exception {
		worker.end();
		super.postStop();
	}

	
	@Override
	public void preRestart(Throwable reason, Option<Object> message) throws Exception {
		super.preRestart(reason, message);
		// TODO
	}
	@Override
	public void postRestart(Throwable reason) throws Exception {
		// TODO Auto-generated method stub
		super.postRestart(reason);
	}
}
