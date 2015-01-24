package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.io.stream.UrlStream;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;


public class TestControlSuccess {

	private static ByteArrayOutputStream target;
	private static SCManager manager;
	
	
	public static void main(String[] args) throws Exception {
		manager = SCManager.instance();

		// consumer
		target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream>     out = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream in = new UrlStream(url);
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);		
		
		final String workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		
		// This is called with a null response if the Patterns.ask timeout expires
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message response){
	            report(workerName, response);		
	        }
	    });
		
		manager.send(workerName, Control.Start);
		
	}
	
	
	private static void report(String workerName, final Message response) {
        System.out.println("onComplete Response is " + response);
		
		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message response) {
				System.out.println("service shutdown scheduled");
				manager.shutdown();
			}
		});
		
		System.out.println("pipe results: expect >15kb with Google found in the text");
		System.out.println( "total bytes: " +target.size() );
		if (target.size()>100) {
			System.out.println( new String(target.toByteArray(), 0, 100) );
		} else {
			System.out.println("ERROR: Received too little data.");
		}
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
