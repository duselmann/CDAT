package gov.cida.cdat.exception;

public class CdatException extends Exception {

	private static final long serialVersionUID = 1L;

	public CdatException() {}
	public CdatException(String msg) {super(msg);}
	public CdatException(String msg, Throwable cause) {super(msg,cause);}
	
}
