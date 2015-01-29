package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.CdatException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;

public class TestStreams {

	public static void main(String[] args) throws Exception {
		urlPipedStream();
		
	}
	
	public static void urlPipedStream() throws Exception {
		
		System.out.println("pipe build");
		
		// consumer
		ByteArrayOutputStream      target   = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> consumer = new SimpleStream<OutputStream>(target);

		// producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// pipe
		final DataPipe pipe = new DataPipe(google, consumer);
		
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
					pipe.processAll();
				} catch (CdatException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		System.out.println("main waithing for pipe...");
		Thread.sleep(1000);
		System.out.println("main closing pipe");
		pipe.close();
		
		System.out.println("pipe results: expect Google page loaded on separate thead, checking also that all levels are closed and flush called if found.");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
