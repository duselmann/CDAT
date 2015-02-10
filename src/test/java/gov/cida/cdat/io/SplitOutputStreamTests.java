package gov.cida.cdat.io;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.junit.Test;

public class SplitOutputStreamTests {

	@Test
	public void testSplit_closeCalledOnEach() throws Exception {
		final Boolean[] closeCalled = new Boolean[3];

		ByteArrayOutputStream out0 = new ByteArrayOutputStream(1024){
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
			}
		};
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024){
			@Override
			public void close() throws IOException {
				closeCalled[1] = true;
			}
		};
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024){
			@Override
			public void close() throws IOException {
				closeCalled[2] = true;
			}
		};
		
		SplitOutputStream split = new SplitOutputStream(out0,out1,out2);

		String testBytes = "TestBytes";
		split.write( testBytes.getBytes() );
		split.flush();
		split.close();
		
		assertTrue("Close should be called", closeCalled[0]);
		assertTrue("Close should be called", closeCalled[1]);
		assertTrue("Close should be called", closeCalled[2]);
		
		assertFalse("There should be no errors on splitstream", split.hasErrors());
	}
	
	@Test
	public void testSplit_flushCalledOnEach() throws Exception {
		final Boolean[] flushCalled = new Boolean[3];

		ByteArrayOutputStream out0 = new ByteArrayOutputStream(1024){
			@Override
			public void flush() throws IOException {
				flushCalled[0] = true;
			}
		};
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024){
			@Override
			public void flush() throws IOException {
				flushCalled[1] = true;
			}
		};
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024){
			@Override
			public void flush() throws IOException {
				flushCalled[2] = true;
			}
		};
		
		SplitOutputStream split = new SplitOutputStream(out0,out1,out2);

		String testBytes = "TestBytes";
		split.write( testBytes.getBytes() );
		split.flush();
		split.close();
		
		assertEquals("flush should be called", true, flushCalled[0]);
		assertEquals("flush should be called", true, flushCalled[1]);
		assertEquals("flush should be called", true, flushCalled[2]);
	}
	
	@Test
	public void testSplit_writeByteArray() throws Exception {
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(1024);
		
		SplitOutputStream split = new SplitOutputStream(out1,out2,out3);

		String testBytes = "TestBytes";
		split.write( testBytes.getBytes() );
		split.flush();
		split.close();
		
		assertEquals("Out should recieve split write", testBytes, new String(out1.toByteArray()));
		assertEquals("Out should recieve split write", testBytes, new String(out2.toByteArray()));
		assertEquals("Out should recieve split write", testBytes, new String(out3.toByteArray()));
	}
	
	@Test
	public void testSplit_writeByteArray_offsetAndLength() throws Exception {
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(1024);
		
		SplitOutputStream split = new SplitOutputStream(out1,out2,out3);

		String testBytes = "TestBytes";
		// offset is zero based like arrays
		split.write( testBytes.getBytes(), 4, 4 );
		split.flush();
		split.close();
		
		String expect = "Byte";
		assertEquals("Out should recieve split write", expect, new String(out1.toByteArray()));
		assertEquals("Out should recieve split write", expect, new String(out2.toByteArray()));
		assertEquals("Out should recieve split write", expect, new String(out3.toByteArray()));
	}

	@Test
	public void testSplit_writeByte() throws Exception {
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(1024);
		
		SplitOutputStream split = new SplitOutputStream(out1,out2,out3);

		byte b = 65;
		split.write(b);
		split.flush();
		split.close();
		
		assertEquals("Out should recieve split write", b, out1.toByteArray()[0]);
		assertEquals("Out should recieve split write", b, out2.toByteArray()[0]);
		assertEquals("Out should recieve split write", b, out3.toByteArray()[0]);
		
		assertEquals("Out should recieve only one byte", 1, out1.size());
		assertEquals("Out should recieve only one byte", 1, out2.size());
		assertEquals("Out should recieve only one byte", 1, out3.size());
	}

	@Test
	public void testSplit_exceptionOnOneDoesNotEffectTheOthers() throws Exception {
		final Boolean[] closeCalled = new Boolean[3];
		final int[] writeCount = new int[3];

		final String errorMessage   = "test error";
		
		ByteArrayOutputStream out0 = new ByteArrayOutputStream(1024){
			@Override
			public void write(byte[] bytes) throws IOException {
				writeCount[0]++;
				throw new IOException(errorMessage);
			}
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
			}
		};
		ByteArrayOutputStream out1 = new ByteArrayOutputStream(1024){
			@Override
			public void write(byte[] bytes) throws IOException {
				writeCount[1]++;
				super.write(bytes);
			}
			@Override
			public void close() throws IOException {
				closeCalled[1] = true;
			}
		};
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(1024){
			@Override
			public void write(byte[] bytes) throws IOException {
				writeCount[2]++;
				super.write(bytes);
			}
			@Override
			public void close() throws IOException {
				closeCalled[2] = true;
			}
		};
		
		SplitOutputStream split = new SplitOutputStream(out0,out1,out2);

		String testBytes = "TestBytes";
		split.write( "Test".getBytes() );
		split.write( "Bytes".getBytes() );
		split.flush();
		split.close();
		
		assertTrue("Close should be called", closeCalled[0]);
		assertTrue("Close should be called", closeCalled[1]);
		assertTrue("Close should be called", closeCalled[2]);
		
		assertFalse("Out should recieve split write", testBytes.equals( new String(out0.toByteArray()) ) );
		assertEquals("Out should recieve split write", testBytes, new String(out1.toByteArray()));
		assertEquals("Out should recieve split write", testBytes, new String(out2.toByteArray()));
		
		assertEquals("Out should write once because it errored once",  1, writeCount[0]);
		assertEquals("Out should write twice", 2, writeCount[1]);
		assertEquals("Out should write twice", 2, writeCount[2]);
		
		assertTrue("There should be errors on splitstream", split.hasErrors());
		assertEquals("There should be 1 errors on splitstream", 1, split.errorCount());
		
		Map<OutputStream,Exception> errors = split.getErrors();
		assertFalse("There should be errors on errors collection", errors.isEmpty());
		assertEquals("There should be 1 errors on errors collection", 1, errors.size());
		
		assertTrue("Expect the source of the error in keys", errors.containsKey(out0));
		assertEquals("Expect the exception assigned to the source of the error",errorMessage, errors.get(out0).getMessage());
		
		errors.clear();
		assertTrue("There should be errors on splitstream (protective copy)", split.hasErrors());
		assertEquals("There should be 1 errors on splitstream (protective copy)", 1, split.errorCount());		
	}
}
