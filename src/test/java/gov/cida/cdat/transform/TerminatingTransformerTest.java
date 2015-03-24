package gov.cida.cdat.transform;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;

import org.junit.Test;

public class TerminatingTransformerTest {

	@Test
	public void testMatchBytes() {
		assertTrue( TerminatingTransformer.matchBytes("\n".getBytes(), "\n".getBytes(), 0) );
		assertTrue( TerminatingTransformer.matchBytes("\r\n".getBytes(), "\r\n".getBytes(), 0) );
		assertTrue( TerminatingTransformer.matchBytes("\n".getBytes(), "a\n".getBytes(), 1) );
		assertTrue( TerminatingTransformer.matchBytes("\r\n".getBytes(), "a\r\n".getBytes(), 1) );

		assertFalse( TerminatingTransformer.matchBytes("\n".getBytes(), "a".getBytes(), 0) );
		assertFalse( TerminatingTransformer.matchBytes("asdf".getBytes(), "a".getBytes(), 0) );
		assertFalse( TerminatingTransformer.matchBytes("\n".getBytes(), "\na".getBytes(), 1) );
		assertFalse( TerminatingTransformer.matchBytes("\n".getBytes(), "\na\n".getBytes(), 1) );
	}
	
	@Test
	public void testMatchBytes_bounds() {
		assertFalse( TerminatingTransformer.matchBytes(null, "a".getBytes(), 0) );
		assertFalse( TerminatingTransformer.matchBytes("".getBytes(), "a".getBytes(), 0) );
		assertFalse( TerminatingTransformer.matchBytes("a".getBytes(), null, 0) );
		assertFalse( TerminatingTransformer.matchBytes("a".getBytes(), "a".getBytes(), 1) );
		assertFalse( TerminatingTransformer.matchBytes("a".getBytes(), "aasda".getBytes(), 5) );
		assertFalse( TerminatingTransformer.matchBytes("asdf".getBytes(), "asdfasdf".getBytes(), 5) );
	}
	
	
	@Test
	public void testCheckForTerminator_stillTransforming() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		trans.checkForTerminator("asdf".getBytes(), 0, 4);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
	}
	
	@Test
	public void testCheckForTerminator_bypass() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		TestUtils.refectSetValue(trans, "transforming", false);
		assertFalse( (boolean) TestUtils.reflectValue(trans, "transforming") );

		trans.checkForTerminator("asdf".getBytes(), 0, 4);
	}

	@Test
	public void testCheckForTerminator_byteArrayIsTerminator() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		trans.checkForTerminator("\n".getBytes(), 0, 1);
		
		assertFalse( (boolean) TestUtils.reflectValue(trans, "transforming") );

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}
	
	@Test
	public void testCheckForTerminator_byteArrayInTerminator() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		trans.checkForTerminator("a\na".getBytes(), 0, 3);
		
		assertFalse( (boolean) TestUtils.reflectValue(trans, "transforming") );

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}

	@Test
	public void testCheckForTerminator_byteArrayEndTerminator() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		trans.checkForTerminator("asdf\n".getBytes(), 0, 5);
		
		assertFalse( (boolean) TestUtils.reflectValue(trans, "transforming") );

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}

	@Test
	public void testCheckForTerminator_byteArrayEndTerminator_outOfLengthRange() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		trans.checkForTerminator("asdf\n".getBytes(), 0, 3);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}
	
	
	@Test
	public void testCheckForTerminator_byteArrayShorterThenTerminatorCached() {
		TerminatingTransformer trans = new TerminatingTransformer("\r\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] in = "\t".getBytes();
		trans.checkForTerminator(in, 0, 1);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertNotNull(cache);
		assertEquals(1, cache.length);
		assertFalse( in == cache );
		assertEquals(in[0], cache[0]);
	}
	
/*	
	@Test
	public void testCheckForTerminator_cacheAccumulation() {
		TerminatingTransformer trans = new TerminatingTransformer("\r\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] in1 = "\r".getBytes();
		trans.checkForTerminator(in1, 0, 1);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] cache1 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertNotNull(cache1);
		assertEquals(1, cache1.length);
		assertFalse( in1 == cache1 );
		assertEquals(in1[0], cache1[0]);

		byte[] in2 = "\n".getBytes();
		trans.checkForTerminator(in2, 0, 1);
		
		assertFalse( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] cache2 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache2);
	}
	*/
}
