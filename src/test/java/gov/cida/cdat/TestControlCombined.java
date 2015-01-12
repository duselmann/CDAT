package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Controller;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.io.stream.PipeStream;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.io.stream.UrlStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;


public class TestControlCombined {

	public static void main(String[] args) throws Exception {
		final Controller control = Controller.get();

		// consumer
		final ByteArrayOutputStream target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream>     out = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// pipe
		final PipeStream pipe = new PipeStream(google, out);		
		
		final String serviceName = control.addService("google", pipe);
		
		control.send(serviceName, Message.create("Message", "Test"));
		control.send(serviceName, Control.Start);
		control.send(serviceName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message repsonse){
	            System.out.println("onComplete Response is " + repsonse);
	            report(control, serviceName, target);		
	        }
	    });
	}
	
	
	private static void report(final Controller control,final String serviceName,
			final ByteArrayOutputStream target) {
		
		control.send(serviceName, Control.Stop, new Callback() {
			public void onComplete(Throwable t, Message repsonse) throws Throwable {
				System.out.println("service shutdown");
				control.shutdown();
			}
		});
		System.out.println("pipe results");
		System.out.println( "total bytes: " +target.size() );
		System.out.println( new String(target.toByteArray(), 0, 100) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
