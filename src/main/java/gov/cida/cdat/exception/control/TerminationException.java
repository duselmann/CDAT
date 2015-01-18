package gov.cida.cdat.exception.control;

/**
 * Thrown on a forced termination control message to ensure all tiers terminate.
 * 
 * TODO this might not be necessary but could be useful.
 * 
 * @author duselmann
 */
public class TerminationException extends ControlException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public TerminationException(String msg) {super(msg);}
	public TerminationException(String msg, Throwable cause) {super(msg,cause);}
	
}
