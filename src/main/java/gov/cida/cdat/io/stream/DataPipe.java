package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.IO;
import gov.cida.cdat.io.Openable;
import gov.cida.cdat.io.stream.api.StreamContainer;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPipe implements Openable<InputStream>, Closeable {
	
	private final StreamContainer<InputStream> producer;
	private final StreamContainer<OutputStream> consumer;
	private boolean isComplete;
	
	public DataPipe(StreamContainer<InputStream> ins, StreamContainer<OutputStream> out) {
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
		InputStream  in = null;
		OutputStream out= null;
		try {
			try {
				in = producer.open();
			} catch (Exception e) {
				throw new  StreamInitException("Failed open producer to pipe streams", e);
			}
			try {
				out = consumer.open();
			} catch (Exception e) {
				throw new  StreamInitException("Failed open consumer to pipe streams", e);
			}
			try {
				IO.copy(in, out);
			} catch (Exception e) {
				throw new  StreamInitException("Failed to copy pipe streams", e);
			}
			return producer.getStream(); // TODO this might not be useful
		} finally {
			close(); // TODO should this auto close on complete?
		}
	}

	@Override
	public void close() {
		isComplete = true;
		try {
			Closer.close(producer);
		} finally {
			Closer.close(consumer);
		}
	}
	
	public boolean isComplete() {
		return isComplete;
	}
}
