package gov.cida.cdat.io.stream;

import gov.cida.cdat.io.StatusOutputStream;
import gov.cida.cdat.io.stream.api.Stream;

import java.io.OutputStream;

public class StatusStream extends ChainedStream<StatusOutputStream> {

	public StatusStream(Stream<OutputStream> target) {
		super(target);
	}

	@Override
	protected StatusOutputStream chain(OutputStream stream) {
		return new StatusOutputStream(stream);
	}
}
