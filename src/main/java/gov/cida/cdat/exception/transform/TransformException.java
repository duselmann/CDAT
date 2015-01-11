package gov.cida.cdat.exception.transform;

import gov.cida.cdat.exception.CdatException;

/**
 * General Transformation base Exception
 * Used for invalid config, value, name, bounds, etc.
 * Details to be specified in the message text.
 * 
 * @author duselmann
 */

public class TransformException extends CdatException {

	private static final long serialVersionUID = 1L;

	public TransformException() {}
	public TransformException(String msg) {super(msg);}
	public TransformException(String msg, Throwable cause) {super(msg,cause);}
	
}
