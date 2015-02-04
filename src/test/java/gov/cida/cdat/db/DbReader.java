package gov.cida.cdat.db;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.exception.producer.ProducerException;
import gov.cida.cdat.io.Openable;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.stream.TransformStreamContainer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DbReader<T> implements Closeable, Openable<OutputStream> {

	private ResultSet rs;
	private TransformStreamContainer<T> transformer;
	private TransformOutputStream<T> target;
		
	
	protected DbReader(ResultSet rs, TransformStreamContainer<T> transformer) {
		this.rs = rs;
		this.transformer = transformer;
	}

	public abstract T createInstance(ResultSet rs) throws SQLException;

	@Override
	public OutputStream open() throws StreamInitException {
		target = transformer.getStream();
		return target;
	}
	
	public DbReader<T> read() throws CdatException {
		System.out.println("DbReader read");

		try {
			open(); // TODO this should be refactored
			while (rs.next()) {
				T instance = createInstance(rs);
				target.write(instance);
			}
		} catch (Exception e) {
			throw new ProducerException("Error reading from results.",e);
		}
		return this;
	}
	
	@Override
	public void close() throws IOException {
		System.out.println("DbReader close");
		
		try {
			transformer.close();
			rs.close();
		} catch (SQLException e) {
			throw new IOException("Error closing resultset.",e);
		} finally {
			rs = null;
		}
	}
}