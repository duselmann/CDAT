package gov.cida.cdat.service;

import static org.junit.Assert.*;
import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.control.Status;
import gov.cida.cdat.control.Time;
import gov.cida.cdat.control.Worker;
import gov.cida.cdat.exception.CdatException;

import org.junit.Test;

public class SessionInfoTests {

	@Test
	public void testInfoIsNew() {
		Service session = Service.open();
		
		try {
			Worker workerA = new Worker(){};
			Worker workerB = new Worker(){};
			
			String nameA = session.addWorker("workerA", workerA);
			String nameB = session.addWorker("workerB", workerB);
			
			final Message[] message = new Message[1];
			Message getInfo = Message.create(Control.info, Service.SESSION);
			session.send(Service.SESSION, getInfo, new Callback() {
				@Override
				public void onComplete(Throwable t, Message response) {
					message[0] = response;
					TestUtils.log(response);
				}
			});
			
			Time.waitForResponse(message,100);
			
			TestUtils.log(message[0]);
			
			assertEquals("Expect info on workerA to be "+Status.isNew,
					Status.isNew.toString(), message[0].get(nameA));
			assertEquals("Expect info on workerB to be "+Status.isNew,
					Status.isNew.toString(), message[0].get(nameB));
		} finally {
			session.close();
		}
	}

	@Test
	public void testInfoIsDisposed() throws Exception {
		Service session = Service.open();
		
		try {
			Worker workerA = new Worker(){};
			Worker workerB = new Worker(){};
			
			String nameA = session.addWorker("workerA", workerA);
			String nameB = session.addWorker("workerB", workerB);
			
			final Message[] message = new Message[1];
			Message getInfo = Message.create(Control.info, Service.SESSION);
			
			session.send(nameA, Control.Start);
			session.send(nameB, Control.Start);
			
			Thread.sleep(100);
			
			session.send(Service.SESSION, getInfo, new Callback() {
				@Override
				public void onComplete(Throwable t, Message response) {
					message[0] = response;
					TestUtils.log(response);
				}
			});
			
			Time.waitForResponse(message,100);
			
			TestUtils.log(message[0]);
			
			assertEquals("Expect info on workerA to be "+Status.isDisposed,
					Status.isDisposed.toString(), message[0].get(nameA));
			assertEquals("Expect info on workerB to be "+Status.isDisposed,
					Status.isDisposed.toString(), message[0].get(nameB));
		} finally {
			session.close();
		}
	}
	
	
	@Test
	public void testInfoIsStarted() throws Exception {
		Service session = Service.open();
		
		try {
			session.setAutoStart(true);
			
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
			Worker workerB = new Worker(){
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
			
			String nameA = session.addWorker("workerA", workerA);
			String nameB = session.addWorker("workerB", workerB);
			
			final Message[] message = new Message[1];
			Message getInfo = Message.create(Control.info, Service.SESSION);
			
			
			session.send(Service.SESSION, getInfo, new Callback() {
				@Override
				public void onComplete(Throwable t, Message response) {
					message[0] = response;
					TestUtils.log(response);
				}
			});
			
			Time.waitForResponse(message,100);
			
			TestUtils.log(message[0]);
			
			assertEquals("Expect info on workerA to be "+Status.isStarted,
					Status.isStarted.toString(), message[0].get(nameA));
			assertEquals("Expect info on workerB to be "+Status.isStarted,
					Status.isStarted.toString(), message[0].get(nameB));
		} finally {
			session.close();
		}
	}}
