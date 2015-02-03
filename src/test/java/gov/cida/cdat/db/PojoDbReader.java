package gov.cida.cdat.db;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.producer.ProducerException;

import java.sql.ResultSet;

public class PojoDbReader extends DbReader<Pojo> {
	
	protected PojoDbReader(ResultSet rs) {
		super(rs);
	}
	
	public Pojo read() throws CdatException {
		Pojo person = null;
		try {
			if (rs.next()) {
				person = new Pojo();
				person.name = rs.getString(1);
				person.address = rs.getString(2);
				person.phone = rs.getString(3);
			}
		} catch (Exception e) {
			throw new ProducerException("Error reading from results.",e);
		}
		return person;
	}
}
