package gov.cida.cdat.db;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.stream.TransformStreamContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class PojoDbContainer extends DbContainer<Pojo> {

	ResultSet rs;
	
	public PojoDbContainer(Connection conn, TransformStreamContainer<Pojo> target) {
		super(conn, target);
	}

	@Override
	public DbReader<Pojo> init(TransformOutputStream<Pojo> transformer) throws StreamInitException {
		System.out.println("Create pojo db reader.");
		
		try {
			Statement st = conn.createStatement();
			rs = st.executeQuery("select * from people");
		} catch (Exception e) {
			throw new StreamInitException("Failed to open db result set", e);
		}

		return new PojoDbReader(rs, target);
	}
}
