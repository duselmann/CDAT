package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStreamContainer;
import gov.cida.cdat.io.stream.StreamContainer;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Worker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;


public class SCManagerControlInterruptTest {

	/**
	 * This sets up an infinite stream that can be interrupted. We pass if it stops.
	 * I tried to make a finite stream but the copy loop was too fast to reliably
	 * stop at a predictable byte count. This is better anyhow as it simulates a
	 * very large data source.
	 * @throws Exception
	 */
	@Test
	public void testInterruptedStream() throws Exception {
		SCManager manager = SCManager.instance();

		final boolean[] closeCalled = new boolean[2];
		// consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10) {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
				super.close();
			}
		};
		SimpleStreamContainer<OutputStream> consumer = new SimpleStreamContainer<OutputStream>(target);
		
		// producer
		StreamContainer<InputStream> producer = new StreamContainer<InputStream>(){
			int readCount=1;
			InputStream  source = new InputStream() {
				
				@Override
				public int read() throws IOException {
					throw new RuntimeException("Should not be called");
				}
				@Override
				public synchronized int read(byte[] b, int off, int len) throws IOException {
					TestUtils.log("read(byte[],off,len) called", off, len);
 					return super.read(b, off, len);
				}
				@Override
				public synchronized int read(byte[] b) {
					TestUtils.log("test read(byte[]) called - to read a small byte count");
					String readString = "part" + readCount++;
			        System.arraycopy(readString.getBytes(), 0, b, 0, readString.length());

					return readString.length();
				}
				@Override
				public void close() throws IOException {
					closeCalled[1] = true;
					super.close();
				}
			};
			@Override
			protected String getName() {
				return "TestingProducerContainer";
			}
			@Override
			public InputStream init() throws StreamInitException {
				return source;
			}
		};
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		Worker worker       = new PipeWorker(pipe){
			@Override
			public boolean process() throws CdatException {
				TestUtils.log("test process() called - to set a small read time window");
				boolean isMore = pipe.process(1);
				return isMore;
			}
		};
		
		String workerName = manager.addWorker("Interrupted", worker);
		
		manager.send(workerName, Message.create(Control.Start));
		manager.send(workerName, Message.create(Control.Stop));
		manager.shutdown();
		
		Thread.sleep(100);
		String results =  new String(target.toByteArray());
		TestUtils.log("Loaded Stream: ", results);
		TestUtils.log("pipe results: loaded ", target.size());
		
		Assert.assertTrue("Expect to be more than zero.", results.length()>0);
		Assert.assertTrue("Expect the consumer to be closed.", closeCalled[0]);
		Assert.assertTrue("Expect the producer to be closed.", closeCalled[1]);
	}
}
