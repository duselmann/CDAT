package gov.cida.cdat;


import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.io.stream.UrlStream;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;


public class TestControlStop {

	public static void main(String[] args) throws Exception {
		SCManager manager = SCManager.instance();

		// consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out  = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// pipe
		final DataPipe pipe = new DataPipe(google, out);		
		
		String workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Message.create(Control.Start));
//		Thread.sleep(500);
		manager.send(workerName, Message.create(Control.Stop));
		manager.shutdown();
		
		System.out.println();
		System.out.println("pipe results: expect zero length and no Google found");
		System.out.println( target.size() );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
		System.out.println();
		System.out.println();
		
	}
	
	

}
