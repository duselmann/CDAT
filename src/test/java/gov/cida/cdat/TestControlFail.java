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
		URL url = new URL("http://www.asdfsdfasdf.com");
		UrlStreamContainer in = new UrlStreamContainer(url);
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);
		
		workerName = manager.addWorker("google", pipe);
		
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
		
		System.out.println();
		System.out.println();
	}
}
