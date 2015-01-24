package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileStream extends StreamContainer<InputStream> {

	private final File file;
	
	public FileStream(File file) {
		this.file = file;
	}

	@Override
	public InputStream init() throws StreamInitException {
		try {
			return new FileInputStream(file);
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open URL stream", e);
		}
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
}
