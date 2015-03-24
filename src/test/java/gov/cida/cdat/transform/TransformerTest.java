package gov.cida.cdat.transform;

import static org.junit.Assert.*;

import org.junit.Test;

public class TransformerTest {
	
	@Test
	public void testMatchBytes() {
		assertTrue( Transformer.matchBytes("\n".getBytes(), "\n".getBytes(), 0) );
		assertTrue( Transformer.matchBytes("\r\n".getBytes(), "\r\n".getBytes(), 0) );
		assertTrue( Transformer.matchBytes("\n".getBytes(), "a\n".getBytes(), 1) );
		assertTrue( Transformer.matchBytes("\r\n".getBytes(), "a\r\n".getBytes(), 1) );

		assertFalse( Transformer.matchBytes("\n".getBytes(), "a".getBytes(), 0) );
		assertFalse( Transformer.matchBytes("asdf".getBytes(), "a".getBytes(), 0) );
		assertFalse( Transformer.matchBytes("\n".getBytes(), "\na".getBytes(), 1) );
		assertFalse( Transformer.matchBytes("\n".getBytes(), "\na\n".getBytes(), 1) );
	}
	
	@Test
	public void testMatchBytes_bounds() {
		assertFalse( Transformer.matchBytes(null, "a".getBytes(), 0) );
		assertFalse( Transformer.matchBytes("".getBytes(), "a".getBytes(), 0) );
		assertFalse( Transformer.matchBytes("a".getBytes(), null, 0) );
		assertFalse( Transformer.matchBytes("a".getBytes(), "a".getBytes(), 1) );
		assertFalse( Transformer.matchBytes("a".getBytes(), "aasda".getBytes(), 5) );
		assertFalse( Transformer.matchBytes("asdf".getBytes(), "asdfasdf".getBytes(), 5) );
	}

}
