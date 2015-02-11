package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Worker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

// TODO ensure that the fail is handled by the session strategy and that the worker is disposed
public class SCManagerControlFailTest {

	private static ByteArrayOutputStream target;
	private static String workerName;
	private static SCManager manager;
	
	@Test
	public void testFailResponse() throws Exception {
		manager = SCManager.instance();

		// consumer
		target = new ByteArrayOutputStream(1024*10);
		SimpleStreamContainer<OutputStream>     out = new SimpleStreamContainer<OutputStream>(target);
		
		// producer
		InputStream error = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
		SimpleStreamContainer<InputStream> in = new SimpleStreamContainer<InputStream>(error);
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);
		Worker worker = new PipeWorker(pipe);

		workerName = manager.addWorker("error", worker);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Control.Start);
		
		final Message[] message = new Message[1];
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message response){
	        	message[0] = response;
	            report(response);
	        }
	    });

		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message response) {
				TestUtils.log("service shutdown");
				manager.shutdown();
			}
		});
		
		TestUtils.waitAlittleWhileForResponse(message);
		
		Assert.assertTrue("", message[0].toString().contains("Error reading from producer stream"));
	}
	
	
	private static void report(final Message response) {
		TestUtils.log("onComplete Response is ", response);
		TestUtils.log("pipe results: expect zero length, handled by session, and pool system continue running");
		TestUtils.log( "total bytes: ", target.size() );
		TestUtils.log( new String(target.toByteArray()) );
		
		String qual = "NOT ";
		if (null != response.get(Status.isError)) {
			qual = "";
		}
		TestUtils.log("response message DOES ", qual, "contain isError message" );
		TestUtils.log("response isError => ", response.get(Status.isError) );
	}
}
