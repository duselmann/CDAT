package gov.cida.cdat.exception.producer;

import gov.cida.cdat.exception.StreamInitException;


/**
 * General Producer Connection base Exception
 * 
 * @author duselmann
 */
public class ConnectionException extends StreamInitException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public ConnectionException(String msg) {super(msg);}
	public ConnectionException(String msg, Throwable cause) {super(msg,cause);}
	
}
