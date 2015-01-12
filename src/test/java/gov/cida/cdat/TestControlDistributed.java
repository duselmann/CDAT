package gov.cida.cdat;


import gov.cida.cdat.control.AddWorker;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Controller;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.service.distributed.Worker;


public class TestControlDistributed {

	public static void main(String[] args) throws Exception {
		Controller control = Controller.get();
		
		String serviceName = control.addService();

		AddWorker producer  = new AddWorker(AddWorker.Type.Producer,  new Worker("Producer") {});
		AddWorker transform = new AddWorker(AddWorker.Type.Transform, new Worker("Transform") {});
		AddWorker consumer  = new AddWorker(AddWorker.Type.Consumer,  new Worker("Consumer") {});
		
		control.send(serviceName, producer);
		control.send(serviceName, transform);
		control.send(serviceName, consumer);
		
		control.send(serviceName, Message.create("Message", "Test"));
		control.send(serviceName, Message.create(Control.Start));

		Thread.sleep(1000);
		control.send(serviceName, Message.create(Control.Stop));
		
		Thread.sleep(2000);
		control.shutdown();
	}
	
	

}
