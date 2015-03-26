package gov.cida.cdat.transform;

import static org.junit.Assert.*;

import org.junit.Test;

public class RegexTransfromerTest {

	@Test
	public void testTransform_trickleBytes() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		
		String expected = "<qwerty></qwerty><qwerty></qwerty>";
		String source   = "<asdf></asdf><asdf></asdf>";
		byte[] original = source.getBytes();
		
		StringBuilder transformed = new StringBuilder();
		for (int b=0; b<original.length; b++) {
			byte[] trans = transform.transform(original, b, 1);
			if (trans.length>0) {
				transformed.append( new String(trans) );
			}
		}
		transformed.append(new String(transform.getRemaining()));
		assertEquals(expected, transformed.toString());
	}

	@Test
	public void testTransform_randomByteLengths() {
		Transformer transform = new RegexTransformer("asdf", "qwerty");
		
		String expected = "<qwerty></qwerty><qwerty></qwerty>";
		String source   = "<asdf></asdf><asdf></asdf>";
		byte[] original = source.getBytes();
		
		StringBuilder transformed = new StringBuilder();
		int len = 1;
		for (int b=0; b<original.length; b+=len) {
			len = 1 + (int)(Math.random()*3);
			len = (b+len > original.length) ?original.length-b :len; // bounds check
//			TestUtils.log(len);
			byte[] trans = transform.transform(original, b, len);
			if (trans.length>0) {
				transformed.append( new String(trans) );
			}
		}
		transformed.append(new String(transform.getRemaining()));
		assertEquals(expected, transformed.toString());
	}

}
