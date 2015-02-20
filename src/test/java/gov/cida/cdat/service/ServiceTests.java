package gov.cida.cdat.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.io.container.DataPipe;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.message.AddWorkerMessage;
import gov.cida.cdat.service.Naming;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import scala.Function1;
import scala.Option;
import scala.PartialFunction;
import scala.Tuple2;
import scala.concurrent.Awaitable;
import scala.concurrent.CanAwait;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.reflect.ClassTag;
import scala.util.Try;
import akka.actor.ActorRef;
import akka.dispatch.OnComplete;


public class ServiceTests {

	/*
	 * Support Methods
	 */
	
	
	public static class PipeTestObj {
		public ByteArrayInputStream  producer;
		public ByteArrayOutputStream consumer;
		public DataPipe       pipe;
		
		public String results() {
			if (consumer == null) {
				return null;
			}
			return new String(consumer.toByteArray());
		}
	}
		
	public static PipeTestObj createTestPipe(String textString) {
		PipeTestObj testPipe = new PipeTestObj();
		
		// set up consumer
		testPipe.consumer = new ByteArrayOutputStream(1024*10);
		SimpleStreamContainer<OutputStream> out = new SimpleStreamContainer<OutputStream>(testPipe.consumer);
		
		// set up producer
		testPipe.producer  = new ByteArrayInputStream(textString.getBytes());
		SimpleStreamContainer<InputStream>   in = new SimpleStreamContainer<InputStream>(testPipe.producer);
		
		// pipe: connect the consumer and producer together with a data pump
		testPipe.pipe = new DataPipe(in, out);
				
		return testPipe;
	}

	
	/*
	 * TESTS FOLLOW
	 */
	
	
	@Test
	public void testSingleton() {
		Service instance1 = Service.open();
		try {
			Service instance2 = Service.instance();
			
			assertEquals("Singleton instances should be equivilent", instance1, instance2);
			assertTrue("Singleton instances should be equivilent references", instance1 == instance2);
		} finally {
			instance1.close();
		}
	}
	
	@Test
	public void testSession_SameSession() {
		Service session = Service.open();
		
		try {
			ActorRef instance1 = session.session();
			ActorRef instance2 = session.session();
			
			assertEquals("Session instances should be equivilent", instance1, instance2);
			assertTrue("Session instances should be equivilent references", instance1 == instance2);
		} finally {
			session.close();
		}
    }
	
	@Test
	public void testSession_DifferentTheadsDifferentSessions() throws Exception {
		final Service session = Service.open();
		
		try {
			ActorRef instance1 = session.session();
			
			final ActorRef[] sessionRef = new ActorRef[1];
			
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					sessionRef[0] = session.session();
				}
			});
			thread.start();
			thread.join();
			
			ActorRef instance2 = sessionRef[0];
	
			assertFalse("Session instances should be different across threads", instance1 == instance2);
		} finally {
			session.close();
		}
    }

	
	@Test
	// it is hard to test only one of these phases
	public void testWorker_StartRunCompleteStop() throws Exception {
		
		// obtain an instance of the session
		final Service  session = Service.open();

		try {
			// a test string that can be used for comparison
			String TEST_STRING = "Test String A";
	
			PipeTestObj testPipe = createTestPipe(TEST_STRING);
			
			// submit the pipe as a worker to the session on the session
			Worker        worker     = new PipeWorker(testPipe.pipe);
			final String  workerName = session.addWorker(TEST_STRING, worker);
	
			final String EXPECTED = TEST_STRING.replaceAll(" ","_")+"-1";
			// ensure that the response contains the worker name since a future is returned from the submit
			assertEquals("we expect the worker name returned from addWorker", EXPECTED, workerName);
			
			// this is a holder to pass data between threads.
			final Message[] response = new Message[1];
			
			// wait for worker to complete
			session.send(workerName, Control.onComplete, new Callback(){
	    		// This is called with a null response if the Patterns.ask timeout expires
		        public void onComplete(Throwable t, Message resp) {
		        	// get a reference to the message
		    		response[0] = resp;
		        }
		    });
			// start the worker (from cDAT point of view, from AKKA it is already running)
			session.send(workerName, Control.Start);
	
			// wait some time for the worker to finish
			int count = TestUtils.waitAlittleWhileForResponse(response);
			
	    	// this send the message that this worker is no longer needed
			session.send(workerName, Control.Stop);
			
			// lets just see the count
			TestUtils.log("wait cycle count:", count);
			
			assertTrue("Expect count to be must less than 100", count<100);
			
			// now test that the message is as we expect
			assertFalse("Message from on complete should not be null", response[0]==null );
			assertEquals("OnComplete Message from on complete should be 'done'", "done",  response[0].get(Control.onComplete) );
			
			// test that the data was pumped from producer to consumer
			assertEquals("Expected output: " + TEST_STRING, TEST_STRING, testPipe.results() );
		} finally {
			session.close();
		}
    }
		
	
	@Test
	// TODO would also like to check for memory leaks
	public void testWorker_workerDisposeOnStop() throws Exception {
		
		TestUtils.log("Start testing a second start");
		
		// obtain an instance of the session
		final Service  session = Service.open();

		try {
			// a test string that can be used for comparison
			String TEST_STRING = "Test String B";
	
			PipeTestObj testPipe = createTestPipe(TEST_STRING);
	
			// submit the pipe as a worker to the session on the session
			Worker        worker     = new PipeWorker(testPipe.pipe);
			final String  workerName = session.addWorker(TEST_STRING, worker);
	
			// this is a holder to pass data between threads.
			final Message[] response = new Message[1];
			
			// wait for worker to complete
			session.send(workerName, Control.onComplete, new Callback(){
	    		// This is called with a null response if the Patterns.ask timeout expires
		        public void onComplete(Throwable t, Message resp) {
		        	// get a reference to the message
		    		response[0] = resp;
		        }
		    });
			// start the worker (from cDAT point of view, from AKKA it is already running)
			session.send(workerName, Control.Start);
			
			// wait some time for the worker to finish
			TestUtils.waitAlittleWhileForResponse(response);
	
	    	// this send the message that this worker is no longer needed
			session.send(workerName, Control.Stop);
	
			TestUtils.log("Issuing second start");
			
			// clear out the consumer to see if it gets refilled
			testPipe.consumer.reset();
			
			// try to start it up again
			session.send(workerName, Control.Start);
			
			// wait some time for the worker to finish
			TestUtils.waitAlittleWhileForResponse(response);
			
			// now test that the consumer remains empty
			assertEquals("Expect that the disposed worker does not execute", "", testPipe.results() );
		} finally {
			session.close();
		}
    }
	
	@Test
	// it is hard to test only one of these phases
	public void testWorker_withCallback() throws Exception {
		
		// obtain an instance of the session
		final Service  session = Service.open();

		try {
			// a test string that can be used for comparison
			String TEST_STRING = "Test String C";
	
			PipeTestObj testPipe = createTestPipe(TEST_STRING);
			
			// this is a holder to pass data between threads.
			final Message[] response = new Message[1];
			
			// submit the pipe as a worker to the session on the session
			Worker        worker     = new PipeWorker(testPipe.pipe);
			session.addWorker(TEST_STRING, worker, new Callback(){
	    		// This is called with a null response if the Patterns.ask timeout expires
		        public void onComplete(Throwable t, Message resp) {
		        	// get a reference to the message
		    		response[0] = resp;
		        }
		    });
	
			// wait some time for the worker to finish
			TestUtils.waitAlittleWhileForResponse(response);
			
			final String EXPECTED = TEST_STRING.replaceAll(" ","_")+"-1";
			final String workerName = response[0].get(Naming.WORKER_NAME);
			// ensure that the response contains the worker name since a future is returned from the submit
			assertEquals("we expect the worker name in the Callback response Message", EXPECTED, workerName);
			
	    	// this send the message that this worker is no longer needed
			session.send(workerName, Control.Stop);
		} finally {
			session.close();
		}
    }
	
	@Test
	public void testCreateNameFromLabel_spacesToUnderscore() throws Exception {
		
		// obtain an instance of the session
		Service  session     = Service.open();
		
		try {
			final String RAW_LABEL = "a b c";
			final String EXPECT    = "a_b_c-1";
			
			String result    = session.createNameFromLabel(RAW_LABEL);
			
			assertEquals("we expect that a label spaces are transposed to underscores '_'", EXPECT, result);
		} finally {
			session.close();
		}
	}
	
	
	@Test
	public void testCreateNameFromLabel_initialSuffix() throws Exception {
		
		// obtain an instance of the session
		Service  session     = Service.open();
		
		try {
			final String RAW_LABEL = "abc";
			final String EXPECT    = "abc-1";
			
			String result    = session.createNameFromLabel(RAW_LABEL);
			
			assertEquals("we expect that a label is ensured unique with a suffix", EXPECT, result);
		} finally {
			session.close(); // asdf not closed
		}
	}
	
	@Test
	public void testCreateNameFromLabel_secondaryInitialSuffix() throws Exception {
		
		// obtain an instance of the session
		Service  session      = Service.open();
		
		try {
			final String RAW_LABEL1 = "qrs";
			final String EXPECT1    = "qrs-1";
			
			final String RAW_LABEL2 = "xyz";
			final String EXPECT2    = "xyz-1";
			
			String result1    = session.createNameFromLabel(RAW_LABEL1);
			String result2    = session.createNameFromLabel(RAW_LABEL2);
			
			assertEquals(EXPECT1, result1);
			assertEquals("we expect that each new label has its own suffix count", EXPECT2, result2);
		} finally {
			session.close();
		}
	}
	
	@Test
	public void testCreateNameFromLabel_secondarySuffix_ensureUniqueness() throws Exception {
		
		// obtain an instance of the session
		Service  session      = Service.open();
		
		try {
			final String RAW_LABEL = "www";
			final String EXPECT1    = "www-1";
			final String EXPECT2    = "www-2";
			final String EXPECT3    = "www-3";
			
			String result1    = session.createNameFromLabel(RAW_LABEL);
			String result2    = session.createNameFromLabel(RAW_LABEL);
			String result3    = session.createNameFromLabel(RAW_LABEL);
			
			assertEquals("we always expect the first suffix to be '-1' ", EXPECT1, result1);
			assertEquals("we expect that each subsequent name request for the same label has an incremented suffix - should be '-2' ",
					EXPECT2, result2);
			assertEquals("we expect that each subsequent name request for the same label has an incremented suffix - should be '-3' ",
					EXPECT3, result3);
		} finally {
			session.close();
		}
	}
	
	@Test
	public void testcreateAddWorkerMessage_simpleFactoryMethod_yaRight_ItsNeverSimple_soWeTest() throws Exception {
		
		// obtain an instance of the session
		Service  session     = Service.open();
		
		try {
			final String RAW_LABEL = "aaa";
			final String EXPECT    = "aaa-1";
			
			final DataPipe pipe    = new DataPipe(null, null);
			
			Worker worker          = new PipeWorker(pipe);
			AddWorkerMessage msg   = session.createAddWorkerMessage(RAW_LABEL, worker);
					
			assertEquals("we expect that an Add Worker Message name be related to the given label", EXPECT, msg.getName());
	
			// reflection access is a bit wonky - this is way many of my classes are package access
			// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
			Object workerPipe      = TestUtils.reflectValue(msg.getWorker(), "pipe");
			
			assertEquals("we expect that an Add Worker Message pipe be the given pipe", pipe, workerPipe);
		} finally {
			session.close();
		}
	}
	
	@Test
	public void testWrapCallback_onCompleteWrappingTest() throws Throwable {
		
		// set up the test responses
		final boolean[] onCompleteCalled_callback = new boolean[1];
		onCompleteCalled_callback[0] = false;
		final boolean[] onCompleteCalled_future   = new boolean[1];
		onCompleteCalled_future[0] = false;
		@SuppressWarnings("unchecked") // generic array
		final OnComplete<Object>[] onCompleteRef  = new OnComplete[1];
		final ExecutionContext[]     contextRef   = new ExecutionContext[1];

		
		// a test callback that sets the result so we know it was called
		Callback callback      = new Callback() {
			@Override
			public void onComplete(Throwable t, Message response) {
				onCompleteCalled_callback[0] = true;
			}
		};
		
		// A test future holder to be assigned the test callback
		// only the onComplete is implemented below as it is under test here
		Future<Object> future  = new Future<Object>() {
			
			// need to cast because of the <U>
			@SuppressWarnings("unchecked")
			@Override
			public <U> void onComplete(Function1<Try<Object>, U> onComplete,
					ExecutionContext context) {
				onCompleteCalled_future[0] = true;
				onCompleteRef[0] = (OnComplete<Object>)onComplete;
				contextRef[0]    = context;
			}
			
			@Override public Awaitable<Object> ready(Duration arg0, CanAwait arg1) throws TimeoutException, InterruptedException {return null;}
			@Override public Object result(Duration arg0, CanAwait arg1) throws Exception {return null;}
			@Override public <U> Future<Object> andThen(PartialFunction<Try<Object>, U> arg0, ExecutionContext arg1) {return null;}
			@Override public <S> Future<S> collect(PartialFunction<Object, S> arg0, ExecutionContext arg1) {return null;}
			@Override public Future<Throwable> failed() {return null;}
			@Override public <U> Future<U> fallbackTo(Future<U> arg0) {return null;}
			@Override public Future<Object> filter(Function1<Object, Object> arg0, ExecutionContext arg1) {return null;}
			@Override public <S> Future<S> flatMap(Function1<Object, Future<S>> arg0, ExecutionContext arg1) {return null;}
			@Override public <U> void foreach(Function1<Object, U> arg0, ExecutionContext arg1) {}
			@Override public boolean isCompleted() {return false;}
			@Override public <S> Future<S> map(Function1<Object, S> arg0, ExecutionContext arg1) {return null;}
			@Override public <S> Future<S> mapTo(ClassTag<S> arg0) {return null;}
			@Override public <U> void onFailure(PartialFunction<Throwable, U> arg0, ExecutionContext arg1) {}
			@Override public <U> void onSuccess(PartialFunction<Object, U> arg0, ExecutionContext arg1) {}
			@Override public <U> Future<U> recover(PartialFunction<Throwable, U> arg0, ExecutionContext arg1) {return null;}
			@Override public <U> Future<U> recoverWith(PartialFunction<Throwable, Future<U>> arg0, ExecutionContext arg1) {return null;}
			@Override public <S> Future<S> transform(Function1<Object, S> arg0, Function1<Throwable, Throwable> arg1, ExecutionContext arg2) {return null;}
			@Override public Option<Try<Object>> value() {return null;}
			@Override public Future<Object> withFilter(Function1<Object, Object> arg0, ExecutionContext arg1) {return null;}
			@Override public <U> Future<Tuple2<Object, U>> zip(Future<U> arg0) {return null;}
		};
		
		
		// obtain an instance of the session
		Service  session     = Service.open();

		try {
			Service.wrapCallback(future, session.workerPool.dispatcher(), callback);
	
			assertTrue("Callback should have been attached to the Future", onCompleteCalled_future[0]);
			
			assertTrue("An OnComplete function wrapper should have been passed into the OnComplete Future attachment", null != onCompleteRef[0]);
			assertTrue("An AKKA context should have been passed into the OnComplete Future attachment", null != contextRef[0]);
			
			assertFalse("OnComplete should NOT have been called yet", onCompleteCalled_callback[0]);
			onCompleteRef[0].onComplete(null, null);
			// this tests that the Callback instance has been wrapped
			assertTrue("OnComplete should HAVE been called", onCompleteCalled_callback[0]);
		} finally {
			session.close();
		}
	}
	
	// TODO should test the SCManager.shutdown method; however, it would invalidate other tests at this time. Need to isolate each test.
}
