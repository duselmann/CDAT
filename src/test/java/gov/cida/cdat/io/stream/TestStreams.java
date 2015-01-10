package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;

public class TestStreams {

	public static void main(String[] args) throws Exception {
		urlPipedStream();
		
	}
	
	public static void urlPipedStream() throws Exception {
		
		System.out.println("pipe build");
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> out  = new SimpleStream<OutputStream>(target);
		
		final PipeStream pipe = new PipeStream(google, out);
		
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
				} catch (StreamInitException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		System.out.println("main waithing for pipe...");
		Thread.sleep(1000);
		System.out.println("pipe close");
		pipe.close();
		
		System.out.println("pipe results");
		System.out.println( target.size() );
		System.out.println( new String(target.toByteArray(), 0, 100) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
