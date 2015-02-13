package gov.cida.cdat.service;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.service.Worker;
import static org.junit.Assert.*;

import org.junit.Test;


public class DelegatorAutoStartTest {

	private static SCManager session;
	
	
	@Test
	public void testAutoStart() throws Exception {
		session = SCManager.open();

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
			// TODO this is why I would like a session reset/dispose after leaving scope or similar
			session.setAutoStart(false);
		}
	}
}
