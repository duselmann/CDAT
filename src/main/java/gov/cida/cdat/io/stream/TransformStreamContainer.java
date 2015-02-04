package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.transform.Transformer;

import java.io.OutputStream;

public class TransformStreamContainer<T> extends StreamContainer<TransformOutputStream<T>> {

	private StreamContainer<OutputStream> downstream;
	private Transformer<T> transform;
	
	public TransformStreamContainer(Transformer<T> transform, StreamContainer<OutputStream> target) {
		super(target);
		this.downstream = target;
		this.transform  = transform;
	}
	
	@Override
	public TransformOutputStream<T> init() throws StreamInitException {
		OutputStream stream = downstream.open();
		return new TransformOutputStream<T>(stream, transform);
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
}
