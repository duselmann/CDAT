package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.exception.producer.SourceNotFoundException;
import gov.cida.cdat.io.stream.api.AbstractStream;
import gov.cida.cdat.io.stream.api.StreamProducer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UrlStream extends AbstractStream<InputStream> implements StreamProducer<InputStream> {

	private final URL url;
	
	public UrlStream(URL url) {
		this.url = url;
	}

	@Override
	public InputStream init() throws StreamInitException {
		try {
			return url.openStream();
		} catch (java.net.UnknownHostException e) {
			// TODO change init sig to handle SourceNotFound
			throw new  StreamInitException("Failed to open URL stream", 
					new SourceNotFoundException("Failed to open URL stream", e) );
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open URL stream", e);
		}
	}
	
	@Override
	protected String getName() {
		return getClass().getName();
	}
}
