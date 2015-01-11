package gov.cida.cdat.exception.producer;

import gov.cida.cdat.exception.CdatException;

/**
 * Thrown for any general producer issue not specified by a specific instance.
 * Details to be specified in the message text.
 * 
 * @author duselmann
 */
public class ProducerException extends CdatException {

	private static final long serialVersionUID = 1L;

	public ProducerException() {}
	public ProducerException(String msg) {super(msg);}
	public ProducerException(String msg, Throwable cause) {super(msg,cause);}
	
}
