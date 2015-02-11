package gov.cida.cdat.db;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.container.TransformStreamContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PojoDbReader extends DbReader<Pojo> {
	
	public PojoDbReader(Connection conn, TransformStreamContainer<Pojo> transformer) {
		super(conn, transformer);
	}
	
	
	@Override
	public void query() throws StreamInitException {
		System.out.println("Create pojo db reader.");
		
		try {
			rs = st.executeQuery("select * from people");
		} catch (Exception e) {
			throw new StreamInitException("Failed to open db result set", e);
		}
	}
	
	
	@Override
	public Pojo createInstance(ResultSet rs) throws SQLException {
		System.out.println("create pojo instance from rs.");
		
		Pojo person = new Pojo();
		person.name = rs.getString(1);
		person.address = rs.getString(2);
		person.phone = rs.getString(3);
		return person;
	}
}
