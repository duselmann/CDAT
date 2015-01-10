package gov.cida.cdat.control;

import gov.cida.cdat.exception.DupicateNameException;

import java.util.Map;
import java.util.WeakHashMap;

import akka.actor.ActorRef;

public class Registry {
	
	int count; // internal count auto-named entries
	final Map<String,ActorRef> actors;
	
	public Registry() {
		// when refs get cleaned up they will not be held in this registry
		actors = new WeakHashMap<String, ActorRef>();
	}
	
	public ActorRef get(String name) {
		return actors.get(name);
	}
	
	public void put(String name, ActorRef actor) {
		if (actors.containsKey(name)) {
			throw new DupicateNameException("Actor service names must be unique.");
		}
		actors.put(name, actor);
	}

	public String createName(String name) {
		String newName = name + (count++);
		
		while (actors.containsKey(newName)) {
			newName = name + (count++);
		}
		
		return newName;
	}

	public int size() {
		return actors.size();
	}
}
