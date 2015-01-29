package gov.cida.cdat.message;

import gov.cida.cdat.service.Worker;

import java.io.Serializable;

/**
 * An internal message class for transmitting the request to
 * start a worker process.
 * 
 * NOTE: It would be nice if there was a friend access mode
 * in order to remove the public. 
 * 
 * TODO This class might have to move
 * 
 * @author duselman
 */
public class AddWorkerMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The unique name for this worker. It is internal an
	 * internal call so it is guaranteed to be unique.
	 * Any changes to the framework must take this into account.
	 */
	private final String name;
	
	/**
	 * The worker to run on a delegate 'thread'
	 */
	private final Worker delegate;

	/**
	 * This worker should automatically start on submit
	 */
	private boolean autoStart;
	
	
	private AddWorkerMessage(String name, Worker worker) {
		this(name,worker,false);
	}
	private AddWorkerMessage(String name, Worker worker, boolean autoStart) {
		this.name      = name;
		this.delegate  = worker;
		this.autoStart = autoStart;
	}
	

	public static AddWorkerMessage create(String workerName, final Worker worker) {
		AddWorkerMessage addWorker = new AddWorkerMessage(workerName, worker);
		return addWorker;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public Worker getWorker() {
		return delegate;
	}


	public boolean isAutoStart() {
		return autoStart;
	}
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}
	
}
