package gov.cida.cdat.exception;

public class StreamInitException extends Exception {

	private static final long serialVersionUID = 1L;

	public StreamInitException() {}
	public StreamInitException(String msg) {super(msg);}
	public StreamInitException(String msg, Throwable cause) {super(msg,cause);}
	
}
