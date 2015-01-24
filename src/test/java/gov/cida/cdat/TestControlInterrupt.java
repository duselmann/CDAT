package gov.cida.cdat;


import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.FileStream;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;


public class TestControlInterrupt {

	public static void main(String[] args) throws Exception {
		SCManager manager = SCManager.instance();

		// consumer
		ByteArrayOutputStream      target   = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> consumer = new SimpleStream<OutputStream>(target);
		
		// producer
		File file = new File("lib/akka/scalatest_2.11-2.1.3.jar");
		FileStream producer = new FileStream(file);
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);		
		
<<<<<<< HEAD
		String workerName = manager.addWorker("google", pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		manager.send(workerName, Message.create(Control.Start));
		Thread.sleep(8);
		manager.send(workerName, Message.create(Control.Stop));
		manager.shutdown();
=======
		String workerName = control.addWorker("google", pipe);
		
		control.send(workerName, Message.create("Message", "Test"));
		control.send(workerName, Message.create(Control.Start));
		Thread.sleep(8);
		control.send(workerName, Message.create(Control.Stop));
		control.shutdown();
>>>>>>> 36bc3ee7287e36de047d009aa3525c808514e464
		
		System.out.println("pipe results: loaded " +target.size()+ " of a total 6920622 before interrupt");
	}
}
