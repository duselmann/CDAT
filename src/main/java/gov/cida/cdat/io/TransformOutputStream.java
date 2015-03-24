package gov.cida.cdat.io;

import gov.cida.cdat.transform.Transformer;

import java.io.IOException;
import java.io.OutputStream;

public class TransformOutputStream extends OutputStream {

	private OutputStream target;
	private Transformer transform;
	
	public TransformOutputStream(OutputStream target, Transformer transform) {
		this.target = target;
		this.transform = transform;
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		byte[] newBytes = transform.transform(bytes, off, len);
		target.write(newBytes);
	}
	
	@Override
	public void write(int b) throws IOException {
		throw new RuntimeException("Writing a single byte is not supported");
	}
	
	public void write(Object obj) throws IOException {
		byte[] bytes = transform.transform(obj);
		target.write(bytes);
	}
	
	@Override
	public void flush() throws IOException {
		target.write( transform.getRemaining() );
		super.flush();
	}
	
	@Override
	public void close() throws IOException {
		Closer.close(target);
		super.close();
	}
}
