package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.IO;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPipe implements Openable<InputStream>, Closeable {
	
	public static final int DEFAULT_DURATION = 10000; // TODO make configurable
	public static final int FULL_DURATION    = -1;
	
	private final StreamContainer<? extends InputStream>  producer;
	private final StreamContainer<? extends OutputStream> consumer;
	private boolean isComplete;
	
	public DataPipe(StreamContainer<? extends InputStream> ins, StreamContainer<? extends OutputStream> out) {
		this.producer = ins;
		this.consumer = out;
	}

	public InputStream getProducerStream() {
		return producer.getStream();
	}
	public OutputStream getConsumerStream() {
		return consumer.getStream();
	}

	@Override
	public InputStream open() throws StreamInitException {
		try {
			producer.open();
		} catch (Exception e) {
			Closer.close(producer);
			throw new  StreamInitException("Failed open producer to pipe streams", e);
		}
		try {
			consumer.open();
		} catch (Exception e) {
			Closer.close(producer);
			Closer.close(consumer);
			throw new  StreamInitException("Failed open consumer to pipe streams", e);
		}
		return producer.getStream(); // TODO this might not be useful or necessary
	}

	
	public boolean process(long milliseconds) throws CdatException{
		try {
			boolean isMore = IO.copy(producer.getStream(), consumer.getStream(), milliseconds);
			return isMore;
		} catch (Exception e) {
			throw new  StreamInitException("Failed to copy pipe streams", e);
		}
	}
	public boolean process() throws CdatException{
		return process(DEFAULT_DURATION);
	}
	public boolean processAll() throws CdatException{
		return process(-1);
	}
	

	@Override
	public void close() {
		isComplete = true;
		Closer.close(producer);
		Closer.close(consumer);
	}
	
	public boolean isComplete() {
		return isComplete;
	}
}
