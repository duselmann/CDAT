package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.exception.producer.ConnectionException;
import gov.cida.cdat.exception.producer.FileNotFoundException;
import gov.cida.cdat.exception.producer.SourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlStreamContainer extends StreamContainer<InputStream> {

	private final URL url;
	
	public UrlStreamContainer(URL url) {
		this.url = url;
	}

	@Override
	public InputStream init() throws StreamInitException {
		try {
			return url.openStream();
		} catch (java.net.UnknownHostException e) {
			throw new SourceNotFoundException("Failed to open URL stream", e);
		} catch (java.io.FileNotFoundException e) {
			throw new FileNotFoundException("The URL returned with a 404, File Not Found.", e);
		} catch (MalformedURLException e) {
			throw new ConnectionException("Invalid URL",e);
		} catch (IOException e) {
			throw new StreamInitException("Failed to open URL stream", e);
		}
	}
	
	@Override
	public String getName() {
		return getClass().getName();
	}
}
