package gov.cida.cdat.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.Naming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import akka.actor.ActorRef;


public class SCManagerTests {

	/*
	 * Support Methods
	 */
	
	
	public static class PipeTestObj {
		public ByteArrayInputStream  producer;
		public ByteArrayOutputStream consumer;
		public DataPipe       pipe;
		
		public String results() {
			if (consumer == null) {
				return null;
			}
			return new String(consumer.toByteArray());
		}
	}
	

	public static int waitAlittleWhileForResponse(Message response[]) {
		int count=0;
		while (null==response[0] && count++ < 100) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		return count;
	}
	
	public static PipeTestObj createTestPipe(String textString) {
		PipeTestObj testPipe = new PipeTestObj();
		
		// set up consumer
		testPipe.consumer = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out = new SimpleStream<OutputStream>(testPipe.consumer);
		
		// set up producer
		testPipe.producer  = new ByteArrayInputStream(textString.getBytes());
		SimpleStream<InputStream>   in = new SimpleStream<InputStream>(testPipe.producer);
		
		// pipe: connect the consumer and producer together with a data pump
		testPipe.pipe = new DataPipe(in, out);
				
		return testPipe;
	}
	
	public static void print(Object ... objs) {
		// used to print a result that stands out from the logs
		// use logging or println itself if this is not needed
		
		System.out.println();
		for (Object obj:objs) {
			System.out.print(obj==null ?"null" :obj.toString());
			System.out.print(" ");
		}
		System.out.println();
		System.out.println();
	}
	
	
	/*
	 * TESTS FOLLOW
	 */
	
	
	@Test
	public void testSingleton() {
		SCManager instance1 = SCManager.instance();
		SCManager instance2 = SCManager.instance();
		
		assertEquals("Singleton instances should be equivilent", instance1, instance2);
		assertTrue("Singleton instances should be equivilent references", instance1 == instance2);
    }
	
	@Test
	public void testSession_SameSession() {
		SCManager manager = SCManager.instance();
		ActorRef instance1 = manager.session();
		ActorRef instance2 = manager.session();
		
		assertEquals("Session instances should be equivilent", instance1, instance2);
		assertTrue("Session instances should be equivilent references", instance1 == instance2);
    }
	
	@Test
	public void testSession_DifferentTheadsDifferentSessions() throws Exception {
		final SCManager manager = SCManager.instance();
		
		ActorRef instance1 = manager.session();
		
		final ActorRef[] sessionRef = new ActorRef[1];
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				sessionRef[0] = manager.session();
			}
		});
		thread.start();
		thread.join();
		
		ActorRef instance2 = sessionRef[0];

		assertFalse("Session instances should be different across threads", instance1 == instance2);
    }

	
	@Test
	// it is hard to test only one of these phases
	public void testWorker_StartRunCompleteStop() throws Exception {
		
		// obtain an instance of the manager
		final SCManager  manager = SCManager.instance();

		// a test string that can be used for comparison
		String TEST_STRING = "Test String A";

		PipeTestObj testPipe = createTestPipe(TEST_STRING);
		
		// submit the pipe as a worker to the manager on the session
		final String  workerName = manager.addWorker(TEST_STRING, testPipe.pipe);

		final String EXPECTED = TEST_STRING.replaceAll(" ","_")+"-1";
		// ensure that the response contains the worker name since a future is returned from the submit
		assertEquals("we expect the worker name returned from addWorker", EXPECTED, workerName);
		
		// this is a holder to pass data between threads.
		final Message response[] = new Message[1];
		
		// wait for worker to complete
		manager.send(workerName, Control.onComplete, new Callback(){
    		// This is called with a null response if the Patterns.ask timeout expires
	        public void onComplete(Throwable t, Message resp) {
	        	// get a reference to the message
	    		response[0] = resp;
	        }
	    });
		// start the worker (from cDAT point of view, from AKKA it is already running)
		manager.send(workerName, Control.Start);

		// wait some time for the worker to finish
		int count = waitAlittleWhileForResponse(response);
		
    	// this send the message that this worker is no longer needed
		manager.send(workerName, Control.Stop);
		
		// lets just see the count
		print("wait cycle count:", count);
		
		assertTrue("Expect count to be must less than 100", count<100);
		
		// now test that the message is as we expect
		assertFalse("Message from on complete should not be null", response[0]==null );
		assertEquals("OnComplete Message from on complete should be 'done'", "done",  response[0].get(Control.onComplete) );
		
		// test that the data was pumped from producer to consumer
		assertEquals("Expected output: " + TEST_STRING, TEST_STRING, testPipe.results() );
    }
		
	
	@Test
	// TODO would also like to check for memory leaks
	public void testWorker_workerDisposeOnStop() throws Exception {
		
		print("Start testing a second start");
		
		// obtain an instance of the manager
		final SCManager  manager = SCManager.instance();

		// a test string that can be used for comparison
		String TEST_STRING = "Test String B";

		PipeTestObj testPipe = createTestPipe(TEST_STRING);

		// submit the pipe as a worker to the manager on the session
		final String  workerName = manager.addWorker(TEST_STRING, testPipe.pipe);

		// this is a holder to pass data between threads.
		final Message response[] = new Message[1];
		
		// wait for worker to complete
		manager.send(workerName, Control.onComplete, new Callback(){
    		// This is called with a null response if the Patterns.ask timeout expires
	        public void onComplete(Throwable t, Message resp) {
	        	// get a reference to the message
	    		response[0] = resp;
	        }
	    });
		// start the worker (from cDAT point of view, from AKKA it is already running)
		manager.send(workerName, Control.Start);
		
		// wait some time for the worker to finish
		waitAlittleWhileForResponse(response);

    	// this send the message that this worker is no longer needed
		manager.send(workerName, Control.Stop);

		print("Issuing second start");
		
		// clear out the consumer to see if it gets refilled
		testPipe.consumer.reset();
		
		// try to start it up again
		manager.send(workerName, Control.Start);
		
		// wait some time for the worker to finish
		waitAlittleWhileForResponse(response);
		
		// now test that the consumer remains empty
		assertEquals("Expect that the disposed worker does not execute", "", testPipe.results() );
    }
	
	@Test
	// it is hard to test only one of these phases
	public void testWorker_withCallback() throws Exception {
		
		// obtain an instance of the manager
		final SCManager  manager = SCManager.instance();

		// a test string that can be used for comparison
		String TEST_STRING = "Test String C";

		PipeTestObj testPipe = createTestPipe(TEST_STRING);
		
		// this is a holder to pass data between threads.
		final Message response[] = new Message[1];
		
		// submit the pipe as a worker to the manager on the session
		manager.addWorker(TEST_STRING, testPipe.pipe, new Callback(){
    		// This is called with a null response if the Patterns.ask timeout expires
	        public void onComplete(Throwable t, Message resp) {
	        	// get a reference to the message
	    		response[0] = resp;
	        }
	    });

		// wait some time for the worker to finish
		waitAlittleWhileForResponse(response);
		
		final String EXPECTED = TEST_STRING.replaceAll(" ","_")+"-1";
		final String workerName = response[0].get(Naming.WORKER_NAME);
		// ensure that the response contains the worker name since a future is returned from the submit
		assertEquals("we expect the worker name in the Callback response Message", EXPECTED, workerName);
		
    	// this send the message that this worker is no longer needed
		manager.send(workerName, Control.Stop);
    }
	
	@Test
	public void testCreateNameFromLabel_spacesToUnderscore() throws Exception {
		
		// obtain an instance of the manager
		SCManager  manager     = SCManager.instance();
		
		final String RAW_LABEL = "a b c";
		final String EXPECT    = "a_b_c-1";
		
		String result    = manager.createNameFromLabel(RAW_LABEL);
		
		assertEquals("we expect that a label spaces are transposed to underscores '_'", EXPECT, result);
	}
	
	
	@Test
	public void testCreateNameFromLabel_initialSuffix() throws Exception {
		
		// obtain an instance of the manager
		SCManager  manager     = SCManager.instance();
		
		final String RAW_LABEL = "abc";
		final String EXPECT    = "abc-1";
		
		String result    = manager.createNameFromLabel(RAW_LABEL);
		
		assertEquals("we expect that a label is ensured unique with a suffix", EXPECT, result);
	}
	
	@Test
	public void testCreateNameFromLabel_secondaryInitialSuffix() throws Exception {
		
		// obtain an instance of the manager
		SCManager  manager      = SCManager.instance();
		
		final String RAW_LABEL1 = "qrs";
		final String EXPECT1    = "qrs-1";
		
		final String RAW_LABEL2 = "xyz";
		final String EXPECT2    = "xyz-1";
		
		String result1    = manager.createNameFromLabel(RAW_LABEL1);
		String result2    = manager.createNameFromLabel(RAW_LABEL2);
		
		assertEquals(EXPECT1, result1);
		assertEquals("we expect that each new label has its own suffix count", EXPECT2, result2);
	}
	
	@Test
	public void testCreateNameFromLabel_secondarySuffix_ensureUniqueness() throws Exception {
		
		// obtain an instance of the manager
		SCManager  manager      = SCManager.instance();
		
		final String RAW_LABEL = "www";
		final String EXPECT1    = "www-1";
		final String EXPECT2    = "www-2";
		final String EXPECT3    = "www-3";
		
		String result1    = manager.createNameFromLabel(RAW_LABEL);
		String result2    = manager.createNameFromLabel(RAW_LABEL);
		String result3    = manager.createNameFromLabel(RAW_LABEL);
		
		assertEquals("we always expect the first suffix to be '-1' ", EXPECT1, result1);
		assertEquals("we expect that each subsequent name request for the same label has an incremented suffix - should be '-2' ",
				EXPECT2, result2);
		assertEquals("we expect that each subsequent name request for the same label has an incremented suffix - should be '-3' ",
				EXPECT3, result3);
	}
	
	
	
	// TODO should the manager have an feature to auto start and stop workers?
	// TODO need to check why the callback has a different thread. it could be a junit thing
}
