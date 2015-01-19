package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.io.stream.PipeStream;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.io.stream.UrlStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;


public class TestControlFail {

	private static ByteArrayOutputStream target;
	private static String workerName;
	private static SCManager manager;
	
	
	public static void main(String[] args) throws Exception {
		manager = SCManager.get();

		// consumer
		target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream>     out = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.asdfsdfasdf.com");
		UrlStream in = new UrlStream(url);
		
		// pipe
		PipeStream pipe = new PipeStream(in, out);		
		
		workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Control.Start);
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message repsonse){
	            report(repsonse);		
	        }
	    });
	}
	
	
	private static void report(final Message repsonse) {
        System.out.println("onComplete Response is " + repsonse);
		
		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message repsonse) throws Throwable {
				System.out.println("service shutdown");
				manager.shutdown();
			}
		});
		System.out.println("pipe results");
		System.out.println( "total bytes: " +target.size() );
		System.out.println( new String(target.toByteArray()) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
