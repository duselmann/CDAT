package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UrlStream extends AbstractStream<InputStream> implements StreamProducer<InputStream> {

	private final URL url;
	
	public UrlStream(URL url) {
		this.url = url;
	}

	@Override
	public InputStream open() throws StreamInitException {
		try {
			return setStream( url.openStream() );
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open URL stream", e);
		}
	}
}
