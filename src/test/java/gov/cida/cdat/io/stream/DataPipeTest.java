package gov.cida.cdat.io.stream;

import gov.cida.cdat.TestUtils;
import gov.cida.cdat.exception.CdatException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
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
		
		final Object[] done = new Object[1]; 
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
					pipe.processAll();
					done[0] = pipe;
				} catch (CdatException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		System.out.println("main waithing for pipe...");
		TestUtils.waitAlittleWhileForResponse(done);
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
		
		Assert.assertEquals("Expect to recieve "+dataRef.length, dataRef.length, target.size());
		Assert.assertTrue("Expect to find the 'middle'", results.contains("middle"));
		Assert.assertTrue("Expect to find the 'end'", results.contains("end"));
//		Assert.assertTrue("Expect close to be called on input", closeCalled[1]);
		Assert.assertTrue("Expect close to be called on output", closeCalled[0]);
		
	}

}
