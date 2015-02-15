package gov.cida.cdat.service;


import gov.cida.cdat.control.Status;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import akka.actor.ActorRef;


public class Registry {
	
	final Map<String,WeakReference<ActorRef>> workers;
	final Map<String,Status>   stati;
	
	public Registry() {
		// when objects get GC'ed they will not be held in this registry
		workers = new WeakHashMap<String, WeakReference<ActorRef>>();
		stati   = new HashMap<String, Status>();
	}
	
	public ActorRef get(String name) {
		if ( ! workers.containsKey(name) ) {
			return null;
		}
		if (workers.get(name) == null) {
			return null;
		}
		if (workers.get(name).get() == null) {
			workers.remove(name);
			return null;
		}
		return workers.get(name).get();
	}
	
	public String put(String name, ActorRef actor) {
		workers.put(name, new WeakReference<ActorRef>(actor));
		setStatus(name, Status.isNew);

		return name;
	}
	
	public void remove(String name) {
		workers.remove(name);
	}

	public void setStatus(String name, Status newStatus) {
		if (stati.get(name) != null) {
			Status current = stati.get(name);
			if (newStatus.ordinal() < current.ordinal()) {
				// we only accept status further towards dispose
				return;
			}
		}
		
		stati.remove(name);
		stati.put(name, newStatus);
		
		// remove not alive workers
//		if ( ! Status.isAlive(newStatus) ) {
//			WeakReference<ActorRef> ref = workers.remove(name);
//			if (ref != null && ref.get() != null) {
//				
//			}
//		}
	}
	
	public Status getStatus(String name) {
		return stati.get(name);
	}
	public boolean isAlive(String name) {
		return Status.isAlive( getStatus(name) );
	}
	
	public Set<String> names() {
		Set<String> names = new HashSet<String>(stati.keySet());
		names.addAll(workers.keySet());
		
		return names;
	}
}
