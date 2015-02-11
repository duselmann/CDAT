package gov.cida.cdat.io.container;

import static org.junit.Assert.*;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.container.SplittingContainer;
import gov.cida.cdat.io.container.StreamContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

public class SplitStreamContainerTests {

	@Test
	public void testSuccessfulSplit() throws Exception {
		final Boolean[] closeCalled = new Boolean[3];
		ByteArrayOutputStream[] outs = new ByteArrayOutputStream[3];
		
		@SuppressWarnings("unchecked")
		StreamContainer<ByteArrayOutputStream>[] containers = new StreamContainer[3];
		
		for(int b=0; b<3; b++) {
			final int b_final = b;
			outs[b] = new ByteArrayOutputStream(1024){
				@Override
				public void close() throws IOException {
					closeCalled[b_final] = true;
				}
			};
			final ByteArrayOutputStream out_final = outs[b];
			containers[b] = new StreamContainer<ByteArrayOutputStream>() {
				ByteArrayOutputStream wrappedStream = out_final;
				@Override
				public ByteArrayOutputStream init() throws StreamInitException {
					return wrappedStream;
				}
				@Override
				protected String getName() {
					return "test spliting stream";
				}
			};
		}
		
		final int[] chainCalls = new int[1];
		SplittingContainer splittingContainer = new SplittingContainer(containers) {
			@Override
			protected OutputStream chain(OutputStream stream) {
				chainCalls[0]++;
				return stream;
			}
		};
		
		assertFalse("Expect the stream NOT to be open yet", splittingContainer.isOpen());
		
		assertEquals("Expect the chain called zero before open", 0, chainCalls[0]);
		
		String testBytes = "Test Bytes";
		splittingContainer.open();
		assertTrue("Expect the stream to be open yet", splittingContainer.isOpen());
		
		assertEquals("Expect the chain to called once per split stream on open", 3, chainCalls[0]);
		
		splittingContainer.getSplitStream().write(testBytes.getBytes());		
		
		splittingContainer.close();
		assertFalse("Expect the stream NOT to be open any longer", splittingContainer.isOpen());
		
		int child=0;
		for(ByteArrayOutputStream out : outs) {
			assertTrue("Expect the close to called once per split stream on close", closeCalled[child++]);
			String result = new String(out.toByteArray());
			assertEquals("Expect each split output to have the same as the single input", testBytes, result);
		}
	}

}
