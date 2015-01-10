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
		
		control.sendControl(serviceName, producer);
		control.sendControl(serviceName, transform);
		control.sendControl(serviceName, consumer);
		
		control.sendControl(serviceName, Message.create("Message", "Test"));
		control.sendControl(serviceName, Message.create(Control.Start));

		Thread.sleep(1000);
		control.sendControl(serviceName, Message.create(Control.Stop));
		
		Thread.sleep(2000);
		control.shutdown();
	}
	
	

}
