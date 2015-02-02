package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStreamContainer;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO ensure that the fail is handled by the session strategy and that the worker is disposed
public class TestControlFail {

	private static ByteArrayOutputStream target;
	private static String workerName;
	private static SCManager manager;
	
	
	public static void main(String[] args) throws Exception {
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
		
		workerName = manager.addWorker("error", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Control.Start);
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message response){
	            report(response);
	        }
	    });

		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message response) {
				System.out.println("service shutdown");
				manager.shutdown();
			}
		});
	}
	
	
	private static void report(final Message response) {
        System.out.println("onComplete Response is " + response);
		
		System.out.println();
		System.out.println("pipe results: expect zero length, handled by session, and pool system continue running");
		System.out.println( "total bytes: " +target.size() );
		System.out.println( new String(target.toByteArray()) );
		
		String qual = "NOT ";
		if (null != response.get(Status.isError)) {
			qual = "";
		}
		System.out.println("response message DOES " +qual+ "contain isError message" );
		System.out.println("response isError => " + response.get(Status.isError) );

		System.out.println();
		System.out.println();
	}
}
