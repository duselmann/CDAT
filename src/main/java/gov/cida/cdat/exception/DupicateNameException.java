package gov.cida.cdat.exception;

public class DupicateNameException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DupicateNameException() {}
	public DupicateNameException(String msg) {super(msg);}
	public DupicateNameException(String msg, Throwable cause) {super(msg,cause);}

}
