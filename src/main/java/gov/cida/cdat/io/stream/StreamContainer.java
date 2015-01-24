package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO StreamContainer and remove the interface entirely
public abstract class StreamContainer<S extends Closeable> implements Closeable, Openable<S> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
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
	abstract public S init() throws StreamInitException; 
	
	
	/**
	 * the open has been finalized to ensure that streams are opened only once
	 */
	@Override
	public final S open() throws StreamInitException {
		logger.debug("Open called: {} ", getName());
		// TODO test that this is correct, works, and sufficient
		// TODO could be that we need a new boolean hasBeenOpened or like mechanism
		if ( stream != null ) {
			throw new StreamInitException("Can only open stream once.");
		}
		return setStream( init() );
	}

	
	@Override
	public final void close() throws IOException {
		logger.debug("Close called: {} ", getName());
		try {
			Method flush;
			Class<?> streamClass = getStream().getClass();
			if ( null != ( flush = streamClass.getMethod("flush") ) ) {
				logger.debug("Flush called: {} ", streamClass.getName());
				flush.invoke(getStream());
			}
		} catch (Exception e) {
			// does not matter, if flush not available then do not do it
		} finally {
			cleanup(); // TODO determine ideal calling location for this method
			closeStream();
		}
	}
	
	
	/**
	 * called by close for subclasses to clean-up resources on close
	 * for example the DBA stream should free the JDBC connection
	 */
	protected void cleanup() {
		
	}

	
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
		S oldStream = getStream();
		stream = null;
		if (oldStream != null) {
			logger.debug("Close stream: {} ", oldStream.getClass().getName());
			Closer.close(oldStream);
		}
	}
	
	
	public boolean isOpen() {
		return stream != null;
	}
}
