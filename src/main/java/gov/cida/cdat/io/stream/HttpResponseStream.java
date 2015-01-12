package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.api.AbstractStream;
import gov.cida.cdat.io.stream.api.StreamConsumer;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class HttpResponseStream extends AbstractStream<OutputStream> implements StreamConsumer<OutputStream> {
	
	private final HttpServletResponse response;
	
	public HttpResponseStream(HttpServletResponse response) {
		this.response = response;
	}
	
	/**
	 *  implementations should define response header and content-type
	 */
	public void apply() {
	}
	
	/**
	 *  implementations should define response header and content-type in
	 *  the apply method and also have the option to override the init
	 */
	@Override
	public OutputStream init() throws StreamInitException {
		try {
			apply();
			return response.getOutputStream();
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open http request stream", e);
		}
	}

	@Override
	protected final String getName() {
		return getClass().getName();
	}
}
