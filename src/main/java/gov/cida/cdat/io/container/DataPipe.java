package gov.cida.cdat.io.container;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.exception.consumer.ConsumerException;
import gov.cida.cdat.exception.producer.ProducerException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.IO;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPipe implements Openable<InputStream>, Closeable {
	public static final String PIPE_ERROR = "Failed to copy pipe streams";
	
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
		} catch (StreamInitException e) {
			closePipe();
			throw e;
		} catch (Exception e) {
			closePipe();
			throw new  StreamInitException("Failed open producer to pipe streams", e);
		}
		try {
			consumer.open();
		} catch (StreamInitException e) {
			closePipe();
			throw e;
		} catch (Exception e) {
			closePipe();
			throw new  StreamInitException("Failed open consumer to pipe streams", e);
		}
		return producer.getStream(); // TODO this might not be useful or necessary
	}

	
	// this is useful for small test buffers and large big flow buffers
	public boolean process(long milliseconds, int bufferSize) throws CdatException{
		try {
			boolean isMore = IO.copy(producer.getStream(), consumer.getStream(), milliseconds, bufferSize);
			return isMore;
		} catch (ProducerException e) {
			throw new ProducerException(PIPE_ERROR, e);
		} catch (ConsumerException e) {
			throw new ConsumerException(PIPE_ERROR, e);
		} catch (Exception e) {
			throw new CdatException(PIPE_ERROR, e);
		}
	}
	public boolean process(long milliseconds) throws CdatException{
		try {
			boolean isMore = IO.copy(producer.getStream(), consumer.getStream(), milliseconds);
			return isMore;
		} catch (ProducerException e) {
			throw new ProducerException(PIPE_ERROR, e);
		} catch (ConsumerException e) {
			throw new ConsumerException(PIPE_ERROR, e);
		} catch (Exception e) {
			throw new CdatException(PIPE_ERROR, e);
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
		closePipe();
	}
	/**
	 * Helper method so close() and exceptions on open() can clean up
	 * TODO should process call this on exception also?
	 */
	protected void closePipe() {
		Closer.close(producer);
		Closer.close(consumer);
	}
	
	
	public boolean isComplete() {
		return isComplete;
	}
}
