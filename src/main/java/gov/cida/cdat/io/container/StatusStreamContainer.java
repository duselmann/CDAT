package gov.cida.cdat.io.container;

import gov.cida.cdat.io.StatusOutputStream;

import java.io.OutputStream;

public class StatusStreamContainer extends ChainedStreamContainer<StatusOutputStream> {

	public StatusStreamContainer(StreamContainer<OutputStream> target) {
		super(target);
	}

	@Override
	protected StatusOutputStream chain(OutputStream stream) {
		return new StatusOutputStream(stream);
	}
}
