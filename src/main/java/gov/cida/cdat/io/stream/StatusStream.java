package gov.cida.cdat.io.stream;

import gov.cida.cdat.io.StatusOutputStream;

import java.io.OutputStream;

public class StatusStream extends ChainedStream<StatusOutputStream> {

	public StatusStream(StreamContainer<OutputStream> target) {
		super(target);
	}

	@Override
	protected StatusOutputStream chain(OutputStream stream) {
		return new StatusOutputStream(stream);
	}
}
