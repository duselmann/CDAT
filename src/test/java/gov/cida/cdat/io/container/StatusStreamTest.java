package gov.cida.cdat.io.container;

import gov.cida.cdat.TestUtils;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.StatusOutputStream;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.io.container.StatusStreamContainer;
import gov.cida.cdat.io.container.StreamContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

public class StatusStreamTest {

	
	@Test
	public void testStatusPipedStream() throws Exception {
		
		System.out.println("pipe build");
		final Boolean[] closeCalled = new Boolean[2];
		
		// consumer
		ByteArrayOutputStream target = new ByteArrayOutputStream(1024*10) {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
				super.close();
			}
		};
		SimpleStreamContainer<OutputStream> consumer = new SimpleStreamContainer<OutputStream>(target);

		// status chained stream
		final StatusStreamContainer status = new StatusStreamContainer(consumer);
		
		// chained container class example - it works too but the line above is more concise
//		final ChainedStream<StatusOutputStream> status =
//			new ChainedStream<StatusOutputStream>(consumer) {
//				@Override
//				protected StatusOutputStream chain(OutputStream stream) {
//					return new StatusOutputStream(stream);
//				}
//			};
		
		// producer
		final byte[] dataRef = TestUtils.sampleData();
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
		final DataPipe pipe = new DataPipe(in, status);
		
		status(status.getChainedStream());
		
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
					status(status.getChainedStream());
					pipe.processAll();
					status(status.getChainedStream());
				} catch (CdatException e) {
					e.printStackTrace();
				}
			}

		}.start();
		new Thread() {
			@Override
			public void run() {
				System.out.println("status thread");
				while (status.getChainedStream() == null) {
					status(status.getChainedStream());
					try {Thread.sleep(50); } catch (InterruptedException e) {}
				}
				status(status.getChainedStream());
				try {Thread.sleep(50); } catch (InterruptedException e) {}
				status(status.getChainedStream());
			}

		}.start();
		
		System.out.println("main waithing for pipe...");
		Thread.sleep(1000);
		System.out.println("main closing pipe");
		pipe.close();
		
		System.out.println("pipe results: expect a status of not open initially, then a progress report of Google page loaded");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		

		String results =  new String(target.toByteArray());
		String msg = "'middle' Not Found";
		if (results.contains("middle") ) {
			msg = "'middle' Found";
		}

		TestUtils.log(msg);
		status(status.getChainedStream());
		
		Assert.assertEquals("Expect to recieve "+dataRef.length, dataRef.length, target.size());
		Assert.assertTrue("Expect to find the 'middle'", results.contains("middle"));
		Assert.assertTrue("Expect to find the 'end'", results.contains("end"));
		Assert.assertTrue("Expect close to be called on input", closeCalled[1]);
		Assert.assertTrue("Expect close to be called on output", closeCalled[0]);

		StatusOutputStream statusStream = status.getChainedStream();
		Assert.assertFalse("status should not be open: ", statusStream.isOpen());
		Assert.assertTrue("status should be done: ", statusStream.isDone());
		Assert.assertEquals("status byteCount should be 20014: ", dataRef.length, statusStream.getByteCount());
		Assert.assertTrue("status time since last write should be less than 1sec: ", 1000 > statusStream.getMsSinceLastWrite());
		Assert.assertTrue("status time since last write should be greater than 200ms: ", 200 < statusStream.getMsSinceLastWrite());
		Assert.assertTrue("status is open for less than 1.5sec: ", 1500 > statusStream.getOpenTime());
		Assert.assertTrue("status is open for greater than 200 ms: ", 200 < statusStream.getOpenTime());
		
	}
	public static void status(StatusOutputStream status) {
		if (status == null) {
			System.out.println("  status is not init yet: " + status);
		} else {
			System.out.println();
			System.out.println("  status is open: " + status.isOpen());
			System.out.println("  status is done: " + status.isDone());
			System.out.println("  status is byteCount: " + status.getByteCount());
			System.out.println("  status time since last write: " + status.getMsSinceLastWrite());
			System.out.println("  status is open for: " + status.getOpenTime() +"ms");
		}
	}

}
