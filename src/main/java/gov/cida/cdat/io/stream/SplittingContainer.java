package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.SplitOutputStream;

import java.io.OutputStream;

public abstract class SplittingContainer<S extends SplitOutputStream> extends StreamContainer<SplitOutputStream> {

	private StreamContainer<OutputStream>[] targets;
	private S stream;

	public SplittingContainer(@SuppressWarnings("unchecked") StreamContainer<OutputStream> ... targets) {
		this.targets = targets;
	}
	
	@Override
	protected String getName() {
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
		for (StreamContainer<OutputStream> target : targets) {
			stream = chain( target.open() );
		}
		return stream;
	}
	
	
	@Override
	protected final void cleanup() {
		for (StreamContainer<OutputStream> target : targets) {
			Closer.close(target);
		}
	}
	
	/**
	 * This is a specific getStream that is typed 
	 * @return
	 */
	public final S getSplitStream() {
		return stream;
	}
}
