package gov.cida.cdat.io.container;

import gov.cida.cdat.TestUtils;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.io.container.StreamContainer;
import gov.cida.cdat.transform.RegexTransformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

public class TransformerTest {

	@Test
	public void transformPipedStream() throws Exception {
		
		System.out.println("pipe build");
		
		// Consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);

		// Transformer
		RegexTransformer transform = new RegexTransformer("middle","center");
		TransformOutputStream<Object> tout = new TransformOutputStream<Object>(target, transform);
		SimpleStreamContainer<OutputStream> consumer  = new SimpleStreamContainer<OutputStream>(tout);

		// Producer
		final byte[] dataRef = TestUtils.sampleData();
		ByteArrayInputStream stream = new ByteArrayInputStream(dataRef);
		StreamContainer<InputStream> producer = new SimpleStreamContainer<InputStream>(stream);
		
		// Pipe producer to consumer
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
		System.out.println("pipe close");
		pipe.close();
		
		System.out.println("pipe results: expect all div tags converted to span");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String results = new String(target.toByteArray());
		String msg = "as desired, middle not found, ";
		if ( results.contains("center") ) {
			msg += "center ";
		}
		if ( results.contains("middle") ) {
			msg = "oops! middle ";
		}
		
		TestUtils.log(msg, "found");
		
		Assert.assertTrue("'middle' should have been replaced with 'center'", results.contains("center"));
		Assert.assertFalse("'middle' should have been replaced with 'center'", results.contains("middle"));
	}

	
	@Test
	public void chainedTransformPipedStream() throws Exception {
		
		System.out.println("pipe build");
		
		// Consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);

		// Transformer 1
		RegexTransformer transform1 = new RegexTransformer("middle","center");
		TransformOutputStream<Object> tStream1 = new TransformOutputStream<Object>(target, transform1);
		//SimpleStreamContainer<OutputStream> tContainer  = new SimpleStreamContainer<OutputStream>(tStream1);

		// Transformer 2
		RegexTransformer transform2 = new RegexTransformer("Z","~");
		TransformOutputStream<Object> tStream2 = new TransformOutputStream<Object>(tStream1, transform2);
		SimpleStreamContainer<OutputStream> consumer  = new SimpleStreamContainer<OutputStream>(tStream2);

		// Producer
		final byte[] dataRef = TestUtils.sampleData();
		ByteArrayInputStream stream = new ByteArrayInputStream(dataRef);
		StreamContainer<InputStream> producer = new SimpleStreamContainer<InputStream>(stream);
		
		// Pipe producer to consumer
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
		System.out.println("pipe close");
		pipe.close();
		
		System.out.println("pipe results: expect all div tags converted to span");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String results = new String(target.toByteArray());
		String msg = "as desired, middle not found, ";
		if ( results.contains("center") ) {
			msg += "center";
		}
		if ( results.contains("middle") ) {
			msg = "oops! middle";
		}
		
		TestUtils.log(msg, "found");
		
		Assert.assertTrue("'middle' should have been replaced with 'center'", results.contains("center"));
		Assert.assertFalse("'middle' should have been replaced with 'center'", results.contains("middle"));
		Assert.assertTrue("'Z' should have been replaced with '~'", results.contains("~"));
		Assert.assertFalse("'Z' should have been replaced with '~'", results.contains("Z"));
	}
}
