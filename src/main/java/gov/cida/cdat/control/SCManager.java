package gov.cida.cdat.control;

import gov.cida.cdat.io.stream.PipeStream;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;


public class SCManager { // TODO better name
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	static final SCManager instance;
	static {
		instance = new SCManager();
	}
	public static SCManager get() {
		return instance;
	}
	
		
	final ActorSystem threadPool;
	final Registry actors;

	
	private ActorSystem threadPool() {
		return threadPool;
	}

	
	private SCManager() {
		actors = new Registry();
        // Create the 'CDAT' actor system
        threadPool = ActorSystem.create("CDAT"); // TODO doc structure
        // TODO test that contain is same instance or new instance - we need know
        // TODO session system for mutliple users
	}
	
	public String addService(String serviceName, PipeStream pipe) { // TODO QueryWorker name change
        // Create the service actor
        final ActorRef actor = threadPool().actorOf(Props.create(
        		gov.cida.cdat.service.combined.Service.class, pipe), serviceName);
        
        actors.put(serviceName, actor); // TODO dispose of actor?
        
		return serviceName;
	}
	
	
	private Future<Object> send(String serviceName, Object msg) {
        // send a message
		ActorRef actor = actors.get(serviceName);		
		if (actor == null) {
			return null; // TODO decide if this is appropriate and sufficient
		}
		
	    return Patterns.ask(actor, msg, 1000); // TODO config timeout , doc the logic
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
	
	// TODO examples
	
	// TODO impl start/stop fail return true/false

	// TODO possible blocking and control on control
	
	public void shutdown() {
		final ActorSystem system = threadPool();
//		threadPool().shutdown();
		system.scheduler().scheduleOnce( FiniteDuration.create(20, "seconds"),
			new Runnable() {
				@Override
				public void run() {
				    logger.info("shutdown");
				    system.shutdown();
				}
		}, system.dispatcher());
		
	}

	// TODO Future<Message>
	public Future<Object> send(String serviceName, Control ctrl, final Callback onComplete) {
		Future<Object> response = send(serviceName, ctrl);
		OnComplete<Object> wrapper = new OnComplete<Object>() {
			public void onComplete(Throwable t, Object repsonse) throws Throwable {
				onComplete.onComplete(t, (Message)repsonse);
			}
		};
	    response.onComplete(wrapper, threadPool().dispatcher());
		return response;
	}
}
