package gov.cida.cdat.db;

import gov.cida.cdat.io.stream.TransformStreamContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PojoDbReader extends DbReader<Pojo> {
	
	
	protected PojoDbReader(ResultSet rs, TransformStreamContainer<Pojo> transformer) {
		super(rs, transformer);
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
