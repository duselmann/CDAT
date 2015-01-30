package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class HttpResponseContainer extends StreamContainer<OutputStream> {
	
	private final HttpServletResponse response;
	
	public HttpResponseContainer(HttpServletResponse response) {
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
