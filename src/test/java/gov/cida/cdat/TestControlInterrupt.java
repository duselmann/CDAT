package gov.cida.cdat;


import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.FileStreamContainer;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStreamContainer;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;


public class TestControlInterrupt {

	public static void main(String[] args) throws Exception {
		SCManager manager = SCManager.instance();

		// consumer
		ByteArrayOutputStream      target   = new ByteArrayOutputStream(1024*10);
		SimpleStreamContainer<OutputStream> consumer = new SimpleStreamContainer<OutputStream>(target);
		
		// producer
		File file = new File("lib/akka/scalatest_2.11-2.1.3.jar");
		
		if ( ! file.exists() ) {
			System.out.println();
			System.out.println("This test requires a large multi MB file.");
			System.out.println(file.getAbsolutePath() + " was not found.");
			System.exit(1);
		}
		
		FileStreamContainer producer = new FileStreamContainer(file);
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		
		String workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Message.create(Control.Start));
		Thread.sleep(8);
		manager.send(workerName, Message.create(Control.Stop));
		manager.shutdown();
		
		System.out.println("pipe results: loaded " +target.size()+ " of a total 6920622 before interrupt");
	}
}
