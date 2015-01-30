package gov.cida.cdat.service;


import gov.cida.cdat.control.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import akka.actor.ActorRef;


public class Registry {
	
	final Map<String,ActorRef> workers;
	final Map<String,String>   stati;
	
	public Registry() {
		// when objects get GC'ed they will not be held in this registry
		workers = new WeakHashMap<String, ActorRef>();
		stati   = new HashMap<String, String>();
	}
	
	public ActorRef get(String name) {
		return workers.get(name);
	}
	
	public String put(String name, ActorRef actor) {
		String uniqueName = createUniqueName(name);
		workers.put(uniqueName, actor);
		setStatus(name, Status.isNew);

		return name;
	}

	// package access for testing
	String createUniqueName(String name) {
		int count = 0; // internal count auto-named entries
		
		String uniqueName = name;
		
		while ( workers.containsKey(uniqueName) || stati.containsKey(uniqueName) ) {
			uniqueName = name + (count++);
		}
		
		return uniqueName;
	}

	
	public void setStatus(String name, String status) {
		stati.remove(name);
		stati.put(name, status);
	}
	public void setStatus(String name, Status status) {
		if ( Status.isDone.equals(status) ) {
			workers.remove(name);
		}
		setStatus(name, status.toString());
	}

}
