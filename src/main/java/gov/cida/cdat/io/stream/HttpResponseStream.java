package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class HttpResponseStream extends AbstractStream<OutputStream> implements StreamConsumer<OutputStream> {
	
	private final HttpServletResponse response;
	
	public HttpResponseStream(HttpServletResponse response) {
		this.response = response;
	}
	
	@Override
	public OutputStream open() throws StreamInitException {
		try {
			return setStream( response.getOutputStream() );
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open http request stream", e);
		}
	}
}
