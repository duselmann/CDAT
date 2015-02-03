package gov.cida.cdat.db;

import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.transform.Transformer;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;


public class DBTesting {
	
	
	Connection conn;

	
	
	@Before
	public void setUp() throws SQLException, ClassNotFoundException {
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
	public void testSelectStream() {
		System.out.println("testSelectStream");
		
		// consumer
		ByteArrayOutputStream target = new ByteArrayOutputStream(4096*4);
//		SimpleStreamContainer<ByteArrayOutputStream> baosc = new SimpleStreamContainer<ByteArrayOutputStream>(baos);
		
		// Transformer
		Transformer<Pojo> transform = new PojoCharacterSeparatorTransformer(",");
		TransformOutputStream<Pojo> pout = new TransformOutputStream<Pojo>(target, transform);

//		SimpleStreamContainer<OutputStream> out  = new SimpleStreamContainer<OutputStream>(tout);
//
		// Producer
		
//		
//		// Pipe producer to consumer
//		final DataPipe pipe = new DataPipe(google, out);
		
	}
}