package gov.cida.cdat.service;


import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.PipeWorker;
import gov.cida.cdat.service.Worker;
import static org.junit.Assert.*;

import org.junit.Test;


public class DelegatorStatusTests {

	
	
	@Test
	public void testStatusLifeCycle() throws Exception {
		SCManager session = SCManager.open();

		try {
			final String workerName = session.addWorker("statusTests",  new Worker() {
				@Override
				public boolean process() throws CdatException {
					return true; // tell the system that there is more to process
				}
			});
	
			Message response;
			
			response = session.send(workerName, Status.isNew);
			TestUtils.log("send status isNew - expect true", response);
			assertTrue("Expect the delegate respond with isNew message", 
					response.contains(Status.isNew));
			assertTrue("Expect the delegate be NEW", 
					response.get(Status.isNew).equals("true"));
			
			response = session.send(workerName, Status.CurrentStatus);
			TestUtils.log("send status CurrentStatus - expect isNew", response);
			assertTrue("Expect the delegate respond with CurrentStatus message", 
					response.contains(Status.CurrentStatus));
			assertTrue("Expect the delegate be isNew", 
					response.get(Status.CurrentStatus).equals("isNew"));
			
			response = session.send(workerName, Status.isStarted);
			TestUtils.log("send status isStarted - expect false", response);
			assertTrue("Expect the delegate respond with isStarted message", 
					response.contains(Status.isStarted));
			assertTrue("Expect the delegate NOT to have been started yet", 
					response.get(Status.isStarted).equals("false"));
	
			response = session.send(workerName, Status.isDone);
			TestUtils.log("send status isDone - expect false", response);
			assertTrue("Expect the delegate respond with isDone message", 
					response.contains(Status.isDone));
			assertTrue("Expect the delegate NOT to have been completed yet", 
					response.get(Status.isDone).equals("false"));
	
			response = session.send(workerName, Status.isAlive);
			TestUtils.log("send status isAlive - expect true", response);
			assertTrue("Expect the delegate respond with isAlive message", 
					response.contains(Status.isAlive));
			assertTrue("Expect the delegate to be alive", 
					response.get(Status.isAlive).equals("true"));
	
			
			session.send(workerName, Control.Start);
			
			
			response = session.send(workerName, Status.isNew);
			TestUtils.log("send status isNew - expect false after start", response);
			assertTrue("Expect the delegate respond with isNew message", 
					response.contains(Status.isNew));
			assertTrue("Expect the delegate be NEW", 
					response.get(Status.isNew).equals("false"));
			
			response = session.send(workerName, Status.CurrentStatus);
			TestUtils.log("send status CurrentStatus - expect isStarted", response);
			assertTrue("Expect the delegate respond with CurrentStatus message", 
					response.contains(Status.CurrentStatus));
			assertTrue("Expect the delegate be isStarted", 
					response.get(Status.CurrentStatus).equals("isStarted"));
			
			response = session.send(workerName, Status.isStarted);
			TestUtils.log("send status isStarted - expect true", response);
			assertTrue("Expect the delegate respond with isStarted message", 
					response.contains(Status.isStarted));
			assertTrue("Expect the delegate to be started", 
					response.get(Status.isStarted).equals("true"));
			
			response = session.send(workerName, Status.isDone);
			TestUtils.log("send status isDone - expect false", response);
			assertTrue("Expect the delegate respond with isDone message", 
					response.contains(Status.isDone));
			assertTrue("Expect the delegate NOT to have been completed yet", 
					response.get(Status.isDone).equals("false"));
	
			response = session.send(workerName, Status.isAlive);
			TestUtils.log("send status isAlive - expect true", response);
			assertTrue("Expect the delegate respond with isAlive message", 
					response.contains(Status.isAlive));
			assertTrue("Expect the delegate to be alive", 
					response.get(Status.isAlive).equals("true"));
	
			final Message[] stopped = new Message[1];
			session.send(workerName, Control.Stop, new Callback() {
				@Override
				public void onComplete(Throwable t, Message response) {
					stopped[0] = response;
				}
			});
			TestUtils.waitAlittleWhileForResponse(stopped);
			
			response = session.send(workerName, Status.isDone);
			TestUtils.log("send status isDone - expect true", response);
			assertTrue("Expect the delegate respond with isDone message", 
					response.contains(Status.isDone));
			assertTrue("Expect the delegate to have been completed", 
					response.get(Status.isDone).equals("true"));
	
			response = session.send(workerName, Status.isAlive);
			TestUtils.log("send status isAlive - expect false", response);
			assertTrue("Expect the delegate respond with isAlive message", 
					response.contains(Status.isAlive));
			assertTrue("Expect the delegate NOT to be alive", 
					response.get(Status.isAlive).equals("false"));
			
			response = session.send(workerName, Status.isDisposed);
			TestUtils.log("send status isDone - expect false", response);
			assertTrue("Expect the delegate respond with isDisposed message", 
					response.contains(Status.isDisposed));
			assertTrue("Expect the delegate to have been completed", 
					response.get(Status.isDisposed).equals("true"));
		
		} finally {
			session.close();
		}
	}
	
	
	@Test
	public void testMessagesSentToWorkerSimpler() throws Exception {
		SCManager session = SCManager.open();

		try {
			final Message[] workerMessage = new Message[1];
			Worker worker = new PipeWorker(null) {
				@Override
				public Message onReceive(Message msg) {
					workerMessage[0] = msg;
					return null;
				}
			};
			
			final String workerName = session.addWorker("onReceiveTest", worker);
			
			System.out.println("send custom message to worker");
			Message testMsg = Message.create("Message", "Test");
			session.send(workerName, testMsg);
			TestUtils.waitAlittleWhileForResponse(workerMessage);
			assertTrue("Expect the worker to receive messages", workerMessage[0].contains("Message"));
			assertTrue("Expect the worker to receive messages", workerMessage[0].get("Message").equals("Test"));
			
			session.send(workerName, Control.Stop);
		} finally {
			session.close(); // SESSION-19 failed to close and received null on session(string) call
		}
	}
	
	
	@Test
	public void testDoubleStartShouldError() throws Exception {
		SCManager session = SCManager.open();

		session.setAutoStart(true);
		
		try {
			final int[] callCount = new int[1];
			final Boolean[] beginCalled = new Boolean[1];
			final String workerName = session.addWorker("autoStartTest",  new Worker() {
				
				@Override
				public void begin() throws CdatException {
					beginCalled[0]=true;
					callCount[0]++;
				}
			});
			session.send(workerName, Control.Start);
			session.send(workerName, Control.Start);
	
			TestUtils.waitAlittleWhileForResponse(beginCalled);
			Thread.sleep(500);
			
			assertTrue("begin should be called", beginCalled[0]);
			assertEquals("begin should be called once", 1, callCount[0]);
			
			session.send(workerName, Control.Stop);
			
		} finally {
			session.close(); // this resets the autoStart
		}
	}
}
