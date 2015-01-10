package gov.cida.cdat;


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
		Controller control = Controller.get();

		// consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out  = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// pipe
		final PipeStream pipe = new PipeStream(google, out);		
		
		String serviceName = control.addService("google", pipe);
		
		control.sendControl(serviceName, Message.create("Message", "Test"));
		control.sendControl(serviceName, Message.create(Control.Start));

		Thread.sleep(1000);
		control.sendControl(serviceName, Message.create(Control.Stop));
		
		Thread.sleep(1000);
		control.shutdown();
		
		System.out.println("pipe results");
		System.out.println( target.size() );
		System.out.println( new String(target.toByteArray(), 0, 100) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);		
	}
	
	

}
