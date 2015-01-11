package gov.cida.cdat.exception;

/**
 * Thrown when creating new services if the given name for a new service is already in use
 * 
 * @author duselmann
 */
public class DupicateNameException extends RuntimeException { // TODO RT or not

	private static final long serialVersionUID = 1L;

	public DupicateNameException() {}
	public DupicateNameException(String msg) {super(msg);}
	public DupicateNameException(String msg, Throwable cause) {super(msg,cause);}

}
