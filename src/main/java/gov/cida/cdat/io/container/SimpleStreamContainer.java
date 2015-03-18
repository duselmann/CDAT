package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;

import java.io.Closeable;

public final class SimpleStreamContainer<S extends Closeable> extends StreamContainer<S> {

	// simple stream wrapper where open applies the given stream to flow
	private final S wrappedStream;
	
	public SimpleStreamContainer(S stream) {
		this.wrappedStream = stream;
	}
	
	@Override
	public S init() throws StreamInitException {
		return wrappedStream;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}
}
