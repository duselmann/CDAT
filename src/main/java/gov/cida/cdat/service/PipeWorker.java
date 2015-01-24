package gov.cida.cdat.service;

import gov.cida.cdat.io.stream.DataPipe;

public class PipeWorker extends Worker {

	private final DataPipe pipe;
	
	public PipeWorker(DataPipe pipe) {
		this.pipe = pipe;
	}
	
	@Override
	public void begin() throws Exception {
		super.begin();
		pipe.open();
	}
	
	@Override
	public void end() {
		try {
			pipe.close();
		} finally {
			super.end();
		}
	}
	
}
