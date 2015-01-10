package gov.cida.cdat.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import scala.sys.process.ProcessBuilder.Source;



public class HttpWorker extends Worker   {

	private BufferedInputStream stream;
	
	
	@Override
	public void begin() throws Exception {
		super.begin();
		InputStream http = new URL("http://www.google.com").openStream();
		stream = new BufferedInputStream(http,4096);

	}
	
//	@Override
//	public void write(int b) throws IOException {
//
//	}
	
	@Override
	public void end() {
		super.end();
	}

}
