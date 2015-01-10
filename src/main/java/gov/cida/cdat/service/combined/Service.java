package gov.cida.cdat.service.combined;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.PipeStream;

import java.io.IOException;
import java.util.Map;

import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	
	private PipeStream pipe;
	
	public Service(PipeStream pipe) {
		this.pipe = pipe;
	}

	@SuppressWarnings("unchecked")
	public void onReceive(Object msg) throws Exception {
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

	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
	}
	
	
	private void start() throws StreamInitException {
		pipe.open();
	}
	private void stop(String force) throws IOException {
		pipe.close();
		context().stop(getSelf());
	}

}
