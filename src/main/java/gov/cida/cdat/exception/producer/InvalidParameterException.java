package gov.cida.cdat.exception.producer;

/**
 * Thrown if the server connection parameter invalid name, value, etc.
 * 
 * @author duselmann
 */
public class InvalidParameterException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public InvalidParameterException(String msg) {super(msg);}
	public InvalidParameterException(String msg, Throwable cause) {super(msg,cause);}
	
}
