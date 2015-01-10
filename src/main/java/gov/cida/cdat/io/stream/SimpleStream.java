package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.api.AbstractStream;

import java.io.Closeable;

public class SimpleStream<S extends Closeable> extends AbstractStream<S> {

	// simple stream wrapper where open applies the given stream to flow
	private S stream;
	
	public SimpleStream(S stream) {
		this.stream = stream;
	}
	
	@Override
	public S open() throws StreamInitException {
		return setStream(stream);
	}
}
