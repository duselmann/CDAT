package gov.cida.cdat.io.stream.api;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;

public abstract class AbstractStream<S extends Closeable> implements Stream<S> {
	
	private S stream;
	
	@Override
	public S open() throws StreamInitException {
		if ( getStream() == null ) {
			throw new StreamInitException("Can only open stream once.");
		}
		return null;
	}
	@Override
	public void close() throws IOException {
		try {
			Method flush;
			if ( null != ( flush = getStream().getClass().getMethod("flush") ) ) {
				System.out.println("Flush called.");
				flush.invoke(getStream());
			}
		} catch (Exception e) {
			// does not matter, if flush not available then do not do it
		}
		System.out.println("Close called.");
		Closer.close( getStream() );
	}

	
	@Override
	public final S getStream() {
		return stream;
	}	
	protected final S setStream(S stream) {
		if ( getStream() == null ) {
			this.stream = stream;
		}
		return getStream();
	}
	
	
	public boolean isOpen() {
		return stream != null;//  &&  stream.isOpen();
	}
}
