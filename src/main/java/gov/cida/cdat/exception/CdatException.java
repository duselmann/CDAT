package gov.cida.cdat.exception;

/**
 * Thrown if the stream is unexpectedly closed in any tier as the 
 * "caused by" for that tier specific exception
 * 
 * @author duselmann
 */
public class CdatException extends Exception {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public CdatException(String msg) {super(msg);}
	public CdatException(String msg, Throwable cause) {super(msg,cause);}
		
}
