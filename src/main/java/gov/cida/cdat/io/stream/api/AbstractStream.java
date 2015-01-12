package gov.cida.cdat.io.stream.api;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;

public abstract class AbstractStream<S extends Closeable> implements Stream<S> {
	
	private S stream;
	
	
	/**
	 * This is used to identify the subclass in logging
	 * @return String name of the implementing subclass
	 */
	abstract protected String getName();
	
	
	/**
	 * This must be implemented to initialize the underlying streams
	 * @return the opened stream that has been initialized
	 * @throws StreamInitException thrown if there was an error opening the stream
	 */
	//TODO the exception list will grow as impl is fleshed out. i.e. SourceNotFound...
	abstract protected S init() throws StreamInitException; 
	
	
	/**
	 * the open has been finalized to ensure that streams are opened only once
	 */
	@Override
	public final S open() throws StreamInitException {
		System.out.println("Open called: " + getName());
		// TODO test that this is correct, works, and sufficient
		// TODO could be that we need a new boolean hasBeenOpened or like mechanism
		if ( stream != null ) {
			throw new StreamInitException("Can only open stream once.");
		}
		return setStream( init() );
	}

	
	@Override
	public final void close() throws IOException {
		System.out.println("Close called: " + getName());
		try {
			Method flush;
			if ( null != ( flush = getStream().getClass().getMethod("flush") ) ) {
				System.out.println("Flush called: " +getStream().getClass().getName());
				flush.invoke(getStream());
			}
			
			cleanup(); // TODO determine ideal calling location for this method
			
			Closer.close(stream);
			closeStream();
		} catch (Exception e) {
			// does not matter, if flush not available then do not do it
		}
	}
	
	
	/**
	 * called by close for subclasses to clean-up resources on close
	 * for example the DBA stream should free the JDBC connection
	 */
	protected void cleanup() {
		
	}

	
	@Override
	public final S getStream() {
		return stream;
	}	
	protected final S setStream(S newStream) {
		// original stream protection
		if ( this.stream == null ) {
			this.stream = newStream;
		}
		return newStream;
	}
	private void closeStream() throws IOException {
		// NPE protection
		S oldStream = getStream();
		stream = null;

		oldStream.close(); // chain call to ensure down stream can free resources
	}
	
	
	public boolean isOpen() {
		return stream != null;
	}
}
