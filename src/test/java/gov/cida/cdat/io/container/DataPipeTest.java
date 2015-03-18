package gov.cida.cdat.io.container;

import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.io.container.StreamContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.*;

import org.junit.Test;

public class DataPipeTest {

	
	@Test
	public void pipedStream() throws Exception {
		
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

		// producer
		final byte[] dataRef = TestUtils.sampleData();
		ByteArrayInputStream stream = new ByteArrayInputStream(dataRef);
		StreamContainer<InputStream> producer = new SimpleStreamContainer<InputStream>(stream);
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		
		final Object[] completed = new Object[1]; 
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
					pipe.processAll();
					completed[0] = pipe;
				} catch (CdatException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		System.out.println("main waithing for pipe...");
		Time.waitForResponse(completed,100);

		System.out.println("main closing pipe");
		pipe.close();
		
		System.out.println("pipe results: expect Google page loaded on separate thead, checking also that all levels are closed and flush called if found.");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String results =  new String(target.toByteArray());
		String msg = "'middle' Not Found";
		if (results.contains("middle") ) {
			msg = "'middle' Found";
		}

		TestUtils.log(msg);
		
		assertEquals("Expect to recieve "+dataRef.length, dataRef.length, target.size());
		assertTrue("Expect to find the 'middle'", results.contains("middle"));
		assertTrue("Expect to find the 'end'", results.contains("end"));
//		Assert.assertTrue("Expect close to be called on input", closeCalled[1]);
		assertTrue("Expect close to be called on output", closeCalled[0]);
		
	}
	
	@Test
	public void pipedStream_producerOpenException() throws Exception {
		
		System.out.println("pipe build");
		final Boolean[] cleanupCalled = new Boolean[]{false,false};
		final Boolean[] initCalled  = new Boolean[]{false,false};
		
		// consumer
		StreamContainer<OutputStream> consumer = new StreamContainer<OutputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[0] = true;
			}
			@Override
			public OutputStream init() throws StreamInitException {
				initCalled[0] = true;
				return null;
			}
		};

		// producer
		StreamContainer<InputStream> producer = new StreamContainer<InputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[1] = true;
			}
			@Override
			public InputStream init() throws StreamInitException {
				initCalled[1] = true;
				throw new StreamInitException("test init exception");
			}
		};
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		try {
			pipe.open();
			fail("we expect a StreamInitException");
		} catch (StreamInitException e) {
			// this is what we are testing
		}
		assertTrue("When the producer (opened first in a DataPipe) throws an exception then open/init should be called on the producer because it was opened",
				initCalled[1]);
		assertTrue("When the producer (opened first in a DataPipe) throws an exception then cleanup should be called in order to allow for release of resources",
				cleanupCalled[1]);
		assertFalse("When the producer (opened first in a DataPipe) throws an exception then open/init should NOT be called on the consumer because it was never opened",
				initCalled[0]);
		assertFalse("When the producer (opened first in a DataPipe) throws an exception then cleanup should NOT be called on the consumer because it was never opened",
				cleanupCalled[0]);
		
		pipe.close();
	}

	@Test
	public void pipedStream_consumerOpenException() throws Exception {
		
		System.out.println("pipe build");
		final Boolean[] cleanupCalled = new Boolean[]{false,false};
		final Boolean[] initCalled  = new Boolean[]{false,false};
		
		// consumer
		StreamContainer<OutputStream> consumer = new StreamContainer<OutputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[0] = true;
			}
			@Override
			public OutputStream init() throws StreamInitException {
				initCalled[0] = true;
				throw new StreamInitException("test init exception");
			}
		};

		// producer
		StreamContainer<InputStream> producer = new StreamContainer<InputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[1] = true;
			}
			@Override
			public InputStream init() throws StreamInitException {
				initCalled[1] = true;
				return null;
			}
		};
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		try {
			pipe.open();
			fail("we expect a StreamInitException");
		} catch (StreamInitException e) {
			// this is what we are testing
		}
		assertTrue("When the consumer (opened second in a DataPipe) throws an exception then open/init should be called on the producer because it was opened",
				initCalled[1]);
		assertTrue("When the consumer (opened second in a DataPipe) throws an exception then cleanup should be called in order to allow for release of resources",
				cleanupCalled[1]);
		assertTrue("When the consumer (opened second in a DataPipe) throws an exception then open/init should be called on the consumer because it was never opened",
				initCalled[0]);
		assertTrue("When the consumer (opened second in a DataPipe) throws an exception then cleanup should be called in order to allow for release of resources",
				cleanupCalled[0]);
		
		pipe.close();
	}
	
	@Test
	public void pipedStream_consumerCloseException() throws Exception {
		
		System.out.println("pipe build");
		final Boolean[] cleanupCalled = new Boolean[]{false,false};
		
		// consumer
		StreamContainer<OutputStream> consumer = new StreamContainer<OutputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[0] = true;
				throw new RuntimeException("test cleanup exception");
			}
			@Override
			public OutputStream init() throws StreamInitException {
				return null;
			}
		};

		// producer
		StreamContainer<InputStream> producer = new StreamContainer<InputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[1] = true;
			}
			@Override
			public InputStream init() throws StreamInitException {
				return null;
			}
		};
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		try {
			pipe.open();
		} catch (StreamInitException e) {
			fail("we expect a open to be successful");
		}
		try {
			pipe.close();
		} catch (Exception e) {
			fail("we expect close to be quiet");
		}
		assertTrue("When the consumer throws an exception on cleanup then the producer should also have cleanup called",
				cleanupCalled[1]);
		assertTrue("We expect the consumer cleanup called on close",
				cleanupCalled[0]);
	}

	@Test
	public void pipedStream_producerCloseException() throws Exception {
		
		System.out.println("pipe build");
		final Boolean[] cleanupCalled = new Boolean[]{false,false};
		
		// consumer
		StreamContainer<OutputStream> consumer = new StreamContainer<OutputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[0] = true;
			}
			@Override
			public OutputStream init() throws StreamInitException {
				return null;
			}
		};

		// producer
		StreamContainer<InputStream> producer = new StreamContainer<InputStream>(){
			@Override
			public String getName() {
				return "test stream container";
			}
			@Override
			protected void cleanup() {
				cleanupCalled[1] = true;
				throw new RuntimeException("test cleanup exception");
			}
			@Override
			public InputStream init() throws StreamInitException {
				return null;
			}
		};
		
		// pipe
		final DataPipe pipe = new DataPipe(producer, consumer);
		try {
			pipe.open();
		} catch (StreamInitException e) {
			fail("we expect a open to be successful");
		}
		try {
			pipe.close();
		} catch (Exception e) {
			fail("we expect close to be quiet");
		}
		assertTrue("We expect the producer cleanup called on close",
				cleanupCalled[1]);
		assertTrue("When the producer throws an exception on cleanup then the consumer should also have cleanup called",
				cleanupCalled[0]);
	}
	
}
