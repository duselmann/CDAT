package gov.cida.cdat.io;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;

public class Closer {
	public static void close(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			// TODO I know, I know. Never swallow. But really?
		}
	}
	public static void close(ResultSet closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			// TODO I know, I know. Never swallow. But really?
		}
	}
	public static void close(Connection closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			// TODO I know, I know. Never swallow. But really?
		}
	}
}
