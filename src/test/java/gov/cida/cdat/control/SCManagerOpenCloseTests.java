package gov.cida.cdat.control;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;

import org.junit.Test;

public class SCManagerOpenCloseTests {

	@Test
	public void testSessionOpenClose() {
		
		SCManager session = SCManager.open();
		final String firstSession = session.sessionName();
				
		try {
			String workerLabel = "testWorkerA";
			String response[] = runWorker(workerLabel, session);
			assertEquals("expect worker A to run", workerLabel, response[0]);
			
		} finally {
			session.close();
		}
		
		session = SCManager.open();
		final String secondSession = session.sessionName();
				
		try {
			String workerLabel = "testWorkerB";
			String response[] = runWorker(workerLabel, session);
			assertEquals("expect worker B to run", workerLabel, response[0]);
			
		} finally {
			session.close();
		}
		
		assertNotEquals("expect new session after session close", firstSession, secondSession);
	}
	
	private String[] runWorker(final String workerLabel, SCManager session) {
		final String[] response = new String[1];
		Worker testWorker = new Worker() {
			public boolean process() {
				response[0] = workerLabel;
				return false; // Answers the question: Is there more?
			}
		};
		String name = session.addWorker(workerLabel, testWorker);
		session.send(name, Control.Start);

		TestUtils.waitAlittleWhileForResponse(response);
		// this is not necessary if session.close() is called so near by
		session.send(name, Control.Stop);
		
		return response;
	}

}
