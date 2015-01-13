package gov.cida.cdat.service.combined;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Worker { 
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	double id = Math.random();
	String name;
	
	public Worker(String name) {
		this.name = name;
	}

	// open, connect, configure, etc
	public void begin() throws Exception { // TODO use framework exceptions
		logger.trace("Worker begin: " + name);
	}
	// process a row or data element
	public void process(byte[] b, int off, int len) {
		logger.trace("Worker process: " + name);
	}
	// close, disconnect, cleanup resources, etc
	public void end() {
		logger.trace("Worker end: " + name);
	}
	
}
