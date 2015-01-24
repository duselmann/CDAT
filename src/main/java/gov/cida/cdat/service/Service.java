package gov.cida.cdat.service;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.message.Message;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Option;
import akka.actor.UntypedActor;


public class Service extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private DataPipe  pipe;
	private InputStream pipeStream;
	
	public Service(DataPipe pipe) {
		this.pipe = pipe;
	}

	
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Message) {
			onReceive((Message)msg);
		}
//		else if (msg instanceof Map) {
//			onReceive(Message.create((Map<String, String>)msg));
//		}
		unhandled(msg);
		sender().tell(Message.create("listens for Message class only"),self());
	}
	public void onReceive(Message msg) throws Exception {
		logger.trace("Service recieved message {}", msg);
		
		if (msg.contains(Control.Stop)) {
			logger.trace("Service recieved message {}", Control.Stop);
			done(msg.get(Control.Stop.toString()));
		}
		if (msg.contains(Control.Start)) {
			logger.trace("Service recieved message {}", Control.Start);
			sender().tell(start(), self());
		}
		if (msg.contains(Control.onComplete)) {
			Message response = onComplete(msg);
			sender().tell(response, self());
		}
		
		unhandled(msg);
		
		logger.debug("onReceive exit");
	}

	private Message onComplete(Message msg) throws CdatException {
		// TODO stops and other control should be tracked.
		// TODO need a good means to track open/finished/closed streams
		int maxWait = Message.getInt(msg, "maxWait", 10000);
		
		int count = 0;
		while (pipeStream == null  &&  (count*100 < maxWait  ||  maxWait > -1)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// cannot sleep is okay
				// TODO code review this please
			}
			count++;
		}
		if (pipeStream == null  &&  count*100>=maxWait) {
			throw new CdatException("onComplete timeout");
		}
		logger.trace("count of waits for complete: {}", count);
		
		// signal back to the future that we are completed
		Message response = Message.extend(msg, Control.onComplete, "True");
		return response;
	}

	
	@Override
	public void preStart() throws Exception {
		// TODO Auto-generated method stub
		super.preStart();
	}
	
	@Override
	public void preRestart(Throwable reason, Option<Object> message)
			throws Exception {
		// TODO Auto-generated method stub
		super.preRestart(reason, message);
	}
	
	@Override
	public void postRestart(Throwable reason) throws Exception {
		// TODO Auto-generated method stub
		super.postRestart(reason);
	}
	
	@Override
	public void postStop() throws Exception {
		// TODO Auto-generated method stub
		super.postStop();
	}
	
	private Message start() throws CdatException {
		Message msg;
		try {
			pipeStream = pipe.open();
			msg = Message.create("Success", "True");
		} catch (Exception e) {
			logger.error("Exception opening pipe",e);
			msg = Message.create("Success", "False");
		} finally {
			done(null);
		}
		return msg;
	}
	
	private void done(String force) {
		pipe.close();
		context().stop( self() );
	}

}
