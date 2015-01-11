package gov.cida.cdat.exception.control;

/**
 * Thrown if no process exists with the given id for status or control
 * 
 * @author duselmann
 */
public class ServiceNotFoundException extends ControlException {

	private static final long serialVersionUID = 1L;

	public ServiceNotFoundException() {}
	public ServiceNotFoundException(String msg) {super(msg);}
	public ServiceNotFoundException(String msg, Throwable cause) {super(msg,cause);}
	
}
