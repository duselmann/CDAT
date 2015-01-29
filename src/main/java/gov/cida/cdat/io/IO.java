package gov.cida.cdat.io;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.consumer.ConsumerException;
import gov.cida.cdat.exception.producer.ProducerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IO {
	public static final int DEFAULT_BUFFER_SIZE = 0x1000; // 4K

	private IO() {}

	/**
	 * Simply copies all bytes from the input source to the output destination.
	 * 
	 * @param source the input suppling data.
	 * @param target the output data destination
	 * @return total bytes read
	 * @throws IOException
	 */
	public static long copy(InputStream source, OutputStream target) throws CdatException {
		return copy(source, target, DEFAULT_BUFFER_SIZE);
	}
	public static long copy(InputStream source, OutputStream target, long duration) throws CdatException {
		// if duration is zero or less that is the signal to copy all
		// otherwise we copy for the given duration
		if (duration <= 0) {
			return copy(source, target);
		} else {
			return copy(source, target, DEFAULT_BUFFER_SIZE, duration);
		}
	}

	/**
	 * Simply copies all bytes from the input source to the output destination.
	 * 
	 * @param source the input suppling data.
	 * @param target the output data destination
	 * @param bufferSize the buffer size to use during copy
	 * @return total bytes read
	 * @throws IOException
	 */
	public static long copy(InputStream source, OutputStream target, int bufferSize)
			throws CdatException {
		
		byte[] buffer = new byte[bufferSize];
		long total = 0;
		int  count = 0;

		while (count >= 0) {
			try {
				target.write(buffer, 0, count);
			} catch (Exception e) {
				throw new ConsumerException("Error writing to consumer stream", e);
			}
			try {
				count = source.read(buffer);
			} catch (Exception e) {
				throw new ProducerException("Error reading from producer stream", e);
			}
			total += count;
		}
		
		return total;
	}
	
	/**
	 * Duration copy. This allows for a breather while process so that status and 
	 * control messages can be processed. If we did not do this we would have to
	 * wait until the entire source completes to respond to an isAlive message.
	 * 
	 * @param source the input suppling data.
	 * @param target the output data destination
	 * @param bufferSize the buffer size to use during copy
	 * @param duration max time in milliseconds to run
	 * @return total bytes read
	 * @throws IOException
	 */
	public static long copy(InputStream source, OutputStream target, int bufferSize, long duration)
			throws CdatException {
		long currentTime = System.currentTimeMillis();
		long endTime     = currentTime + duration;
		
		byte[] buffer = new byte[bufferSize];
		long total = 0;
		int  count = 0;

		while (count >= 0 && currentTime < endTime) {
			try {
				Thread.sleep(100); // TODO this is here for testing
				target.write(buffer, 0, count);
			} catch (Exception e) {
				throw new ConsumerException("Error writing to consumer stream", e);
			}
			try {
				count = source.read(buffer);
			} catch (Exception e) {
				throw new ProducerException("Error reading from producer stream", e);
			}
			total += count;
			currentTime = System.currentTimeMillis();
		}
		
		return total;
	}
}