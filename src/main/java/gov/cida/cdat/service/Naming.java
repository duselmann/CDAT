package gov.cida.cdat.service;

import gov.cida.cdat.message.Message;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

public class Naming extends UntypedActor {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	
	public static final String WORKER_NAME = "worker.name";
	public static final String ADD_NAME    = "add.name";
	
	private final Set<String> names;

	// TODO should this be a singleton?
	public Naming() {
		names = new HashSet<String>();
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		logger.trace("naming received message {}", msg);
		// internal message
		if (msg instanceof String) {
			createNewName((String)msg);
		} else if (msg instanceof Message) { // standard framework message
			addName((Message)msg);
		} else {
			unhandled(msg);
		}
	}
	
	
	// package access for testing
	void addName(Message msg) {
		logger.trace("naming adding name {}", msg);
		
		String name = msg.get(ADD_NAME);
		if (name != null) {
			names.add(name);
		}
	}
	

	void createNewName(String label) {
		logger.trace("naming creating name from {}", label);
		
		String name = createUniqueName(label);
		
		logger.trace("naming created {} from label {}", name, label);
		
		sender().tell( Message.create(WORKER_NAME,name) , self());
	}
	
	
	// package access for testing
	String createUniqueName(String label) {
		int count = 1; // internal count auto-named entries
		
		// spaces are not allowed in AKKA
		label = label.replaceAll(" ", "_");
		
		String uniqueName = label +"-"+ (count++);
		
		while ( names.contains(uniqueName) ) {
			uniqueName = label +"-"+ (count++);
		}
		
		names.add(uniqueName);
		return uniqueName;
	}
}
