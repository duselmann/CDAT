package gov.cida.cdat.io;

import gov.cida.cdat.control.Time;
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
		return copy(source,target,duration,DEFAULT_BUFFER_SIZE);
	}
	public static boolean copy(InputStream source, OutputStream target, long duration, int bufferSize) throws CdatException {
		// if duration is zero or less that is the signal to copy all
		// otherwise we copy for the given duration
		if (duration < 0) {
			logger.trace("IO copy source to target with no duration");
			copy(source, target, bufferSize);
			return false; // there is no more
		} else {
			logger.trace("IO copy source to target with duration {}", duration);
			return copy(source, target, bufferSize, duration);
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

		while ((count = read(source, buffer)) >= 0) {
			write(target, buffer, count);
			total += count;
		}
		
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
		long endTime     = Time.later(duration);
		
		byte[] buffer = new byte[bufferSize];
		long total = 0;
		int  count = 0;

		while (Time.now() < endTime && (count = read(source, buffer)) >= 0) {
			logger.trace("read duration remaining {}", endTime-Time.now());
			write(target, buffer, count);
			total += count;
		}
		
		logger.trace("total read bytes {}", total);
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
			return source.read(buffer);
		} catch (Exception e) {
			throw new ProducerException("Error reading from producer stream", e);
		}
	}
}