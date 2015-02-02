package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.transform.RegexTransformer;
import gov.cida.cdat.transform.Transformer;

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
		Transformer transform = new RegexTransformer("div","span");
		TransformOutputStream tout = new TransformOutputStream(target, transform);

		SimpleStreamContainer<OutputStream> out  = new SimpleStreamContainer<OutputStream>(tout);

		// Producer
		URL url = new URL("http://www.google.com");
		UrlStreamContainer google = new UrlStreamContainer(url);
		
		// Pipe producer to consumer
		final DataPipe pipe = new DataPipe(google, out);
		
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
		System.out.println("pipe close");
		pipe.close();
		
		System.out.println("pipe results: expect all div tags converted to span");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String msg = "as desired, div not found, ";
		if ( new String(target.toByteArray()).toLowerCase().contains("span") ) {
			msg += "span ";
		}
		if ( new String(target.toByteArray()).toLowerCase().contains("div") ) {
			msg = "oops! div ";
		}
		
		System.out.println();
		System.out.println(msg + "found");
	}

}
