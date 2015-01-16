package gov.cida.cdat;


import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.io.stream.FileStream;
import gov.cida.cdat.io.stream.PipeStream;
import gov.cida.cdat.io.stream.SimpleStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;


public class TestControlInterrupt {

	public static void main(String[] args) throws Exception {
		SCManager control = SCManager.get();

		// consumer
		ByteArrayOutputStream      target   = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> consumer = new SimpleStream<OutputStream>(target);
		
		// producer
		File file = new File("lib/akka/scalatest_2.11-2.1.3.jar");
		FileStream producer = new FileStream(file);
		
		// pipe
		final PipeStream pipe = new PipeStream(producer, consumer);		
		
		String serviceName = control.addService("google", pipe);
		
		control.send(serviceName, Message.create("Message", "Test"));
		control.send(serviceName, Message.create(Control.Start));
		Thread.sleep(8);
		control.send(serviceName, Message.create(Control.Stop));
		control.shutdown();
		
		System.out.println("pipe results: loaded " +target.size()+ " of a total 6920622 before interrupt");
	}
}
