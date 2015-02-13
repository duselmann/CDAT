package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.service.Worker;

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
			String sessionName = "SESSION-1";
			assertEquals("Expect 1 unique session names", 1, sessionNames.size());
			assertTrue("Expect SESSION-1 to be created", sessionNames.contains(sessionName));

			// test that the session for the worker created on the OTHER session can be found
			String workerName = workerNames.iterator().next();
			ActorRef sesRef = session.session(workerName);
			assertEquals("Expect " +workerName+ " for be found on SESSION-1", sessionName, sesRef.path().name());
			
			// test that the admin token allows control over OTHER session workers
			session.send(workerName, Control.Start);
			TestUtils.waitAlittleWhileForResponse(processCalled);
			assertTrue("expect that the admin token allows starting other session workers", processCalled[0]);
			
			session.send(workerName, Control.Stop);
			sessions[0].close();
		} finally {
//			System.out.println("shuttdown submitted");
//			manager.shutdown();
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
		final SCManager manager = sessions[0] = SCManager.instance();

		Worker worker = new Worker(){
			@Override
			public boolean process() throws CdatException {
				processCalled[0] = true;
				return super.process();
			}
		};
		final String workerName = manager.addWorker(threadName, worker);
		
		sessionNames.add(manager.sessionName());
		workerNames.add(workerName);
	}

}
