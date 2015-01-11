package gov.cida.cdat.exception.producer;

/**
 * Thrown if the source is not found or not responding 
 * 
 * @author duselmann
 */
public class SourceNotFoundException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	public SourceNotFoundException() {}
	public SourceNotFoundException(String msg) {super(msg);}
	public SourceNotFoundException(String msg, Throwable cause) {super(msg,cause);}
	
}
