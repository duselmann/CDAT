package gov.cida.cdat.db;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.Openable;
import gov.cida.cdat.io.TransformOutputStream;
import gov.cida.cdat.io.stream.TransformStreamContainer;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;

public abstract class DbContainer<T> implements Openable<DbReader<T>>,Closeable  {

	Connection conn;
	DbReader<T> dbReader;
	TransformStreamContainer<T> target;
	
	public DbContainer(Connection conn, TransformStreamContainer<T> transformer) {
		this.conn = conn;
		target = transformer;
	}
	
	public abstract DbReader<T> init(TransformOutputStream<T> target) throws StreamInitException;
	
	public DbReader<T> open() throws StreamInitException {
		System.out.println("DbContainer open");
		
		dbReader = init(target.open());
		return dbReader;
		
	}

	@Override
	public void close() throws IOException {
		System.out.println("DbContainer close");
		Closer.close(dbReader);
		Closer.close(target);
	}
}
