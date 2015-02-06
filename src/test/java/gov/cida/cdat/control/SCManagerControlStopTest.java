package gov.cida.cdat.control;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStreamContainer;
import gov.cida.cdat.io.stream.StreamContainer;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Worker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;


public class SCManagerControlStopTest {

	private static ByteArrayOutputStream target;
	private static SCManager manager;
	private static byte[] dataRef;

	@Test
	public void testThatStopTerminatesWorkers() throws Exception {
		manager = SCManager.instance();
		final Boolean[] closeCalled = new Boolean[2];

		// consumer
		target = new ByteArrayOutputStream(1024*10) {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
				super.close();
			}
		};
		SimpleStreamContainer<OutputStream> out  = new SimpleStreamContainer<OutputStream>(target);
		
		// producer
		dataRef = TestUtils.sampleData();
		StreamContainer<InputStream> in = new StreamContainer<InputStream>() {
			InputStream dataRefStream = new ByteArrayInputStream(dataRef);
			InputStream  source = new InputStream() {
				
				@Override
				public int read() throws IOException {
					throw new RuntimeException("Should not be called");
				}
				@Override
				public synchronized int read(byte[] b, int off, int len) throws IOException {
					TestUtils.log("read(byte[],off,len) called", off, len);
 					return dataRefStream.read(b, off, len);
				}
				@Override
				public synchronized int read(byte[] b) throws IOException {
					TestUtils.log("test read(byte[]) called - to read a small byte count");
 					return dataRefStream.read(b, 0, 5);
				}
				@Override
				public void close() throws IOException {
					closeCalled[1] = true;
					super.close();
					Closer.close(dataRefStream);
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
		final DataPipe pipe = new DataPipe(in, out);
		Worker worker       = new PipeWorker(pipe){
			@Override
			public boolean process() throws CdatException {
				TestUtils.log("test process() called - to set a small read time window");
				boolean isMore = pipe.process(1);
				return isMore;
			}
		};
		
		String workerName = manager.addWorker("producer", worker);
		
		manager.send(workerName, Message.create(Control.Start));
		manager.send(workerName, Message.create(Control.Stop));
//		manager.shutdown(); // TODO in order to test this we need tests to run for the wait period
		
		TestUtils.waitAlittleWhileForResponse(closeCalled);
		TestUtils.log("pipe results: expect short length and no 'middle' found. bytes:", target.size() );
		
		String results =  new String(target.toByteArray());
		String msg = "'middle' Not Found";
		if (results.contains("middle") ) {
			msg = "'middle' Found";
		}

		TestUtils.log(msg);
		
		Assert.assertTrue("Expect to recieve less than 100 bytes", target.size()<100);
		Assert.assertFalse("Expect NOT to find the 'middle'", results.contains("middle"));
		Assert.assertFalse("Expect NOT to find the 'end'", results.contains("end"));
		Assert.assertTrue("Expect close to be called on input", closeCalled[1]);
		Assert.assertTrue("Expect close to be called on output", closeCalled[0]);
	}
	
	

}
