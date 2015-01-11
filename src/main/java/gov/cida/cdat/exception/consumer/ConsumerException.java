package gov.cida.cdat.exception.consumer;

import gov.cida.cdat.exception.CdatException;


/**
 * General Consumer base Exception
 * 
 * @author duselmann
 */
public class ConsumerException extends CdatException {

	private static final long serialVersionUID = 1L;

	public ConsumerException() {}
	public ConsumerException(String msg) {super(msg);}
	public ConsumerException(String msg, Throwable cause) {super(msg,cause);}
	
}
