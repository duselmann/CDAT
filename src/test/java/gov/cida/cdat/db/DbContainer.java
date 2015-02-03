package gov.cida.cdat.db;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class DbContainer<T> implements Openable<DbReader<T>>,Closeable  {

	Connection conn;
	DbReader<T> dbReader;
	
	public DbContainer(Connection conn) {
		this.conn = conn;
	}
	
	public abstract DbReader<T> init(ResultSet rs);
	
	public DbReader<T> open() throws StreamInitException {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("select * from people");
			dbReader = init(rs);
			return dbReader;
		} catch (Exception e) {
			throw new StreamInitException("Failed to open db result set", e);
		}
		
	}

	@Override
	public void close() throws IOException {
		dbReader.close();
	}
}
