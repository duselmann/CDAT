package gov.cida.cdat.io.stream;

import gov.cida.cdat.transform.Transformer;

import java.io.IOException;
import java.io.OutputStream;

public class TransformOutputStream<T extends OutputStream> extends OutputStream {

	private T target;
	private Transformer transform;
	
	public TransformOutputStream(T target, Transformer transform) {
		this.target = target;
		this.transform = transform;
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		byte[] newBytes = transform.transform(bytes, off, len);
		target.write(newBytes, off, len);
	}
	
	@Override
	public void write(int b) throws IOException {
		throw new RuntimeException("Writing a single byte is not supported");
	}
}
