package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.exception.CdatException;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;

// there should be no name conflicts because each thread will have its own session
public class SCManagerAdminTokenTests {

	private static Set<String> sessionNames = new HashSet<String>();
	private static Set<String> workerNames  = new HashSet<String>();
	private static SCManager[] sessions     = new SCManager[1];
	private static Boolean[]  processCalled = new Boolean[1];
	
	@Test
	public void testMultiThreadedRequests() throws Exception {
		String token = TestUtils.reflectValue(SCManager.class, "TOKEN").toString();
		
		// start a 'session' with the admin token
		SCManager session = SCManager.open(token);
		
		try {
			// start another session on another thread that is not ADMIN
			spawnThread("OTHER");
			// wait for the OTHER session to commence with a worker
			TestUtils.waitAlittleWhileForResponse(sessions);

			// make sure that the OTHER session is named what we expect
			assertEquals("Expect 1 unique session name", 1, sessionNames.size());
			assertEquals("Expect 1 unique worker name", 1, sessionNames.size());
			String sessionName = sessionNames.iterator().next();
			String workerName  = workerNames.iterator().next();

			// test that the session for the worker created on the OTHER session can be found
			ActorRef sesRef = session.session(workerName);
			assertEquals("Expect " +workerName+ " for be found on SESSION-1", sessionName, sesRef.path().name());
			
			// test that the admin token allows control over OTHER session workers
			session.send(workerName, Control.Start);
			TestUtils.waitAlittleWhileForResponse(processCalled);
			assertTrue("expect that the admin token allows starting other session workers", processCalled[0]);
			
		} finally {
//			System.out.println("shuttdown submitted");
//			manager.shutdown();
			sessions[0].close();
		}
	}


	private static void spawnThread(final String label) {
		System.out.println("starting " +label+ " new thread");
		new Thread(new Runnable() {
			private final Logger logger = LoggerFactory.getLogger(getClass());
			@Override
			public void run() {
				try {
					logger.debug("running off main thread on {} thread", label);
					submitJob(label);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}, label).start(); // remember not to use run
	}


	private static void submitJob(final String threadName) throws MalformedURLException {
		final SCManager session = sessions[0] = SCManager.open();

		Worker worker = new Worker(){
			@Override
			public boolean process() throws CdatException {
				processCalled[0] = true;
				return super.process();
			}
		};
		final String workerName = session.addWorker(threadName, worker);
		
		sessionNames.add(session.sessionName());
		workerNames.add(workerName);
		
		// nothing is running and the admin session will be starting a job
		// we do not want to close the session but I have this here to show
		// that is has not been forgotten
		//session.close();
	}

}
