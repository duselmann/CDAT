package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.StatusOutputStream;
import gov.cida.cdat.io.stream.api.AbstractStream;
import gov.cida.cdat.io.stream.api.Stream;

import java.io.OutputStream;

public class StatusStream extends AbstractStream<OutputStream> {

	private Stream<OutputStream> target;
	private StatusOutputStream status;

	public StatusStream(Stream<OutputStream> target) {
		this.target = target;
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}

	@Override
	public StatusOutputStream init() throws StreamInitException {
		status = new StatusOutputStream( target.open() );
		return status;
	}
	
	@Override
	protected void cleanup() {
		Closer.close(target);
	}
	
	public StatusOutputStream getStatus() {
		return status;
	}
}
