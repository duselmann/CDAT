package gov.cida.cdat.exception.producer;

/**
 * Thrown if the source is not found or not responding 
 * 
 * @author duselmann
 */
public class SourceNotFoundException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public SourceNotFoundException(String msg) {super(msg);}
	public SourceNotFoundException(String msg, Throwable cause) {super(msg,cause);}
	
}
