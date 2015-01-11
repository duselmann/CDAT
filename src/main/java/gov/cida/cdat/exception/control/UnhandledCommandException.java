package gov.cida.cdat.exception.control;

/**
 * Thrown if no process handles the request
 * 
 * @author duselmann
 */
public class UnhandledCommandException extends ControlException {

	private static final long serialVersionUID = 1L;

	public UnhandledCommandException() {}
	public UnhandledCommandException(String msg) {super(msg);}
	public UnhandledCommandException(String msg, Throwable cause) {super(msg,cause);}
	
}
