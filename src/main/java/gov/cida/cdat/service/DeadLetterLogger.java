package gov.cida.cdat.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;

/**
 * <p>This class makes a custom log of cDAT messages that fall into the deadletter queue.
 * </p>
 * <p>Messages fall into the deadletter if the delegate stopped prior to receiving the message
 * or if the timeout duration expires prior to being handled.
 * </p>
 * <p>Example, when the messages are sent to the SCManager there is a return message for the
 * onComplete handling. If the user does not supply (or request) a callback for the onComplete
 * feature then the reply will fall into the deadletter.
 * </p>
 * <p>It is important that messages do not actually fall on deaf ears so this class ensures that
 * we know what messages are falling into the deadletter queue. This class logs the contents of the
 * message for possible identification of intent.
 * </p>
 * <p>This in combination with the Message.trace() method which adds a trace to the message creator
 * source line for further debugging.
 * </p>
 * 
 * @see gov.cida.cdat.message.Message
 * 
 * @author duselmann
 */
public class DeadLetterLogger extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());	
	
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof DeadLetter) {
			logger.trace("- > {} < - -",msg);
		}
	};
}
