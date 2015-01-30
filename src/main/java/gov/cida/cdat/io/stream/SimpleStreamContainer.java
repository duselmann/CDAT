package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.Closeable;

public final class SimpleStreamContainer<S extends Closeable> extends StreamContainer<S> {

	// simple stream wrapper where open applies the given stream to flow
	private final S stream;
	
	public SimpleStreamContainer(S stream) {
		this.stream = stream;
	}
	
	@Override
	public S init() throws StreamInitException {
		return stream;
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
}
