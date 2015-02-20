package gov.cida.cdat.control;

import gov.cida.cdat.service.Service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This is the shutdown manager to ensure that the container cDAT manager threads
 * also eventually shutdown. The utilization of this feature requires configuration of
 * the shutdown wait time should the default be in sufficient.
 * </p>
 * <p>It is required that the below listener be added to the web.xml for the application
 * making use of cDAT. It is also required that all login jars be in the container lib dir
 * rather than the webapp dir. Otherwise, the classes are not found during shutdown.
 * </p>
 * &lt;listener><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;listener-class>gov.cida.cdat.controlShutdownListener&lt;/listener-class><br>
 * &lt;/listener><br>
 *
 * @author duselmann
 * @see gov.cida.cdat.control.SCManager.shutdown()
 */
public class ShutdownListener implements ServletContextListener {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		logger.info("cDAT is listening for container shutdown.");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("Shutting down cDAT with container shutdown.");
		Service.shutdown();
		try {
			// TODO make configurable same value as the SCManager value plus 5 sec
			Thread.sleep(Time.SECOND.asMS()+100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
