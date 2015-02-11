package gov.cida.cdat.db;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.exception.producer.ProducerException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.Openable;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.container.TransformStreamContainer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DbReader<T> implements Closeable, Openable<OutputStream> {

	protected Connection conn;
	protected int fetchSize;

	protected ResultSet rs;
	protected Statement st;
	
	protected TransformStreamContainer<T> transformer;
	protected TransformOutputStream<T> target;
		
	
	protected DbReader(Connection conn, TransformStreamContainer<T> transformer) {
		this.transformer = transformer;
		this.conn = conn;
		fetchSize = 1024; // TODO make configure
	}

	
	public abstract T createInstance(ResultSet rs) throws SQLException;
	public abstract void query() throws StreamInitException;

	
	@Override
	public OutputStream open() throws StreamInitException {
		System.out.println("DbReader open");
		
		target = transformer.open();
		try {
			st = conn.createStatement(); // TODO prepared statement
			st.setFetchSize(fetchSize);
			query();
		} catch (Exception e) {
			throw new StreamInitException("Failed to create statement.",e);
		}
//		st.getFetchSize();
		
		return target;
	}
	
	
	public boolean read() throws CdatException {
		System.out.println("DbReader read");

		if (target == null) {
			throw new StreamInitException("Must call open before reading");
		}
		boolean hasNext = false;
		try {
			int count = 0;
			// we must do the follow while clause in the proper order because of the 
			// ResultSet.next() side effect of advancing the row.
			// the hasNext assignment desired to capture the state for to return
			// the count compare must come first in order to preserve the next row
			while ( count++<fetchSize && (hasNext=rs.next()) ) {
				T instance = createInstance(rs);
				target.write(instance);
			}
		} catch (Exception e) {
			throw new ProducerException("Error reading from results.",e);
		}
		return hasNext;
	}
	
	
	@Override
	public void close() throws IOException {
		System.out.println("DbReader close");
		
		Closer.close(transformer);
		Closer.close(target);
		Closer.close(rs);
		
		target = null;
		conn = null;
		rs = null;
	}

	
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}
}