package gov.cida.cdat.service.combined;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.PipeStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private PipeStream  pipe;
	private InputStream pipeStream;
	
	public Service(PipeStream pipe) {
		this.pipe = pipe;
	}

	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Map) {
			onReceive((Message)msg);
		}
	}
	public void onReceive(Message msg) throws Exception {
		logger.trace("Service recieved message {}", msg);
		
		if (msg.containsKey(Control.Stop.toString())) {
			logger.trace("Service recieved message {}", Control.Stop);
			stop(msg.get(Control.Stop.toString()));
		}
		if (msg.containsKey(Control.Start.toString())) {
			logger.trace("Service recieved message {}", Control.Start);
			getSender().tell(start(), getSelf());
		}
		if (msg.containsKey(Control.onComplete.toString())) {
			onComplete(msg);
			getSender().tell(onComplete(msg), getSelf());
		}
		
		logger.debug("onReceive exit");
	}

	private Message onComplete(Message msg)
			throws InterruptedException, CdatException {
		// TODO stops and other control should be tracked.
		// TODO need a good means to track open/finished/closed streams
		int maxWait = Message.getInt(msg, "maxWait", 10000);
		
		int count = 0;
		while (pipeStream == null  &&  (count*100 < maxWait  ||  maxWait > -1)) {
			Thread.sleep(100);
			count++;
		}
		if (pipeStream == null) {
			throw new CdatException("onComplete timeout");
		}
		logger.trace("count of waits for complete: {}", count);
		
		// signal back to the future that we are completed
		msg.put(Control.onComplete.toString(), "True");
		return msg;
	}

	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
	}
	
	
	private Message start() throws StreamInitException {
		Message msg;
		try {
			pipeStream = pipe.open();
			msg = Message.create("Success", "True");
		} catch (Exception e) {
			logger.error("Exception opening pipe",e);
			msg = Message.create("Success", "False");
		}
		return msg;
	}
	private void stop(String force) throws IOException {
		pipe.close();
		context().stop(getSelf());
	}

}
