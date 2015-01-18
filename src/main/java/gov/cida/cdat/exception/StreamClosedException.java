package gov.cida.cdat.exception;

/**
 * Thrown if any tier stream is closed when data is accessed
 * 
 * @author duselmann
 */
public class StreamClosedException extends CdatException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public StreamClosedException(String msg) {super(msg);}
	public StreamClosedException(String msg, Throwable cause) {super(msg,cause);}
	
}
