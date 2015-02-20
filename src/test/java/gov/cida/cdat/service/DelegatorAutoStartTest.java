package gov.cida.cdat.service;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.exception.CdatException;
import static org.junit.Assert.*;

import org.junit.Test;


public class DelegatorAutoStartTest {

	private static Service session;
	
	
	@Test
	public void testAutoStart() throws Exception {
		session = Service.open();

		session.setAutoStart(true);
		
		try {
		
			final Boolean[] processCalled = new Boolean[1];
			final String workerName = session.addWorker("autoStartTest",  new Worker() {
				@Override
				public boolean process() throws CdatException {
					processCalled[0] = true;
					return false; // tell the system that there NO more to process
				}
			});
	
			TestUtils.waitAlittleWhileForResponse(processCalled);
			
			assertTrue("process should be called without explicit start message when autostart",
					processCalled[0]);
			
			session.send(workerName, Control.Stop);
			
		} finally {
			session.close();
		}
	}
}
