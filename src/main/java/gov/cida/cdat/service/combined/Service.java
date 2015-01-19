package gov.cida.cdat.service.combined;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.stream.PipeStream;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.japi.Function;


public class Service extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private PipeStream  pipe;
	private InputStream pipeStream;
	
	public Service(PipeStream pipe) {
		this.pipe = pipe;
	}

	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return  new OneForOneStrategy(1, Duration.create("1 minute"),
					new Function<Throwable, Directive>() {
				@Override
				public Directive apply(Throwable t) {
					logger.warn("receved an exception");
					
					if (t instanceof Exception) {
						// TODO proper handling
						logger.warn("receved an exception, resuming");
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
	}
	
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Message) {
			onReceive((Message)msg);
		}
//		else if (msg instanceof Map) {
//			onReceive(Message.create((Map<String, String>)msg));
//		}
		unhandled(msg);
		sender().tell(Message.create("listens to Message class only"),self());
	}
	public void onReceive(Message msg) throws Exception {
		logger.trace("Service recieved message {}", msg);
		
		if (msg.containsKey(Control.Stop.toString())) {
			logger.trace("Service recieved message {}", Control.Stop);
			done(msg.get(Control.Stop.toString()));
		}
		if (msg.containsKey(Control.Start.toString())) {
			logger.trace("Service recieved message {}", Control.Start);
			sender().tell(start(), self());
		}
		if (msg.containsKey(Control.onComplete.toString())) {
			onComplete(msg);
			sender().tell(onComplete(msg), self());
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
		msg.put(Control.onComplete.toString(), "True");
		return msg;
	}

	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
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
