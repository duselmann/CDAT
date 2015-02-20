package gov.cida.cdat;

import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Service;
import gov.cida.cdat.control.Worker;

public class HelloWorld {
	
	public static void main(String[] args) {
		Service session = Service.open();
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
			
			// this is not necessary if session.close() is called so near by
			session.send(name, Control.Stop);
			
		} finally {
			session.close();
		}		
	}
}
