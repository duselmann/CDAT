package gov.cida.cdat.transform;

import static org.junit.Assert.*;

import org.junit.Test;

public class TerminatingManyPatternTransformTest {

	// this is to test te combo of two transformers: Terminating and ManyPattern
	
	@Test
	public void testSinglePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,BETA,GAMMA,DELTA,EPSILON,IOTA\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), trans);
		String actual = new String( term.transform(bytes, 0, bytes.length ) );
		
		assertEquals(expect, actual);
	}

	@Test
	public void testDoublePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,IOTA\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), trans);
		String actual = new String( term.transform(bytes, 0, bytes.length ) );
		
		assertEquals(expect, actual);
	}
	
	@Test
	public void testTriplePatternWithFullBuffer() {
		ManyPatternTransformer trans = new ManyPatternTransformer();
		trans.addMapping("ALPHA", "OMEGA");
		trans.addMapping("BETA", "SIGMA");
		trans.addMapping("IOTA", "PSI");
		
		byte[] bytes  = "ALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA".getBytes(); 
		String expect = "OMEGA,SIGMA,GAMMA,DELTA,EPSILON,PSI\nALPHA,BETA,GAMMA,DELTA,EPSILON,IOTA";
		
		TerminatingTransformer term = new TerminatingTransformer("\n".getBytes(), trans);
		String actual = new String( term.transform(bytes, 0, bytes.length ) );
		
		assertEquals(expect, actual);
	}
}
