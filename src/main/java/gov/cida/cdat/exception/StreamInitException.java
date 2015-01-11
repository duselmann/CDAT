package gov.cida.cdat.exception;

/**
 * Thrown when a stream supplier fails to initialize the stream
 * 
 * @author duselmann
 */
public class StreamInitException extends CdatException {

	private static final long serialVersionUID = 1L;

	public StreamInitException() {}
	public StreamInitException(String msg) {super(msg);}
	public StreamInitException(String msg, Throwable cause) {super(msg,cause);}
	
}
