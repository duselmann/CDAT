package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.stream.api.AbstractStream;
import gov.cida.cdat.transform.Transformer;

import java.io.OutputStream;

public class TransformStream extends AbstractStream<TransformOutputStream> {

	private OutputStream stream;
	private Transformer transform;
	
	public TransformStream(Transformer transform, OutputStream stream) {
		this.stream = stream;
		this.transform = transform;
	}
	
	@Override
	public TransformOutputStream init() throws StreamInitException {
		return new TransformOutputStream(stream, transform);
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
}
