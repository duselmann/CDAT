package gov.cida.cdat.service.combined;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.PipeStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	
	private PipeStream  pipe;
	private InputStream pipeStream;
	
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
		if (msg.containsKey(Control.onComplete.toString())) {
			// TODO stops and other control should be tracked.
			// TODO need a good means to track open/finished/closed streams
			int count = 0;
			while (pipeStream == null) {
				Thread.sleep(100);
				count++;
			}
			System.out.println("count of waits for complete: " + count);

			System.out.println("available: " +
					pipeStream.available() // TODO asdf
			);
			
			msg.put(Control.onComplete.toString(), "True");
			getSender().tell(msg, getSelf());
		}
	}

	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
	}
	
	
	private void start() throws StreamInitException {
		pipeStream = pipe.open();
	}
	private void stop(String force) throws IOException {
		pipe.close();
		context().stop(getSelf());
	}

}
