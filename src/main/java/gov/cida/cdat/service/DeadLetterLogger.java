package gov.cida.cdat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;

public class DeadLetterLogger extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());	
	
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof DeadLetter) {
			logger.trace("- > {} < - -",msg);
		}
	};
}
