package gov.cida.cdat.control;

/**
 * This is the enumeration of standard CDAT control messages.
 * The user can submit custom messages via the Message class.
 * Controls are those actions that alter the state of the worker: stop, start, etc.
 * 
 * @author duselman
 * @see Message
 * @see Status
 */
public enum Control {
	Start, Stop, onComplete
}
