package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.PipeStream;

import java.util.Map;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;


public class Controller {
	static final Controller instance;
	static {
		instance = new Controller();
	}
	public static Controller get() {
		return instance;
	}
	
		
	final ActorSystem context;
	final Registry actors;

	
	private ActorSystem context() {
		return context;
	}

	
	private Controller() {
		actors = new Registry();
        // Create the 'CDAT' actor system
        context = ActorSystem.create("CDAT");
	}
	
	public String addService() {
		return addService( actors.createName("service") );
	}	
	public String addService(String serviceName) {
        // Create the service actor
        final ActorRef actor = context().actorOf(Props.create(
        		gov.cida.cdat.service.distributed.Service.class, new Object[0]), serviceName);
        
        actors.put(serviceName, actor);
        
		return serviceName;
	}
	public String addService(String serviceName, PipeStream pipe) {
        // Create the service actor
        final ActorRef actor = context().actorOf(Props.create(
        		gov.cida.cdat.service.combined.Service.class, pipe), serviceName);
        
        actors.put(serviceName, actor);
        
		return serviceName;
	}
	
	
	private Future<Object> send(String serviceName, Object msg) {
        // send a message
		ActorRef actor = actors.get(serviceName);		
		if (actor == null) {
			return null; // TODO decide if this is appropriate and sufficient
		}
		
	    return Patterns.ask(actor, msg, 1000);
	}
	
	/**
	 * Enumerated control message
	 * @param serviceName
	 * @param ctrl
	 * @return
	 */
	public Future<Object> send(String serviceName, Control ctrl) {
		Object msg = Message.create(ctrl);
		return send(serviceName, msg);
	}
	/**
	 * Enumerated status message
	 * @param serviceName
	 * @param ctrl
	 * @return
	 */
	public Future<Object> send(String serviceName, Status status) {
		Object msg = Message.create(status);
		return send(serviceName, msg);
	}
	/**
	 * Custom Status or Control message
	 * @param serviceName
	 * @param msg
	 * @return
	 */
	public Future<Object> send(String serviceName, Map<String,String> msg) {
		return send(serviceName, (Object)msg);
	}
	
	
	//TODO this is for the distributed approach if it could work
	public Future<Object> send(String serviceName, AddWorker msg) {
		return send(serviceName, (Object)msg);
	}
	
	
	public void shutdown() {
		context().shutdown();
	}


	public Future<Object> send(String serviceName, Control ctrl, final Callback onComplete) {
		Future<Object> response = send(serviceName, ctrl);
		OnComplete<Object> wrapper = new OnComplete<Object>() {
			public void onComplete(Throwable t, Object repsonse) throws Throwable {
				onComplete.onComplete(t, (Message)repsonse);
			}
		};
	    response.onComplete(wrapper, context().dispatcher());
		return response;
	}
}
