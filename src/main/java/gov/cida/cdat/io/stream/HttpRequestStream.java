package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestStream extends AbstractStream<InputStream> implements StreamProducer<InputStream> {

	private final HttpServletRequest request;
	
	public HttpRequestStream(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public InputStream open() throws StreamInitException {
		super.open();
		try {
			return setStream( request.getInputStream() );
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open http request stream", e);
		}
	}
}
