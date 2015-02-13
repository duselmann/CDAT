package gov.cida.cdat.db;

import gov.cida.cdat.TestUtils;
import gov.cida.cdat.control.Callback;
import gov.cida.cdat.control.Control;
import gov.cida.cdat.control.SCManager;
import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.container.SimpleStreamContainer;
import gov.cida.cdat.io.container.TransformStreamContainer;
import gov.cida.cdat.message.Message;
import gov.cida.cdat.service.Worker;
import gov.cida.cdat.transform.Transformer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class DBTesting {
	
	
	static Connection conn;

	
	@BeforeClass
	public static void setUp() throws SQLException, ClassNotFoundException {
		try {
			//Creating testDB database
			System.out.println("Starting in-memory database for unit tests.");
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			conn = DriverManager.getConnection("jdbc:derby:memory:TestingDB;create=true");

			System.out.println("Inserting records into 'people' table for tests.");
			Statement st = conn.createStatement();
			st.executeUpdate("create table people(name varchar(50), address varchar(100), phone varchar(12))");
			st.executeUpdate("insert into  people values('Jane','123 1st St. #4', '555-123-1234')");
			st.executeUpdate("insert into  people values('John','999 2nd Dr. #9', '999-999-9999')");
			st.executeUpdate("insert into  people values('Joe', '777 3rd St. #4', '777-867-5309')");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testSelectStream_outside_SCManager() throws Exception {
		System.out.println("testSelectStream");
		
		// consumer
		ByteArrayOutputStream target = new ByteArrayOutputStream(4096*4);
		SimpleStreamContainer<OutputStream> targetContainer = new SimpleStreamContainer<OutputStream>(target);
		
		// Transformer
		Transformer<Pojo> transform = new PojoTransformer(",");
		TransformStreamContainer<Pojo> transContainer = new TransformStreamContainer<Pojo>(transform, targetContainer);

		// Producer
		final PojoDbReader producer = new PojoDbReader(conn, transContainer);
		producer.open();
		producer.read();
		producer.close();
		
		String csv = new String(target.toByteArray());
		System.out.println( csv );
		
		Assert.assertTrue("Header expexted", csv.startsWith("\"name\",\"address\",\"phone\""));
		Assert.assertTrue("Only one header", 0>csv.indexOf("\"name\",\"address\",\"phone\"", 20));
		Assert.assertTrue("Expect to find John", csv.contains("John"));
		Assert.assertTrue("Expect to find Jane", csv.contains("Jane"));
	}

	
	@Test
	public void testSelectStream_within_SCManager() throws Exception {
		System.out.println("testSelectStream");
		
		// consumer - the 'L' in ETL
		final ByteArrayOutputStream target = new ByteArrayOutputStream(4096*4);
		SimpleStreamContainer<OutputStream> targetContainer = new SimpleStreamContainer<OutputStream>(target);
		
		// Transformer
		Transformer<Pojo> transform = new PojoTransformer(",");
		TransformStreamContainer<Pojo> transContainer = new TransformStreamContainer<Pojo>(transform, targetContainer);

		// Producer
		final PojoDbReader producer = new PojoDbReader(conn, transContainer);
		producer.setFetchSize(1);

		
		SCManager session = SCManager.open();
		Worker    worker  = new Worker() {
			DbReader<Pojo> dbReader;
			@Override
			public void begin() throws CdatException {
				super.begin();
				producer.open();
			}
			@Override
			public boolean process() throws CdatException {
				super.process();
				boolean hasMore = producer.read();
				System.out.println("dbReader has more? " + hasMore);
				return hasMore;
			}
			@Override
			public void end() {
				super.end();
				Closer.close(dbReader);
				Closer.close(producer);
			}
		};
		
		
		String workerName = session.addWorker("SelectAllPeople", worker);
		session.send(workerName, Control.Start);
//		manager.shutdown();
		//prod.open().read().close();
				
		final String[] result = new String[1];
		session.send(workerName, Control.onComplete, new Callback() {			
			@Override
			public void onComplete(Throwable t, Message response) {
				result[0] =  new String(target.toByteArray());
			}
		});
		
		TestUtils.waitAlittleWhileForResponse(result);

		String csv = result[0];
		System.out.println( csv );
		
		Assert.assertTrue("Header expexted", csv.startsWith("\"name\",\"address\",\"phone\""));
		Assert.assertTrue("Only one header", 0>csv.indexOf("\"name\",\"address\",\"phone\"", 20));
		Assert.assertTrue("Expect to find John", csv.contains("John"));
		Assert.assertTrue("Expect to find Jane", csv.contains("Jane"));
	}
}