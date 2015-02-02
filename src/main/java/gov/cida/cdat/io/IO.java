package gov.cida.cdat.io;

import gov.cida.cdat.exception.CdatException;
import gov.cida.cdat.exception.consumer.ConsumerException;
import gov.cida.cdat.exception.producer.ProducerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IO {
	private static final Logger logger = LoggerFactory.getLogger(IO.class);

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
	public static boolean copy(InputStream source, OutputStream target, long duration) throws CdatException {
		// if duration is zero or less that is the signal to copy all
		// otherwise we copy for the given duration
		if (duration <= 0) {
			copy(source, target);
			return false; // there is no more
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
			write(target, buffer, count);
			count = read(source, buffer);
			total += count;
		}
		write(target, buffer, count);
		
		logger.trace("total bytes read {}", total);
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
	public static boolean copy(InputStream source, OutputStream target, int bufferSize, long duration)
			throws CdatException {
		long currentTime = System.currentTimeMillis();
		long endTime     = currentTime + duration;
		
		byte[] buffer = new byte[bufferSize];
		long total = 0;
		int  count = 0;

		while (count >= 0 && currentTime < endTime) {
			write(target, buffer, count);
			count = read(source, buffer);
			total += count;
			currentTime = System.currentTimeMillis();
		}
		write(target, buffer, count);
		
		logger.trace("total bytes read {}", total);
		return count >= 0;
	}

	private static void write(OutputStream target, byte[] buffer, int count) throws ConsumerException {
		if (count <= 0) {
			return;
		}
		try {
			target.write(buffer, 0, count);
		} catch (Exception e) {
			throw new ConsumerException("Error writing to consumer stream", e);
		}
	}
	private static int read(InputStream source, byte[] buffer) throws ProducerException {
		try {
			//Thread.sleep(250); // TODO this is here for testing
			return source.read(buffer);
		} catch (Exception e) {
			throw new ProducerException("Error reading from producer stream", e);
		}
	}
}