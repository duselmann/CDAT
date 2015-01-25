package gov.cida.cdat.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import akka.actor.ActorRef;


public class SCManagerTests {

	public static int waitAlittleWhileForResponse(Message response[]) {
		int count=0;
		while (null==response[0] && count++ < 100) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		return count;
	}
	
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
		String TEST_STRING = "Test String";

		// set up consumer
		ByteArrayOutputStream consumer = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out = new SimpleStream<OutputStream>(consumer);
		
		// set up producer
		ByteArrayInputStream producer  = new ByteArrayInputStream(TEST_STRING.getBytes());
		SimpleStream<InputStream>   in = new SimpleStream<InputStream>(producer);
		
		// pipe: connect the consumer and producer together with a data pump
		DataPipe      pipe = new DataPipe(in, out);
		
		// submit the pipe as a worker to the manager on the session
		final String  workerName = manager.addWorker("google", pipe);

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

		// TODO should the manager have an feature to auto start and stop workers?
		
		// wait some time for the worker to finish
		int count = waitAlittleWhileForResponse(response);
		
    	// this send the message that this worker is no longer needed
		manager.send(workerName, Control.Stop);
		
		// lets just see the count
		System.out.println();
		System.out.println("wait cycle count: " + count);
		System.out.println();
		
		assertTrue("Expect count to be must less than 100", count<100);
		
		// now test that the message is as we expect
		assertFalse("Message from on complete should not be null", response[0]==null );
		assertEquals("OnComplete Message from on complete should be 'done'", "done",  response[0].get(Control.onComplete) );
		
		// test that the data was pumped from producer to consumer
		assertEquals("Expected output: " + TEST_STRING, TEST_STRING, new String(consumer.toByteArray()) );
    }
	
	
	@Test
	// it is hard to test only one of these phases
	public void testWorker_() throws Exception {
		
		System.out.println();
		System.out.println("Start testing a second start");
		System.out.println();
		
		// obtain an instance of the manager
		final SCManager  manager = SCManager.instance();

		// a test string that can be used for comparison
		String TEST_STRING = "Test String";

		// set up consumer
		ByteArrayOutputStream consumer = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out = new SimpleStream<OutputStream>(consumer);
		
		// set up producer
		ByteArrayInputStream producer  = new ByteArrayInputStream(TEST_STRING.getBytes());
		SimpleStream<InputStream>   in = new SimpleStream<InputStream>(producer);
		
		// pipe: connect the consumer and producer together with a data pump
		DataPipe      pipe = new DataPipe(in, out);
		
		// submit the pipe as a worker to the manager on the session
		final String  workerName = manager.addWorker("google", pipe);

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

		// TODO should the manager have an feature to auto start and stop workers?
		
		// wait some time for the worker to finish
		waitAlittleWhileForResponse(response);

    	// this send the message that this worker is no longer needed
		manager.send(workerName, Control.Stop);

		System.out.println();
		System.out.println("Issuing second start");
		System.out.println();
		
		// clear out the consumer to see if it gets refilled
		consumer.reset();
		
		// try to start it up again
		manager.send(workerName, Control.Start);
		
		// wait some time for the worker to finish
		waitAlittleWhileForResponse(response);
		
		// now test that the consumer remains empty
		assertEquals("Expect that the disposed worker does not execute", "", new String(consumer.toByteArray()) );
    }
	// TODO need to check why the callback has a different thread. it could be a junit thing
}
