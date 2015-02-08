package gov.cida.cdat.io;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class StatusStreamTests {

	@Test
	public void testStatus_writeByte() throws Exception {
		
		ByteArrayOutputStream target = new ByteArrayOutputStream(1024);
		StatusOutputStream status = new StatusOutputStream(target);
		
		status.write(256+65);
		status.close();
		
		assertEquals("should write one byte", 1, target.size());
		assertEquals("should write the lower byte of the given int", 65, target.toByteArray()[0]);
		assertEquals("", 1, status.getByteCount());
	}

	@Test
	public void testStatus_writeByteArray() throws Exception {
		
		ByteArrayOutputStream target = new ByteArrayOutputStream(1024);
		StatusOutputStream status = new StatusOutputStream(target);
		
		status.write(new byte[] {65,66,67});
		status.close();
		
		assertEquals("should write one byte", 3, target.size());
		assertEquals("should write the bytes of the given array", 65, target.toByteArray()[0]);
		assertEquals("should write the bytes of the given array", 66, target.toByteArray()[1]);
		assertEquals("should write the bytes of the given array", 67, target.toByteArray()[2]);
		assertEquals("", 3, status.getByteCount());
	}

}
