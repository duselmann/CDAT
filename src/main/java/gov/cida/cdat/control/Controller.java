package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.PipeStream;

import java.util.Map;
import java.util.concurrent.Callable;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
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
	
	
	private Future<Message> send(String serviceName, final Message msg) {
        // send a message
		ActorRef actor = actors.get(serviceName);		
		if (actor == null) {
			return null; // TODO decide if this is appropriate and sufficient
		}
		
		
		Future<Message> future = Futures.future(new Callable<Message>() {
			@Override
			public Message call() throws Exception {
				return msg;
			}
		}, context().dispatcher());
		
		Patterns.pipe(future, context().dispatcher()).to(actor);
		
		return future;
	    //return Patterns.ask(actor, msg, 1000);
	}
	
	/**
	 * Enumerated control message
	 * @param serviceName
	 * @param ctrl
	 * @return
	 */
	public Future<Message> send(String serviceName, Control ctrl) {
		Message msg = Message.create(ctrl);
		return send(serviceName, msg);
	}
	/**
	 * Enumerated status message
	 * @param serviceName
	 * @param ctrl
	 * @return
	 */
	public Future<Message> send(String serviceName, Status status) {
		Message msg = Message.create(status);
		return send(serviceName, msg);
	}
	/**
	 * Custom Status or Control message
	 * @param serviceName
	 * @param msg
	 * @return
	 */
	public Future<Message> send(String serviceName, Map<String,String> msg) {
		Message message = Message.create(msg);
		return send(serviceName, message);
	}
	
	
	//TODO this is for the distributed approach if it could work
	public void send(String serviceName, AddWorker msg) {
		ActorRef actor = actors.get(serviceName);		
		if (actor == null) {
			return; // TODO decide if this is appropriate and sufficient
		}

		actor.tell(msg, ActorRef.noSender());
	}
	
	
	public void shutdown() {
		context().shutdown();
	}


	public Future<Message> send(String serviceName, Control ctrl, final Callback onComplete) {
		Future<Message> response = send(serviceName, Message.create(ctrl));
	    response.onComplete(onComplete, context().dispatcher());
		return response;
	}
}
