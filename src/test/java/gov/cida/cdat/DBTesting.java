package gov.cida.cdat;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.stream.StreamContainer;
import gov.cida.cdat.transform.Transformer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;


public class DBTesting {

	public static class Person implements Serializable {
		private static final long serialVersionUID = 1L;
		String name;
		String address;
		String phone;
	}
	
	public static class PersonInputStream extends ObjectInputStream {

		ResultSet rs;
		
		protected PersonInputStream() throws IOException, SecurityException {
		}
		protected PersonInputStream(ResultSet rs) throws IOException, SecurityException {
			this.rs = rs;
		}

		@Override
		protected Object readObjectOverride() throws IOException, ClassNotFoundException {
			Person person = null;
			try {
				if (rs.next()) {
					person = new Person();
					person.name = rs.getString(1);
					person.address = rs.getString(2);
					person.phone = rs.getString(3);
				}
			} catch (Exception e) {
				throw new IOException("Error reading from results.",e);
			}
			return person;
		}
		
		@Override
		public void close() throws IOException {
			try {
				rs.close();
			} catch (SQLException e) {
				throw new IOException("Error closing resultset.",e);
			} finally {
				rs = null;
			}
		}
	}

	public static class DbContainer extends StreamContainer<PersonInputStream>  {

		ResultSet rs;
		Connection conn;
		
		public DbContainer(Connection conn) {
			this.conn = conn;
		}
			
		@Override
		public PersonInputStream init() throws StreamInitException {
			try {
				Statement st = conn.createStatement();
				rs = st.executeQuery("select * from people");
				return new PersonInputStream(rs);
			} catch (Exception e) {
				throw new StreamInitException("Failed to open db result set", e);
			}
			
		}

		@Override
		protected String getName() {
			return getClass().getName();
		}
	}
	
	public static class PersonTransformToCharacterSeparator extends Transformer<Person> {
		
		String separator = ",";
		
		PersonTransformToCharacterSeparator(String join) {
			separator = join;
		}
		
		@Override
		public byte[] transform(Person person) {
				
			StringBuilder buf = new StringBuilder(200);
			buf.append('"').append(person.name).append('"').append(separator);
			buf.append('"').append(person.address).append('"').append(separator);
			buf.append('"').append(person.phone).append('"');
			
			return buf.toString().getBytes();
		}
		
	}
	
	
	
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
		
//		// consumer
//		ByteArrayOutputStream target = new ByteArrayOutputStream(4096*4);
//		SimpleStreamContainer<ByteArrayOutputStream> baosc = new SimpleStreamContainer<ByteArrayOutputStream>(baos);
//		
//		// Transformer
//		Transformer transform = new PersonTransformToCharacterSeparator(",");
//		TransformOutputStream tout = new TransformOutputStream(target, transform);
//
//		SimpleStreamContainer<OutputStream> out  = new SimpleStreamContainer<OutputStream>(tout);
//
//		// Producer
//		URL url = new URL("http://www.google.com");
//		UrlStreamContainer google = new UrlStreamContainer(url);
//		
//		// Pipe producer to consumer
//		final DataPipe pipe = new DataPipe(google, out);
		
	}
}