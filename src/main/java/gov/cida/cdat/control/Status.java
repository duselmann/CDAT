package gov.cida.cdat.control;

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
	isAlive,isOpen,isStarted,isDone,config
}
