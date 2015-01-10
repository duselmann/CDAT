package gov.cida.cdat.service.combined;


public abstract class Worker { 
	
	double id = Math.random();
	String name;
	
	public Worker(String name) {
		this.name = name;
	}

	// open, connect, configure, etc
	public void begin() throws Exception { // TODO use framework exceptions
		System.out.println("Worker begin: " + name);
	}
	// process a row or data element
	public void process(byte[] b, int off, int len) {
		System.out.println("Worker process: " + name);
	}
	// close, disconnect, cleanup resources, etc
	public void end() {
		System.out.println("Worker end: " + name);
	}
	
}
