package gov.cida.cdat.db;

import java.sql.Connection;
import java.sql.ResultSet;

public class PojoDbContainer extends DbContainer<Pojo> {

	public PojoDbContainer(Connection conn) {
		super(conn);
	}

	@Override
	public DbReader<Pojo> init(ResultSet rs) {
		return new PojoDbReader(rs);
	}
}
