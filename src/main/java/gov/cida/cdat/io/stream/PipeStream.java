package gov.cida.cdat.io.stream;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Closer;
import gov.cida.cdat.io.IO;
import gov.cida.cdat.io.Openable;
import gov.cida.cdat.io.stream.api.Stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PipeStream implements Openable<InputStream>, Closeable {
	
	private final Stream<InputStream> producer;
	private final Stream<OutputStream> consumer;
	
	public PipeStream(Stream<InputStream> ins, Stream<OutputStream> out) {
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
			Closer.close(producer);
			Closer.close(consumer);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			Closer.close(producer);
		} catch (Throwable t) {/* ensure next close is attempted */}
		Closer.close(consumer);
	}
}
