package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.SplitOutputStream;

import java.io.OutputStream;

public abstract class SplittingContainer extends StreamContainer<SplitOutputStream> {

	private StreamContainer<? extends OutputStream>[] targets;
	private OutputStream[] childTargetStreams;
	private SplitOutputStream stream;

	public SplittingContainer(@SuppressWarnings("unchecked") StreamContainer<? extends OutputStream> ... targets) {
		this.targets = targets;
		childTargetStreams = new OutputStream[targets.length];
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
	protected OutputStream chain(OutputStream stream) {
		return stream;
	}
	
	
	@Override
	public final SplitOutputStream init() throws StreamInitException {
		// TODO does it make sense for init() to call another open
		// TODO I would like init() to be the chaining and open to be the this action
		// for now it is necessary for chaining
		int child = 0;
		for (StreamContainer<? extends OutputStream> target : targets) {
			childTargetStreams[child++] = chain( target.open() );
		}
		stream = new SplitOutputStream(childTargetStreams);
		return stream;
	}
	
	
	@Override
	protected final void cleanup() {
		for (StreamContainer<? extends OutputStream> target : targets) {
			Closer.close(target);
		}
	}
	
	/**
	 * This is a specific getStream that is typed 
	 * @return
	 */
	public final SplitOutputStream getSplitStream() {
		return stream;
	}
}
