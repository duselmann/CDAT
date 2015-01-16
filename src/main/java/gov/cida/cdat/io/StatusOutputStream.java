package gov.cida.cdat.io;

import java.io.IOException;
import java.io.OutputStream;

public class StatusOutputStream extends OutputStream {

	private boolean isOpen;
	private boolean isDone;
	private long    byteCount;
	private long    lastWriteTime;
	private long	openTime;

	private OutputStream target;
	
	public StatusOutputStream(OutputStream target) {
		this.target = target;
	}
	
	/**
	 * updates status and delegates to target output stream
	 */
	@Override
	public void write(int b) throws IOException {
		updateTime();
		byteCount += 2;
		target.write(b);
	}
	/**
	 * updates status and delegates to target output stream
	 */
	@Override
	public void write(byte[] bytes) throws IOException {
		updateTime();
		byteCount += bytes.length;
		target.write(bytes);
	}
	/**
	 * updates status and delegates to target output stream
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		updateTime();
		byteCount += (len-off);
		target.write(b, off, len);
	}

	/**
	 * updates the last write time
	 */
	private void updateTime() {
		isOpen = true;
		if (openTime==0) {
			openTime = System.currentTimeMillis();
		}
		lastWriteTime = System.currentTimeMillis();
	}
	/**
	 * returns the milliseconds since the last write
	 * @return long milliseconds since last write
	 */
	public long getMsSinceLastWrite() {
		return System.currentTimeMillis() - lastWriteTime;
	}

	/**
	 * return the time from first write to close
	 * @return
	 */
	public long getOpenTime() {
		// the openTime initially stores the full time in milliseconds when open was called
		// upon close it then stores the duration in milliseconds
		if (isOpen) {
			return System.currentTimeMillis() - openTime;
		}
		return openTime;
	}
	
	/**
	 * returns the current total byte write count
	 * @return long bytes written
	 */
	public long getByteCount() {
		return byteCount;
	}
	
	/**
	 * True if a byte has been transferred
	 * @return
	 */
	public boolean isOpen() {
		return isOpen;
	}
	/**
	 * True if has been open at one point and then closed
	 * @return
	 */
	public boolean isDone() {
		return isDone;
	}
	
	/**
	 * closes the wrapped output stream after updating close and done status
	 */
	@Override
	public void close() throws IOException {
		if (isOpen) {
			isDone = true;
			openTime = System.currentTimeMillis() - openTime;
		}
		isOpen = false;
		target.close();
		target = null;
	}
	
	@Override
	public void flush() throws IOException {
		target.flush();
	}
}
