package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;

import java.io.OutputStream;

public abstract class ChainedStreamContainer<S extends OutputStream> extends StreamContainer<OutputStream> {

	private StreamContainer<OutputStream> target;
	private S stream;

	public ChainedStreamContainer(StreamContainer<OutputStream> target) {
		this.target = target;
	}
	
	@Override
	public String getName() {
		return getClass().getName();
	}

	
	/**
	 * This is the init for ChainedStreams
	 * @param stream the given the stream of the chained 'parent'
	 * @return should return the stream this container is designed
	 */
	protected abstract S chain(OutputStream stream);
	
	
	@Override
	public S init() throws StreamInitException {
		// TODO does it make sense for init() to call another open
		// TODO I would like init() to be the chaining and open to be the this action
		// for now it is necessary for chaining
		stream = chain( target.open() );
		return stream;
	}
	
	
	@Override
	protected final void cleanup() {
		Closer.close(target);
	}
	
	/**
	 * This is a specific getStream that is typed to the chain 
	 * @return
	 */
	public final S getChainedStream() {
		// TODO try refactoring the generics to remove this method
		return stream;
	}
}
