package gov.cida.cdat;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.io.Closer;

public class HelloWorld {
	
	public static void main(String[] args) {
		SCManager session = SCManager.open();
		try {
			Worker helloWorld = new Worker() {
				public boolean process() {
					System.out.println();
					System.out.println("Hello World");
					System.out.println();
					return false; // Answers the question: Is there more?
				}
			};
			String name = session.addWorker("HelloWorld", helloWorld);
			session.send(name, Control.Start);
			
			// sending Stop is not required if session.close() or Closer.close(session) is called so near by
			session.send(name, Control.Stop);
			
		} finally {
			Closer.close(session);
		}		
	}
}
