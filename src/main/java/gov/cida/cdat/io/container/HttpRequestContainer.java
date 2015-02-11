package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.StreamInitException;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestContainer extends StreamContainer<InputStream> {

	private final HttpServletRequest request;
	
	public HttpRequestContainer(HttpServletRequest request) {
		this.request = request;
	}
	
	/**
	 *  implementations should define request parameter here
	 */
	public void apply() {
	}
	
	/**
	 *  implementations should define request parameters in
	 *  the apply method and also have the option to override the init
	 */
	@Override
	public InputStream init() throws StreamInitException {
		try {
			return request.getInputStream();
		} catch (IOException e) {
			throw new  StreamInitException("Failed to open http request stream", e);
		}
	}

	@Override
	protected String getName() {
		return getClass().getName();
	}
}
