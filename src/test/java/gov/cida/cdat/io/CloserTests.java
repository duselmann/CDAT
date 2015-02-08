package gov.cida.cdat.io;

import static org.junit.Assert.*;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;

public class CloserTests {

	@Test
	public void testClose_Closeable() {
		final Boolean[] closeCalled = new Boolean[1];
		
		Closeable closeIt = new Closeable() {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
			}
		};		
		
		Closer.close(closeIt);
		
		assertEquals("Close should be called", true, closeCalled[0]);
	}
	@Test
	public void testClose_AutoCloseable() {
		final Boolean[] closeCalled = new Boolean[1];
		
		AutoCloseable closeIt = new AutoCloseable() {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
			}
		};		
		
		Closer.close(closeIt);
		
		assertEquals("Close should be called", true, closeCalled[0]);
	}

	@Test
	public void testClose_Closeable_silent() {
		final Boolean[] closeCalled = new Boolean[1];
		
		Closeable closeIt = new Closeable() {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
				throw new IOException();
			}
		};		
		try {
			Closer.close(closeIt);
		} catch (Exception e) {
			fail("Close should be called silently");
		}
		assertEquals("Close should be called", true, closeCalled[0]);
	}
	@Test
	public void testClose_AutoCloseable_silent() {
		final Boolean[] closeCalled = new Boolean[1];
		
		AutoCloseable closeIt = new AutoCloseable() {
			@Override
			public void close() throws IOException {
				closeCalled[0] = true;
				throw new IOException();
			}
		};		
		
		try {
			Closer.close(closeIt);
		} catch (Exception e) {
			fail("Close should be called silently");
		}
		
		assertEquals("Close should be called", true, closeCalled[0]);
	}

}
