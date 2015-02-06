package gov.cida.cdat.io;


// TODO ARE THE WRITES BLOCKING
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class SplitOutputStream extends OutputStream {

	private OutputStream[] targets;
	private final Map<String,Exception> errors;
	
	public SplitOutputStream(OutputStream ... targets) {
		this.targets = targets;
		errors = new HashMap<String, Exception>();
	}
	
	/**
	 * updates status and delegates to target output streams
	 */
	@Override
	public void write(int b) throws IOException {
		for (OutputStream target : targets) {
			try {
				if ( ! errors.containsKey(target) ) {
					target.write(b);
				}
			} catch (Exception e) {
				manageException(target, e);
			}
		}
	}

	/**
	 * updates status and delegates to target output streams
	 */
	@Override
	public void write(byte[] bytes) throws IOException {
		for (OutputStream target : targets) {
			try {
				if ( ! errors.containsKey(target) ) {
					target.write(bytes);
				}
			} catch (Exception e) {
				manageException(target, e);
			}
		}
	}
	
	/**
	 * updates status and delegates to target output streams
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		for (OutputStream target : targets) {
			try {
				if ( ! errors.containsKey(target) ) {
					target.write(b, off, len);
				}
			} catch (Exception e) {
				manageException(target, e);
			}
		}
	}

	/**
	 * closes the wrapped output streams 
	 */
	@Override
	public void close() throws IOException {
		for (OutputStream target : targets) {
			Closer.close(target);
		}
		targets = null;
	}
	
	/**
	 * flush all target streams
	 */
	@Override
	public void flush() throws IOException {
		for (OutputStream target : targets) {
			try {
				if ( ! errors.containsKey(target) ) {
					target.flush();
				}
			} catch (Exception e) {
				manageException(target, e);
			}
		}
	}
	
	/**
	 * helper method that can be enhanced in one location
	 * @param target the stream that is in trouble
	 * @param e the exception that the stream experienced
	 */
	void manageException(OutputStream target, Exception e) {
		if (null==target) {
			return;
		}
		errors.put(target.getClass().getName(), e);
	}
	/**
	 * @return true if any stream threw an exception
	 */
	public boolean hasErrors() {
		return ! errors.isEmpty();
	}
	/**
	 * @return the number of streams that had an exception
	 */
	public int errorCount() {
		return errors.size();
	}
	/**
	 * @return A protected copy of all recent exceptions for streams
	 */
	public Map<String,Exception> getErrors() {
		// protective copy
		return new HashMap<String,Exception>(errors);
	}
}
