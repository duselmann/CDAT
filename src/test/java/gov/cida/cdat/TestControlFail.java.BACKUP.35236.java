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


public class TestControlFail {

	private static ByteArrayOutputStream target;
	private static String workerName;
<<<<<<< HEAD
	private static SCManager manager;
=======
	private static SCManager controller;
>>>>>>> 36bc3ee7287e36de047d009aa3525c808514e464
	
	
	public static void main(String[] args) throws Exception {
		manager = SCManager.instance();

		// consumer
		target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream>     out = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.asdfsdfasdf.com");
		UrlStream in = new UrlStream(url);
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);		
		
<<<<<<< HEAD
		workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Control.Start);
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message response){
	            report(response);		
=======
		workerName = controller.addWorker("google", pipe);
		
		controller.send(workerName, Message.create("Message", "Test"));
		controller.send(workerName, Control.Start);
		controller.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message repsonse){
	            report(repsonse);		
>>>>>>> 36bc3ee7287e36de047d009aa3525c808514e464
	        }
	    });
	}
	
	
	private static void report(final Message response) {
        System.out.println("onComplete Response is " + response);
		
<<<<<<< HEAD
		manager.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message response) {
=======
		controller.send(workerName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message repsonse) throws Throwable {
>>>>>>> 36bc3ee7287e36de047d009aa3525c808514e464
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
