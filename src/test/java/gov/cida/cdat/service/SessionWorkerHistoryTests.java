package gov.cida.cdat.service;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.exception.CdatException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionWorkerHistoryTests {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	
	private int traceEntry;
	
	@Before
	public void setup() {
		if (logger.isTraceEnabled()) {
			traceEntry=1;
		}		
	}
	
	
	private Message[] getWorkerHostory(SCManager session, String nameA, String nameB) {
		final Message[] messages = new Message[2];
		Message getHistoryA = Message.create(Control.history, nameA);
		session.send(nameA, getHistoryA, new Callback() {
			@Override
			public void onComplete(Throwable t, Message response) {
				messages[0] = response;
			}
		});
		Message getHistoryB = Message.create(Control.history, nameB);
		session.send(nameB, getHistoryB, new Callback() {
			@Override
			public void onComplete(Throwable t, Message response) {
				messages[1] = response;
			}
		});
		
		TestUtils.waitAlittleWhileForResponse(messages);
		
		TestUtils.log(messages[0]);
		TestUtils.log(messages[1]);
		
		return messages;
	}
	
	@Test
	public void testInfoIsNew() {
		SCManager session = SCManager.open();
		
		try {
			Worker workerA = new Worker(){};
			Worker workerB = new Worker(){};
			
			String nameA = session.addWorker("workerA", workerA);
			String nameB = session.addWorker("workerB", workerB);
			
			Message[] history = getWorkerHostory(session, nameA, nameB);
				
			
			assertEquals("Expect workerA history contain only "+Status.isNew,
					 1+traceEntry, history[0].size());
			assertTrue("Expect workerA history contain only "+Status.isNew,
					 history[0].get(Status.isNew) != null);

			assertEquals("Expect workerB history contain only "+Status.isNew,
					 1+traceEntry, history[1].size());
			assertTrue("Expect workerB history contain only "+Status.isNew,
					 history[1].get(Status.isNew) != null);
		} finally {
			session.close();
		}
	}
		

	@Test
	public void testInfoIsDisposed() throws Exception {
		SCManager session = SCManager.open();
		
		try {
			Worker workerA = new Worker(){};
			Worker workerB = new Worker(){};
			
			String nameA = session.addWorker("workerA", workerA);
			String nameB = session.addWorker("workerB", workerB);
						
			session.send(nameA, Control.Start);
			
			Thread.sleep(100);
			
			Message[] history = getWorkerHostory(session, nameA, nameB);
			
			assertEquals("Expect workerA history contain status - isNew,isStarted,isDone,isDispose,lifespan,runtime",
					 6+traceEntry, history[0].size());
			assertTrue("Expect workerA history contain "+Status.isNew,
					 history[0].get(Status.isNew) != null);
			assertTrue("Expect workerA history contain "+Status.isStarted,
					 history[0].get(Status.isStarted) != null);
			assertTrue("Expect workerA history contain "+Status.isDone,
					 history[0].get(Status.isDone) != null);
			assertTrue("Expect workerA history contain "+Status.isDisposed,
					 history[0].get(Status.isDisposed) != null);
			assertTrue("Expect workerA history contain runtime",
					 history[0].get("runtime") != null);
			assertTrue("Expect workerA history contain lifespan",
					 history[0].get("lifespan") != null);

			assertEquals("Expect workerB history contain only "+Status.isNew,
					 1+traceEntry, history[1].size());
			assertTrue("Expect workerB history contain only "+Status.isNew,
					 history[1].get(Status.isNew) != null);
		} finally {
			session.close();
		}
	}
	
	
	@Test
	public void testInfoIsStarted() throws Exception {
		SCManager session = SCManager.open();
		
		try {			
			Worker workerA = new Worker(){
				@Override
				public boolean process() throws CdatException {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return super.process();
				}
			};
			Worker workerB = new Worker(){};
			
			session.setAutoStart(true);
			String nameA = session.addWorker("workerA", workerA);
			session.setAutoStart(false);
			String nameB = session.addWorker("workerB", workerB);
			
			Message[] history = getWorkerHostory(session, nameA, nameB);
			
			assertEquals("Expect workerA history contain status - isNew,isStarted",
					 2+traceEntry, history[0].size());
			assertTrue("Expect workerA history contain "+Status.isNew,
					 history[0].get(Status.isNew) != null);
			assertTrue("Expect workerA history contain "+Status.isStarted,
					 history[0].get(Status.isStarted) != null);

			assertEquals("Expect workerB history contain only "+Status.isNew,
					 1+traceEntry, history[1].size());
			assertTrue("Expect workerB history contain only "+Status.isNew,
					 history[1].get(Status.isNew) != null);
		} finally {
			session.close();
		}
	}}
