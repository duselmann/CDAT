package gov.cida.cdat.control;

import gov.cida.cdat.service.distributed.Worker;

import java.io.Serializable;

public class AddWorker implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum Type {
		Producer,Transform,Consumer
	}
	
	
	final Type   type;
	final Worker worker;
	
	
	public AddWorker(Type type, Worker worker) {
		this.type   = type;
		this.worker = worker;
	}


	public Type getType() {
		return type;
	}


	public Worker getWorker() {
		return worker;
	}
	
}
