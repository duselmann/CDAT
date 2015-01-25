package gov.cida.cdat.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IO {
	public static final int DEFAULT_BUFFER_SIZE = 0x1000; // 4K

	private IO() {}

	public static long copy(InputStream source, OutputStream target) throws IOException {
		return copy(source, target, DEFAULT_BUFFER_SIZE);
	}

	public static long copy(InputStream source, OutputStream target, int bufferSize)
			throws IOException {
		
		byte[] buffer = new byte[bufferSize];
		long total = 0;
		int  count = 0;

		while (count >= 0) {
			target.write(buffer, 0, count);
			count = source.read(buffer);
			total += count;
		}
		
		return total;
	}
}