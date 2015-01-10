package gov.cida.cdat.services;

import gov.cida.cdat.control.Control;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Map;

import scala.Option;
import akka.actor.UntypedActor;

public class Delegator extends UntypedActor {

	final Worker worker;
	
	private OutputStream stream;
	
	
	public Delegator(Worker worker) {
		this.worker = worker;
	}
	
	
	public void joinTo(OutputStream out) {
		stream = new BufferedOutputStream(out);
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

	
	private void process() {

	}
	

	@Override
	public void preStart() throws Exception {
		super.preStart();
		worker.begin();
	}
	
	@Override
	public void preRestart(Throwable reason, Option<Object> message) throws Exception {
		super.preRestart(reason, message);
		// TODO
	}
	
	@Override
	public void postStop() throws Exception {
		worker.end();
		super.postStop();
	}
	
}
