package gov.cida.cdat.service;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// there should be no name conflicts because each thread will have its own session
public class ServiceControlThreadedTest {

	private static ByteArrayOutputStream consumer;
	private static String workerLabel = "producer";
	private static Message[] messages = new Message[4]; // this must be equal or larger than the number of threads in the test
	private static Set<String> sessionNames = new HashSet<String>();
	private static Set<String> workerNames  = new HashSet<String>();
	
	@Test
	public void testMultiThreadedRequests() throws Exception {
		try {
			// no delay test
			spawnThread("first");
			spawnThread("second");
			
			// delayed test
			Thread.sleep(500);
			spawnThread("third");
			
			Thread.sleep(500);
			spawnThread("fourth");
			
			Thread.sleep(500);
		} finally {
//			System.out.println("shuttdown submitted");
//			manager.shutdown();
		}

		// this might not be necessary because of the thread sleeps
		Time.waitForResponse(messages,100);
		
		for (Message msg : messages) {
			Assert.assertEquals("Message for onComplete should be 'done'", 
					"done", msg.get(Control.onComplete));
		}
		
		TestUtils.log(sessionNames);
		TestUtils.log(workerNames);
		Assert.assertEquals("Expect four unique session names", 4, sessionNames.size());
		Assert.assertEquals("Expect four unique worker names", 4, workerNames.size());
		TestUtils.log(sessionNames);
		TestUtils.log(workerNames);
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
		// consumer
		consumer = new ByteArrayOutputStream(1024*10);
		SimpleStreamContainer<OutputStream> out = new SimpleStreamContainer<OutputStream>(consumer);
		
		// producer
		StringBuilder buf =  new StringBuilder();
		for (int i=0; i<100; i++) {buf.append(threadName);}
		InputStream producer = new ByteArrayInputStream(buf.toString().getBytes());
		SimpleStreamContainer<InputStream>  in  = new SimpleStreamContainer<InputStream>(producer);
		
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);
		Worker worker = new PipeWorker(pipe);
		
		Service session = Service.open();
		
		try {
			final String workerName = session.addWorker(workerLabel, worker);
			
			sessionNames.add(session.sessionName());
			workerNames.add(workerName);
			
			session.send(workerName, Message.create("Message", "Test"));
			
			// This is called with a null response if the Patterns.ask timeout expires
			session.send(workerName, Control.onComplete, new Callback(){
		        public void onComplete(Throwable t, Message response){
		        	messages[threadNameToIndex(threadName)] = response;
		            report(threadName, workerName, response);
		        }
	
				private int threadNameToIndex(String threadName) {
					switch (threadName) {
						case "second": return 1;
						case "third" : return 2;
						case "fourth": return 3;
						default:break;
					}
					// "first"
					return 0;
				}
		    });
			
			// the starting and stopping is not tested here
			session.send(workerName, Control.Start);
//            session.send(workerName, Control.Stop);
			
		} finally {
			Closer.close(session);  // SESSION-20,SESSION-21,SESSION-22,SESSION-23
		}
	}
	
	
	private static void report(String threadName, String workerName, final Message response) {
        System.out.println("onComplete Response is " + response);
		
		System.out.println("pipe results");
		System.out.println( "total bytes: " +consumer.size() );
		if (consumer.size()>100) {
			System.out.println( new String(consumer.toByteArray(), 0, 100) );
		} else {
			System.out.println("ERROR: Received too little data.");
		}
		String msg = "'" +threadName+ "' Not Found";
		if ( new String(consumer.toByteArray()).contains(threadName) ) {
			msg = "'" +threadName+ "' Found !";
		}
		System.out.println();
		System.out.println(msg);
	}

}
