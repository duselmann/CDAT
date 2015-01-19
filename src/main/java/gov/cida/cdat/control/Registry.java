package gov.cida.cdat.control;


import java.util.Map;
import java.util.WeakHashMap;

import akka.actor.ActorRef;


public class Registry {
	
	final Map<String,ActorRef> actors;
	
	public Registry() {
		// when objects get GC'ed they will not be held in this registry
		actors = new WeakHashMap<String, ActorRef>();
	}
	
	public ActorRef get(String name) {
		return actors.get(name);
	}
	
	public String put(String name, ActorRef actor) {
		String uniqueName = createUniqueName(name);
		actors.put(uniqueName, actor);
		return name;
	}

	// protected for testing access
	protected String createUniqueName(String name) {
		int count = 0; // internal count auto-named entries
		
		String uniqueName = name;
		
		while ( actors.containsKey(uniqueName) ) {
			uniqueName = name + (count++);
		}
		
		return uniqueName;
	}

//	public int size() {
//		return actors.size();
//	}
}
