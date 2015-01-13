package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.StatusOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;

public class TestStatusStreams {

	public static void main(String[] args) throws Exception {
		urlPipedStream();
	}
	
	public static void urlPipedStream() throws Exception {
		
		System.out.println("pipe build");
		
		// consumer
		ByteArrayOutputStream      target   = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream> consumer = new SimpleStream<OutputStream>(target);

		final StatusStream status = new StatusStream(consumer);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream google = new UrlStream(url);
		
		// pipe
		final PipeStream pipe = new PipeStream(google, status);
		status(status.getStatus());
		
		new Thread() {
			@Override
			public void run() {
				System.out.println("pipe open");
				try {
					pipe.open();
					status(status.getStatus());					
				} catch (StreamInitException e) {
					e.printStackTrace();
				}
			}

		}.start();
		new Thread() {
			@Override
			public void run() {
				System.out.println("status thread");
				while (status.getStatus() == null) {
					status(status.getStatus());
					try {Thread.sleep(50); } catch (InterruptedException e) {}
				}
				status(status.getStatus());
				try {Thread.sleep(50); } catch (InterruptedException e) {}
				status(status.getStatus());
			}

		}.start();
		
		System.out.println("main waithing for pipe...");
		Thread.sleep(1000);
		System.out.println("main closing pipe");
		pipe.close();
		
		System.out.println("pipe results");
		System.out.println("total size: " +target.size() );
		int length = target.size()>100 ?100 :target.size();
		System.out.println("first 100:" +new String(target.toByteArray(), 0, length) );
		
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
		
		status(status.getStatus());		
	}
	public static void status(StatusOutputStream status) {
		if (status == null) {
			System.out.println("  status is not init yet: " + status);
		} else {
			System.out.println("  status is open: " + status.isOpen());
			System.out.println("  status is done: " + status.isDone());
			System.out.println("  status is byteCount: " + status.getByteCount());
			System.out.println("  status time since last write: " + status.getMsSinceLastWrite());
			System.out.println("  status is open for: " + status.getOpenTime() +"ms");
		}
	}

}
