package gov.cida.cdat.exception.filter;

import gov.cida.cdat.exception.CdatException;

/**
 * General Filter base Exception
 * 
 * @author duselmann
 */
public class FilterException extends CdatException {

	private static final long serialVersionUID = 1L;

	// Do not add other constructors. We want to ensure a message.
	public FilterException(String msg) {super(msg);}
	public FilterException(String msg, Throwable cause) {super(msg,cause);}
	
}
