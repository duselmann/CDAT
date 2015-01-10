package gov.cida.cdat.services;


public abstract class Worker { 

	// connect, configure, etc
	public void begin() throws Exception { // TODO use framework exceptions
		System.out.println("Worker begin.");
	}
	// process a row or data element
	public void process(byte[] b) {
		System.out.println("Worker process.");
	}
	// cleanup resources and close
	public void end() {
		System.out.println("Worker end.");
	}
	
}
