package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStreamContainer;
import gov.cida.cdat.io.stream.UrlStreamContainer;
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
		SimpleStreamContainer<OutputStream>     out = new SimpleStreamContainer<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStreamContainer in = new UrlStreamContainer(url);
		
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
		
		System.out.println("waiting for worker to process");
		Thread.sleep(3000);
		System.out.println("send stop to worker");
		
		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message response) {
				System.out.println("service shutdown scheduled: "+ response);
				manager.shutdown();
	            // this will execute off the main thread
	            // if manager is sent a message it WILL be on a DIFFERENT session
			}
		});
	}
	
	
	private static void report(String workerName, final Message response) {
		System.out.println();
        System.out.println("onComplete Response is " + response);
		
		System.out.println("pipe results: expect >15kb with Google found in the text");
		System.out.println( "total bytes: " +target.size() );
		if (target.size()>100) {
			byte[] bytes = target.toByteArray();
			System.out.println( new String(bytes, 0, 100) );
			System.out.println("...");
			System.out.println( new String(bytes, bytes.length-100, 100) );
		} else {
			System.out.println("ERROR: Received too little data.");
		}
		String msg = "Google Not Found";
		String ggl = new String(target.toByteArray());
		if ( ggl.contains("Google") ) {
			msg = "Google Found";
		}
		if ( ggl.contains("</html>") ) {
			msg += " and </html>";
		}
		System.out.println();
		System.out.println(msg);
		System.out.println();
	}
}
