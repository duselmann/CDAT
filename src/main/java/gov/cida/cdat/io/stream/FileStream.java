package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.api.AbstractStream;
import gov.cida.cdat.io.stream.api.StreamProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileStream extends AbstractStream<InputStream> implements StreamProducer<InputStream> {

	private final File file;
	
	public FileStream(File file) {
		this.file = file;
	}

	@Override
	public InputStream open() throws StreamInitException {
		try {
			return setStream( new FileInputStream(file) );
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open URL stream", e);
		}
	}
}
