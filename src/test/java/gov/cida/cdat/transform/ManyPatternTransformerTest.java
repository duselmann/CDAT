package gov.cida.cdat.transform;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.TransformOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.junit.Test;

public class ManyPatternTransformerTest {

	@Test
	public void testAddMapping_Order() throws IOException {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		trans.addMapping("OMICRON", "PIES");
		
		@SuppressWarnings("unchecked")
		List<RegexTransformer> transformers = (List<RegexTransformer>) TestUtils.reflectValue(trans, "transformers");
		
		assertEquals("OMICRON", transformers.get(0).getPattern());
		assertEquals("ALPHA", transformers.get(1).getPattern());
		assertEquals("IOTA", transformers.get(2).getPattern());
		assertEquals("BETA", transformers.get(3).getPattern());
		assertEquals("PI", transformers.get(4).getPattern());
	}
	
	@Test
	public void testValidation_nullPattern() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		try {
			trans.addMapping(null, "P");
			fail("Null pattern is not acceptable");
		} catch (RuntimeException e) {
			
		}
	}

	@Test
	public void testValidation_nullReplace() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		try {
			trans.addMapping("P", null);
			fail("Null replace is not acceptable");
		} catch (RuntimeException e) {
			
		}
	}

	@Test
	public void testValidation_emptyPattern() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		try {
			trans.addMapping("", "P");
			fail("Empty string pattern is not acceptable");
		} catch (RuntimeException e) {
			
		}
	}

	@Test
	public void testValidation_emptyReplace() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		try {
			trans.addMapping("P", "");
		} catch (RuntimeException e) {
			fail("Empty string replace is acceptable");
		}
	}

	@Test
	public void testValidation_patternEqualReplace() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		try {
			trans.addMapping("P", "P");
			fail("No transfrom will take place if the pattern==replace");
		} catch (RuntimeException e) {
			
		}
	}

	@Test
	public void testValidation_duplicatePattern() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		try {
			trans.addMapping("PI", "another");
			fail("Should not accept duplicate patterns");
		} catch (RuntimeException e) {
			
		}
	}

	@Test
	public void testValidation_replaceReplacedDownStream() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("PI", "ZETA");
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		try {
			trans.addMapping("OMICRON", "PI");
			fail("Should not accept patterns that will then be replaced");
		} catch (RuntimeException e) {
			
		}
		
	}
	
	@Test
	public void testValidation_replaceReplacedUpStream() throws IOException {
		
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		trans.addMapping("OMICRON", "PI");
		
		try {
			trans.addMapping("PI", "ZETA");
			fail("Should not accept patterns that will replace another");
		} catch (RuntimeException e) {
			
		}
		
	}
	
	@Test
	public void testNoPatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		String actual = new String( trans.transform(bytes, 0, bytes.length ) );
		
		assertEquals(expect, actual);
	}

	@Test
	public void testSinglePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		String actual = new String( trans.transform(bytes, 0, bytes.length ) );
		actual += new String(trans.getRemaining());
		
		assertEquals(expect, actual);
	}

	@Test
	public void testDoublePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,IOTA";
		
		String actual = new String( trans.transform(bytes, 0, bytes.length ) );
		actual += new String(trans.getRemaining());
		
		assertEquals(expect, actual);
	}
	
	@Test
	public void testTriplePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,PSI";
		
		String actual = new String( trans.transform(bytes, 0, bytes.length ) );
		actual += new String(trans.getRemaining());
		
		assertEquals(expect, actual);
	}


	@Test
	public void testSinglePatternWithTrickleBuffer() throws IOException {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		OutputStream stream = new TransformOutputStream(out, trans);
		for (int b=0; b<bytes.length; b++) {
			stream.write(bytes, b, 1);
		}
		stream.flush();
		Closer.close(stream);
		String actual = new String( out.toByteArray() );
		
		assertEquals(expect, actual);
	}

	@Test
	public void testDoublePatternWithTrickleBuffer() throws IOException {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,IOTA";
		
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		OutputStream stream = new TransformOutputStream(out, trans);
		for (int b=0; b<bytes.length; b++) {
			stream.write(bytes, b, 1);
		}
		stream.flush();
		Closer.close(stream);
		String actual = new String( out.toByteArray() );
		
		assertEquals(expect, actual);
	}

	@Test
	public void testTriplePatternWithTrickleBuffer() throws IOException {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,PSI";
		
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		OutputStream stream = new TransformOutputStream(out, trans);
		for (int b=0; b<bytes.length; b++) {
			stream.write(bytes, b, 1);
		}
		stream.flush();
		Closer.close(stream);
		String actual = new String( out.toByteArray() );
		
		assertEquals(expect, actual);
	}
}
