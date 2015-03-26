package gov.cida.cdat.transform;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;

import org.junit.Test;

public class TerminatingTransformerTest {

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
		
		int len = trans.checkForTerminator("\n".getBytes(), 0, 1);
		
		assertEquals(0,len);

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}
	
	@Test
	public void testCheckForTerminator_byteArrayInTerminator() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		int len = trans.checkForTerminator("a\na".getBytes(), 0, 3);
		
		assertEquals(1,len);

		byte[] cache = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache);
	}

	@Test
	public void testCheckForTerminator_byteArrayEndTerminator() {
		TerminatingTransformer trans = new TerminatingTransformer("\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		int len = trans.checkForTerminator("asdf\n".getBytes(), 0, 5);
		 
		assertEquals(4,len);

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
	
	
	@Test
	public void testCheckForTerminator_cacheAccumulation() {
		TerminatingTransformer trans = new TerminatingTransformer("\r\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] in1 = "\r".getBytes();
		int len = trans.checkForTerminator(in1, 0, 1);
		
		assertEquals(1,len);
		
		byte[] cache1 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertNotNull(cache1);
		assertEquals(1, cache1.length);
		assertFalse( in1 == cache1 );
		assertEquals(in1[0], cache1[0]);

		byte[] in2 = "\n".getBytes();
		len = trans.checkForTerminator(in2, 0, 1);
		
		assertEquals(0,len);
		
		byte[] cache2 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache2);
	}
	
	
	@Test
	public void testCheckForTerminator_cacheAccumulations() {
		TerminatingTransformer trans = new TerminatingTransformer("\t\r\n".getBytes(), null);
		
		assertTrue( (boolean) TestUtils.reflectValue(trans, "transforming") );
		
		byte[] in1 = "\t".getBytes();
		int len = trans.checkForTerminator(in1, 0, 1);
		
		assertEquals(1,len);
		
		byte[] cache1 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertNotNull(cache1);
		assertEquals(1, cache1.length);
		assertFalse( in1 == cache1 );
		assertEquals(in1[0], cache1[0]);
		
		byte[] in2 = "\r".getBytes();
		len = trans.checkForTerminator(in2, 0, 1);
		
		assertEquals(1,len);
		
		byte[] cache2 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertNotNull(cache2);
		assertEquals(2, cache2.length);
		assertEquals(in1[0], cache2[0]);
		assertEquals(in2[0], cache2[1]);

		byte[] in3 = "\n".getBytes();
		len = trans.checkForTerminator(in3, 0, 1);
		
		assertEquals(0,len);
		
		byte[] cache3 = (byte[]) TestUtils.reflectValue(trans, "cache");
		assertEquals(null, cache3);
	}
	
	
	@Test
	public void testTransformLocal_transforming() {
		
		final byte[] expected = new byte[]{1,2,3};
		
		Transformer transform = new Transformer(){
			@Override
			public byte[] transform(byte[] bytes, int off, int len) {
				return expected;
			}
		};
		byte[] actual = new TerminatingTransformer("s".getBytes(), transform).transformLocal("qwerty".getBytes(), 0, 6);
		
		assertEquals(expected, actual);
	}

	@Test
	public void testTransformLocal_notTransforming() {
		
		Transformer transform = new Transformer(){
			@Override
			public byte[] transform(byte[] bytes, int off, int len) {
				throw new RuntimeException("should not be called");
			}
		};
		TerminatingTransformer term = new TerminatingTransformer("s".getBytes(), transform);
		TestUtils.refectSetValue(term, "transforming", false);
		
		byte[] expect = "qwerty".getBytes();
		byte[] actual = term.transformLocal(expect, 0, 6);
		
		assertArrayEquals(expect, actual);
	}


	@Test
	public void testTransform_noTerminator() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), transform);
		
		byte[] original    = "<asdf></asdf>".getBytes();
		byte[] transformed = term.transform(original, 0, original.length);
		byte[] remainder   = term.getRemaining();
		transformed        = Transformer.merge(transformed, remainder);
		
		TestUtils.log(new String(transformed));
		assertArrayEquals("<qwerty></qwerty>".getBytes(), transformed);
	}
	

	@Test
	public void testTransform_withTerminator() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), transform);
		
		term.transform("\n".getBytes(), 0, 1);

		byte[] original = "<asdf></asdf>".getBytes();
		byte[] transformed = term.transform(original, 0, original.length);

		assertArrayEquals(original, transformed);
		TestUtils.log(new String(transformed));
	}
	

	@Test
	public void testTransform_withTerminatorInLine() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), transform);
		
		byte[] original = "<asdf></asdf>\n<asdf></asdf>".getBytes();
		byte[] transformed = term.transform(original, 0, original.length);

		byte[] expected = "<qwerty></qwerty>\n<asdf></asdf>".getBytes();
		assertArrayEquals(expected, transformed);
		TestUtils.log(new String(transformed));
	}
	
	
	@Test
	public void testTransform_trickleBytes() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), transform);
		
		byte[] original = "<asdf></asdf>\n<asdf></asdf>".getBytes();
		StringBuilder transformed = new StringBuilder();
		for (int b=0; b<original.length; b++) {
			byte[] trans = term.transform(original, b, 1);
			if (trans.length>0) {
				transformed.append( new String(trans) );
			}
		}
//		transformed.append( new String(term.getRemaining()) );
		TestUtils.log(new String(transformed));
		String expected = "<qwerty></qwerty>\n<asdf></asdf>";
		assertEquals(expected, transformed.toString());
	}
}
