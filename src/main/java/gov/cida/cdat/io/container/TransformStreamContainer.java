package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.transform.Transformer;

import java.io.OutputStream;

public class TransformStreamContainer extends StreamContainer<TransformOutputStream> {

	private StreamContainer<? extends OutputStream> downstream;
	private Transformer transform;
	
	public TransformStreamContainer(Transformer transform, StreamContainer<? extends OutputStream> target) {
		super(target);
		this.downstream = target;
		this.transform  = transform;
	}
	
	@Override
	public TransformOutputStream init() throws StreamInitException {
		OutputStream stream = downstream.open();
		return new TransformOutputStream(stream, transform);
	}

	@Override
	public String getName() {
		return getClass().getName();
	}
}
