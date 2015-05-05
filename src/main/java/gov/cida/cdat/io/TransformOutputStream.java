package gov.cida.cdat.io;

import gov.cida.cdat.transform.Transformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


public class TransformOutputStream extends OutputStream {

	protected final OutputStream target;
	protected final Transformer transform;
	
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
	
	
	public static Object extractObject(byte[] bytes) {
		return extractObject(bytes, 0, bytes.length);
	}
	public static Object extractObject(byte[] bytes, int off, int len) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes,off,len));
			return ois.readObject();
		} catch (ClassNotFoundException cause) {
			throw new RuntimeException("Failed to find matching class during object exrtaction", cause);
		} catch (Exception cause) {
			throw new RuntimeException("should not exception because it is a closed system", cause);
		}
	}
	public static byte[] objectByteArray(Object obj) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (IOException cause) {
			// this should really not throw an exception
			throw new RuntimeException("should not exception because it is a closed system", cause);
		}
	}
}
