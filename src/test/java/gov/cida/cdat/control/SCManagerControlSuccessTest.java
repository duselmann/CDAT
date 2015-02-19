package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.service.PipeWorker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

import org.junit.Test;


public class SCManagerControlSuccessTest {

	private static ByteArrayOutputStream target;
	private static byte[] dataRef;
	
	@Test
	public void testSuccessfulJobRun_SubmitStartProcessStopOnCompleteAndCustomMessage() throws Exception {
		SCManager session = SCManager.open();

		try {
			// consumer
			target = new ByteArrayOutputStream(1024*10);
			SimpleStreamContainer<OutputStream> out = new SimpleStreamContainer<OutputStream>(target);
			
			// producer
			dataRef = TestUtils.sampleData();
			InputStream source = new ByteArrayInputStream(dataRef);
			SimpleStreamContainer<InputStream>  in = new SimpleStreamContainer<InputStream>(source);
			
			// pipe
			DataPipe pipe = new DataPipe(in, out);
			Worker worker = new PipeWorker(pipe);
			
			final String workerName = session.addWorker("byteStream", worker);
			
			System.out.println("send custom message to worker");
			session.send(workerName, Message.create("Message", "Test"));
			
			final Message[] completed = new Message[1];
			// This is called with a null response if the Patterns.ask timeout expires
			session.send(workerName, Control.onComplete, new Callback(){
		        public void onComplete(Throwable t, Message response){
		        	completed[0] = response;
		        }
		    });
	
			System.out.println("send start to worker");
			session.send(workerName, Control.Start);
			
			System.out.println("waiting for worker to process");
			TestUtils.waitAlittleWhileForResponse(completed);
	
			assertTrue("DataPipe should be complete when finished", pipe.isComplete());
			assertEquals("producer stream should be null after close", null, pipe.getProducerStream());
			assertEquals("consumer stream should be null after close", null, pipe.getConsumerStream());
			
	        report(workerName, completed[0]);
	
			System.out.println("send stop to worker");
			session.send(workerName, Control.Stop, new Callback() {
				public void onComplete(Throwable t, Message response) {
	//				System.out.println("service shutdown scheduled: "+ response);
	//				manager.shutdown();
		            // this will execute off the main thread
		            // if manager is sent a message it WILL be on a DIFFERENT session
				}
			});
			
		} finally {
			session.close();
		}
	}
	
	
	private static void report(String workerName, final Message response) {
		TestUtils.log("onComplete Response is ", response);
		
		TestUtils.log("pipe results: expect >15kb with Google found in the text");
		TestUtils.log( "total bytes: ", target.size() );
		
		if (target.size()>100) {
			byte[] bytes = target.toByteArray();
			System.out.println( new String(bytes, 0, 100) );
			System.out.println("...");
			System.out.println( new String(bytes, bytes.length-100, 100) );
		} else {
			TestUtils.log("ERROR: Received too little data.");
		}
		String msg = "'middle' Not Found";
		String str = new String(target.toByteArray());
		if ( str.contains("middle") ) {
			msg = "'middle' Found";
		}
		if ( str.contains("end") ) {
			msg += " and 'end'";
		}

		assertTrue("Expect to find 'begin' in results", str.contains("begin"));
		assertTrue("Expect to find 'middle' in results", str.contains("middle"));
		assertTrue("Expect to find 'end' in results", str.contains("end"));
		assertFalse("Expect to NOT find 'zoo' in results", str.contains("zoo"));
		
		TestUtils.log(msg);
	}
}
