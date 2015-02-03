package gov.cida.cdat.db;

import gov.cida.cdat.exception.CdatException;

import java.io.Closeable;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DbReader <T> implements Closeable {

	ResultSet rs;
	
	public abstract T read() throws CdatException;
	
	protected DbReader(ResultSet rs) {
		this.rs = rs;
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