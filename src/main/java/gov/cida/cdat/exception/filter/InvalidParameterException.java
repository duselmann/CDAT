package gov.cida.cdat.exception.filter;

import gov.cida.cdat.exception.CdatException;

/**
 * Details to be specified in the message text.
 * 
 * Thrown if the query string is not parseable. E.G. ?foo=bar?baz=4=23
 * Thrown if the parameter value is invalid, like int parsing or RegEx
 * Thrown if the parameter name is not applicable to the data set (recoverable by no applying the param)
 * Thrown if the parameter value is out of bounds, like range checking or string too long
 * @author duselmann
 *
 */
public class InvalidParameterException extends CdatException {

	private static final long serialVersionUID = 1L;

	public InvalidParameterException() {}
	public InvalidParameterException(String msg) {super(msg);}
	public InvalidParameterException(String msg, Throwable cause) {super(msg,cause);}
	
}
