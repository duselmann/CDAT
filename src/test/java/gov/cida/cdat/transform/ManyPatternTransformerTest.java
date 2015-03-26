package gov.cida.cdat.transform;

import static org.junit.Assert.*;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.TransformOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

public class ManyPatternTransformerTest {

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
