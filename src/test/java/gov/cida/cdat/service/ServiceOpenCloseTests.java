package gov.cida.cdat.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.io.Closer;

import org.junit.Test;

public class ServiceOpenCloseTests {

	@Test
	public void testSessionOpenClose() {
		
		Service session = Service.open();
		final String firstSession = session.sessionName();
				
		try {
			String workerLabel = "testWorkerA";
			String response[] = runWorker(workerLabel, session);
			assertEquals("expect worker A to run", workerLabel, response[0]);
			
		} finally {
			Closer.close(session);
		}
		
		session = Service.open();
		final String secondSession = session.sessionName();
				
		try {
			String workerLabel = "testWorkerB";
			String response[] = runWorker(workerLabel, session);
			assertEquals("expect worker B to run", workerLabel, response[0]);
			
		} finally {
			Closer.close(session);
		}
		
		assertNotEquals("expect new session after session close", firstSession, secondSession);
	}
	
	private String[] runWorker(final String workerLabel, Service session) {
		final String[] response = new String[1];
		Worker testWorker = new Worker() {
			public boolean process() {
				response[0] = workerLabel;
				return false; // Answers the question: Is there more?
			}
		};
		String name = session.addWorker(workerLabel, testWorker);
		session.send(name, Control.Start);

		Time.waitForResponse(response,100);
		// sending Stop is not required if session.close() or Closer.close(session) is called so near by
		session.send(name, Control.Stop);
		
		return response;
	}

}
