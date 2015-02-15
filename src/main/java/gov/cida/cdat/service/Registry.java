package gov.cida.cdat.service;


import gov.cida.cdat.control.Status;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import akka.actor.ActorRef;


public class Registry {
	
	final Map<String,WeakReference<ActorRef>> workers;
	final Map<String,String>   stati;
	
	public Registry() {
		// when objects get GC'ed they will not be held in this registry
		workers = new WeakHashMap<String, WeakReference<ActorRef>>();
		stati   = new HashMap<String, String>();
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

	public void setStatus(String name, String status) {
		if (stati.get(name) != null) {
			Status current = Status.valueOf(stati.get(name));
			Status update  = Status.valueOf(status);
			if (update.ordinal() < current.ordinal()) {
				// we only accept status further towards dispose
				return;
			}
		}
		
		stati.remove(name);
		stati.put(name, status);
		
//		// remove finished workers
//		if (Status.isDisposed.equals(status)
//				|| Status.isDone.equals(status)
//				|| Status.isError.equals(status)) {
//			WeakReference<ActorRef> ref = workers.remove(name);
//			if (ref != null && ref.get() != null) {
//				
//			}
//		}
	}
	public void setStatus(String name, Status status) {
		setStatus(name, status.toString());
	}
	public String getStatus(String name) {
		return stati.get(name);
	}
	public boolean isAlive(String name) {
		Status status = Status.valueOf( getStatus(name) );
		
		if (Status.isDone.equals(status)
				||  Status.isDisposed.equals(status)
				||  Status.isError.equals(status) ) {
			// we do not need to check for isAlive because that is not a set-able status
			// isAlive is a status used to request if the worker is alive
			return false;
		}
		return true; // it is more clear in this case to return true explicitly
	}
	
}
