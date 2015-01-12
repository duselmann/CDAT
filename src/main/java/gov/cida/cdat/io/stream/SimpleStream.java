package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.api.AbstractStream;

import java.io.Closeable;

public final class SimpleStream<S extends Closeable> extends AbstractStream<S> {

	// simple stream wrapper where open applies the given stream to flow
	private final S stream;
	
	public SimpleStream(S stream) {
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
