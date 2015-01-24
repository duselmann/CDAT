package gov.cida.cdat;


import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.io.stream.DataPipe;
import gov.cida.cdat.io.stream.SimpleStream;
import gov.cida.cdat.io.stream.UrlStream;
import gov.cida.cdat.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// there should be no name conflicts because each thread will have its own session
public class TestControlThreaded {

	private static ByteArrayOutputStream target;
	private static String workerLabel = "google";
	private static SCManager manager;
	
	
	public static void main(String[] args) throws Exception {
		manager = SCManager.instance();
		
		try {
			// no delay test
			spawnThread("first");
			spawnThread("second");
			
			// delayed test
			Thread.sleep(2000);
			spawnThread("third");
			
			Thread.sleep(2000);
			spawnThread("forth");
			
			Thread.sleep(2000);
		} finally {
			System.out.println("shuttdown submitted");
			manager.shutdown();
		}
	}


	private static void spawnThread(final String label) {
		System.out.println("starting " +label+ " new thread");
		new Thread(new Runnable() {
			private final Logger logger = LoggerFactory.getLogger(getClass());
			@Override
			public void run() {
				try {
					logger.debug("running off main thread on {} thread", label);
					submitJob();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}).start(); // remember not to use run
	}


	private static void submitJob() throws MalformedURLException {
		// consumer
		target = new ByteArrayOutputStream(1024*10);
		SimpleStream<OutputStream>     out = new SimpleStream<OutputStream>(target);
		
		// producer
		URL url = new URL("http://www.google.com");
		UrlStream in = new UrlStream(url);
		
		// pipe
		DataPipe pipe = new DataPipe(in, out);		
		
		final String workerName = manager.addWorker(workerLabel, pipe);
		
		manager.send(workerName, Message.create("Message", "Test"));
		
		// This is called with a null response if the Patterns.ask timeout expires
		manager.send(workerName, Control.onComplete, new Callback(){
	        public void onComplete(Throwable t, Message response){
	            report(workerName, response);		
	        }
	    });
		
		manager.send(workerName, Control.Start);		
	}
	
	
	private static void report(String workerName, final Message response) {
        System.out.println("onComplete Response is " + response);
		
		System.out.println("pipe results");
		System.out.println( "total bytes: " +target.size() );
		if (target.size()>100) {
			System.out.println( new String(target.toByteArray(), 0, 100) );
		} else {
			System.out.println("ERROR: Received too little data.");
		}
		String msg = "Google Not Found";
		if ( new String(target.toByteArray()).contains("Google") ) {
			msg = "Google Found";
		}
		System.out.println();
		System.out.println(msg);
	}

}
