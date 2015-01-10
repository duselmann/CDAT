package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.api.AbstractStream;

import java.io.OutputStream;

public class TransformStream extends AbstractStream<TransformOutputStream<OutputStream>> {

	// simple stream wrapper where open applies the given stream to flow
	private TransformOutputStream<OutputStream> stream;
	
	public TransformStream(TransformOutputStream<OutputStream> stream) {
		this.stream = stream;
	}
	
	@Override
	public TransformOutputStream<OutputStream> open() throws StreamInitException {
		return setStream(stream);
	}
}
