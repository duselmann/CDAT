package gov.cida.cdat;


import gov.cida.cdat.control.AddWorker;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.Controller;
import gov.cida.cdat.control.Message;
import gov.cida.cdat.services.Worker;


public class TestControl {

	public static void main(String[] args) throws Exception {
		Controller control = Controller.get();
		
		String serviceName = control.addService();

		AddWorker connection  = new AddWorker(AddWorker.Type.Producer, new Worker() {});
		AddWorker transformer = new AddWorker(AddWorker.Type.Transform, new Worker() {});
		AddWorker reciever    = new AddWorker(AddWorker.Type.Consumer, new Worker() {});
		
		control.sendControl(serviceName, connection);
		control.sendControl(serviceName, transformer);
		control.sendControl(serviceName, reciever);
		
		control.sendControl(serviceName, Message.create("Message", "Test"));
		control.sendControl(serviceName, Message.create(Control.Start));

		Thread.sleep(1000);
		control.sendControl(serviceName, Message.create(Control.Stop));
		
		Thread.sleep(2000);
		control.shutdown();
	}
	
	

}
