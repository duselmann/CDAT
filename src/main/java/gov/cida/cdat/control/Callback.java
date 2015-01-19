package gov.cida.cdat.control;

import akka.dispatch.OnComplete;

/**
 * The callback for the message return onComplete. The motivation for this is to change
 * the AKKA Object response message to the CDAT Message class
 * 
 * @author duselman
 *
 */
abstract public class Callback extends OnComplete<Message>{

	/**
	 * Called after a worker is known to be completed.
	 * Casts the AKKA OnComplete from Object to Message response
	 * 
	 * TODO Implement the Delegate and Worker actors if they can be used for a better OnComplete determination
	 * TODO Implement other callback method types. AKKA has others like OnError and OnSuccess (I think)
	 */
	@Override
	abstract public void onComplete(Throwable t, Message repsonse) throws Throwable;

}
