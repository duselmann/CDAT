package gov.cida.cdat.service;


import gov.cida.cdat.control.Status;
import gov.cida.cdat.control.Time;

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
	final Map<String,Map<Status,Long>>   history;
	
	public Registry() {
		// when objects get GC'ed they will not be held in this registry
		workers = new WeakHashMap<String, WeakReference<ActorRef>>();
		stati   = new HashMap<String, Status>();
		history = new HashMap<String, Map<Status,Long>>(){
			private static final long serialVersionUID = 1L;

			@Override
			public String toString() {
				StringBuilder buff = new StringBuilder().append("history::");
				for (String name : keySet()) {
					buff.append(name).append(" :\n");
					for (Status stat : Status.values()) {
						Long time = get(name).get(stat);
						buff.append('\t').append(stat.toString())
							.append(" - ").append(time==null?"":time.toString())
							.append('\n');
					}
					if (null != get(name).get(Status.isDone)) {
						long runtime = get(name).get(Status.isDone)-get(name).get(Status.isStarted);
						buff.append('\t').append("runtime - ").append(runtime).append('\n');
					}
					if (null != get(name).get(Status.isDisposed)) {
						long lifespan = get(name).get(Status.isDisposed)-get(name).get(Status.isNew);
						buff.append('\t').append("lifespan - ").append(lifespan).append('\n');
					}
				}
				
				return buff.toString();
			}
		};
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
		history.put(name, new HashMap<Status,Long>());
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
				return; // we only accept after the current status, closer to dispose
			}
		}
		
		stati.remove(name);
		stati.put(name, newStatus);
		history.get(name).put(newStatus, Time.now());
		
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
	
	public Map<Status,Long> getHistory(String name) {
		Map<Status, Long> workerHistory = new HashMap<Status, Long>();
		
		if (history.get(name) != null) {
			workerHistory.putAll( history.get(name) );
		}
		return workerHistory;
	}
}
