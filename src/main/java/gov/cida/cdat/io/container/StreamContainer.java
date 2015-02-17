package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract implementation for building up a flow of streams. Each stream obtained should be done
 * in an instance of this class. This way you can build up a flow of streams and then open them all at
 * or about the same time instead of opening each one at a time and holding them open while the next
 * is open. Then, since they are chained, upon close all opened resources will have its close called.
 *
 * 
 * @author duselmann
 *
 * @param <S> Implementations must be closeable an nothing else.
 */
public abstract class StreamContainer<S extends Closeable> implements Closeable, Openable<S> {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * This is the stream that is created during init
	 */
	private S stream;
	/**
	 * <p>This is the StreamContainer of the chained downstream flow
	 * </p>
	 * <p>For (contrived) example:</p>
	 * <p>TransformToCSV-&gt;TransformToColumnHeader-&gt;TransformToZip-&gt;HttpResponse.outputstream
	 * </p>
	 * <p>Here the downstream to CSV is the Column Header, the downstream of Zip is the HttpResponse.
	 * </p>
	 */
	private StreamContainer<?> downstream;
	
	/**
	 * The construction for the final downstream implementation. For instance: HttpResponse
	 */
	public StreamContainer() {}
	/**
	 * This is the constructor for the intervening stream containers where we would like to call
	 * close 
	 * @param downstream
	 */
	public StreamContainer(StreamContainer<?> downstream) {
		this.downstream = downstream;
	}
	
	
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
		logger.trace("Open called: {} ", getName());
		// TODO test that this is correct, works, and sufficient
		// TODO could be that we need a new boolean hasBeenOpened or like mechanism
		if ( stream != null ) {
			throw new StreamInitException("Can only open stream once.");
		}
		S initStream = init();
		return setStream(initStream);
	}

	/**
	 * This is called (or should be) when then stream contained within is no longer needed
	 */
	@Override
	public final void close() throws IOException {
		logger.trace("Close called: {} ", getName());
		try {
			if (getStream() instanceof Flushable) {
				((Flushable)getStream()).flush();
			// This used to be used prior to Flushable interface
//			} else {
//				Method flush;
//				Class<?> streamClass = getStream().getClass();
//				if ( null != ( flush = streamClass.getMethod("flush") ) ) {
//					logger.trace("Flush called: {} ", streamClass.getName());
//					flush.invoke(getStream());
//				}
			}
		} catch (Exception e) {
			// does not matter, if flush not available then do not do it
		} finally {
			cleanup(); // TODO determine ideal calling location for this method
			closeStream();
			if (downstream != null) {
				downstream.close();
				downstream = null;
			}
		}
	}
	
	
	/**
	 * called by close for subclasses to clean-up resources on close
	 * for example the DBA stream should free the JDBC connection
	 */
	protected void cleanup() {
		
	}

	/**
	 * @return the stream created by this container
	 */
	public final S getStream() {
		return stream;
	}
	/**
	 * The setter is protected to allow the class and its children to set the stream.
	 * Since it would be bad to loose reference to a stream by creating a new one,
	 * this setter does not accept new streams once the stream has been set until it 
	 * has been closed.
	 * @param newStream the new stream to assign to this container. 
	 * Ideally, it would the one created by the container.
	 * @return the stream created by the container.
	 */
	protected final S setStream(S newStream) {
		// original stream protection
		if ( stream == null ) {
			stream = newStream;
		}
		return stream;
	}
	
	/**
	 * Helper method that closes the container stream and sets it to null for gc
	 */
	private void closeStream() {
		S oldStream = getStream();
		stream = null;
		if (oldStream != null) {
			logger.trace("Close stream: {} ", oldStream.getClass().getName());
			Closer.close(oldStream);
		}
	}
	
	/**
	 * @return true if the container has been open, stream has been 
	 */
	public boolean isOpen() {
		return stream != null;
	}
}
