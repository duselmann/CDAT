package gov.cida.cdat.control;

import gov.cida.cdat.message.Message;


/**
 * This is the enumeration of standard CDAT status messages.
 * The user can submit custom messages via the Message class.
 * Status messages are those that inquire about the state of a worker: isAlive, config, etc.
 * 
 * @author duselman
 * @see Message
 * @see Control
 */
public enum Status {
	isNew,isStarted,isAlive,isDone,isError,isDisposed,CurrentStatus;
	
	// cannot override equals
	public boolean is(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof String) {
			return toString().equalsIgnoreCase((String)other);
		}
		return super.equals(other);
	}
}
