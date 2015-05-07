package gov.cida.cdat.service;


import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.Closer;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;

// there should be no name conflicts because each thread will have its own session
public class ServiceAdminTokenTests {

	private static Set<String> sessionNames;
	private static Set<String> workerNames;
	private static Service[] sessions;
	private static Boolean[]  processCalled;
	
	@Before
	public void setup() {
		sessionNames = new HashSet<String>();
		workerNames  = new HashSet<String>();
		sessions     = new Service[1];
		processCalled = new Boolean[1];
	}
	
	@Test
	public void testMultiThreadedRequests() throws Exception {
		String token = TestUtils.reflectValue(Service.class, "TOKEN").toString();
		
		// start a 'session' with the admin token
		Service session = Service.open(token);
		
		try {
			// start another session on another thread that is not ADMIN
			spawnThread("OTHER");
			// wait for the OTHER session to commence with a worker
			Time.waitForResponse(sessions,100);

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
			Time.waitForResponse(processCalled,100);
			assertTrue("expect that the admin token allows starting other session workers", processCalled[0]);
			
		} finally {
//			System.out.println("shuttdown submitted");
//			manager.shutdown();
			Closer.close(sessions[0]);
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
		final Service session = sessions[0] = Service.open();

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
		//Closer.close(session);
	}

/*	
	@Test
	public void testInfoIsNew() {
		String token = TestUtils.reflectValue(Service.class, "TOKEN").toString();
		
		// start a 'session' with the admin token
		Service session = Service.open(token);
		
		try {
//			Worker workerA = new Worker(){};
//			Worker workerB = new Worker(){};
//			
//			String nameA = session.addWorker("workerA", workerA);
//			String nameB = session.addWorker("workerB", workerB);

			spawnThread("workerA");
			spawnThread("workerB");
			
			final Message[] message = new Message[1];
			Message getInfo = Message.create(Control.info, Service.SESSION);
			session.send(Service.SESSION, getInfo, new Callback() {
				@Override
				public void onComplete(Throwable t, Message response) {
					message[0] = response;
					TestUtils.log(response);
				}
			});
			
			Time.waitForResponse(message,100);
			
			TestUtils.log(message[0]);
			
//			assertEquals("Expect info on workerA to be "+Status.isNew,
//					Status.isNew.toString(), message[0].get(nameA));
//			assertEquals("Expect info on workerB to be "+Status.isNew,
//					Status.isNew.toString(), message[0].get(nameB));
		} finally {
			Closer.close(session);
		}
	}
*/
}
