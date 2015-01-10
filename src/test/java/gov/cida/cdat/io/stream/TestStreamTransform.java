package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;

public class TestStreamTransform {

	public static void main(String[] args) throws Exception {
		urlPipedStream();
		
	}
	
	public static void urlPipedStream() throws Exception {
		
		System.out.println("pipe build");
		
		// Consumer
		ByteArrayOutputStream      target = new ByteArrayOutputStream(1024*10);

		// Transformer
		Transformer transform = new Transformer();
		TransformOutputStream<OutputStream> 
			tout = new TransformOutputStream<OutputStream>(target, transform);

		SimpleStream<OutputStream> out  = new SimpleStream<OutputStream>(tout);

		// Producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// Pipe producer to consumer
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
		
		String msg = "div not found, ";
		if ( new String(target.toByteArray()).toLowerCase().contains("span") ) {
			msg = "span ";
		}
		if ( new String(target.toByteArray()).toLowerCase().contains("div") ) {
			msg += " & div ";
		}
		
		System.out.println();
		System.out.println(msg + "found");
	}

}
