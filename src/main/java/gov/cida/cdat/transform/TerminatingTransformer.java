package gov.cida.cdat.transform;

public class TerminatingTransformer extends Transformer {

	private final byte[] terminator;
	private final Transformer transform;
	
	private boolean transforming;

	public TerminatingTransformer(byte[] terminator, Transformer transform) {
		this.transforming = true;
		this.terminator = terminator;
		this.transform  = transform;
		
		cacheLength = terminator.length;
	}
	
	@Override
	public byte[] transform(byte[] bytes, int off, int len) {
//		if (transforming) {
			int terminationLength = checkForTerminator(bytes, off, len);
			// if transforming we need to know the location of the terminator
			// if terminator has been found then we need to know the full buffer length
			if (terminationLength < 0 || terminationLength >= len ) {
				return transformLocal(bytes, off, len);
			}
			
			byte[] transformed = transformLocal(bytes, off, terminationLength);
			byte[] remainder1  = transform.getRemaining();
			transforming = false;
			byte[] remainder2  = transformLocal(bytes, off+terminationLength, len-terminationLength);
			
			return merge(transformed, remainder1, remainder2);
//		}
		
//		return transformLocal(bytes, off, len);
	}

	protected byte[] transformLocal(byte[] bytes, int off, int len) {
		if (transforming) {
			return transform.transform(bytes, off, len);
		}
		// it is a bit unfortunate that we cannot just pass through if no transform needed because of off/len args
		// TODO look into a no-op bypass
		byte[] raw = new byte[len];
		System.arraycopy(bytes, off, raw, 0, len);
		
		return raw;
	}
	
	/**
	 * Checks the given bytes for the termination pattern. If the termination pattern is found
	 * it will return the location where it was found. If not found it will return the given length
	 * of the byte array, indicating that the termination will be beyond the current buffer. However,
	 * if the termination has already been encountered it will be set to -1, indicating the termination
	 * has already been encountered.
	 * 
	 * @param bytes the buffer to check for the termination byte pattern
	 * @param off   the index to commence checking
	 * @param len   the number of bytes to consider
	 * @return the location of the termination. -1 for already encountered, the len arg if not found, and
	 * any other number in between for the location it was found.
	 */
	protected int checkForTerminator(byte[] bytes, int off, int len) {
		
		if ( ! transforming ) {
			return -1;
		}
		int terminationLength=len;
		
		// presume we will use the given byte array
		byte[] toCheck = bytes;
		int    offset  = off;
		int    length  = len;
		
		// if there are cached bytes then we must check them
		if (cache != null) {
//			toCheck = new byte[cache.length + len];
//			System.arraycopy(cache, 0, toCheck, 0, cache.length);
//			System.arraycopy(bytes, off, toCheck, cache.length, len);
			toCheck = merge(cache, 0, cache.length, bytes, off, len);

			offset = 0;
			length = toCheck.length;
		}
		
		manageCache(toCheck, offset, length);
		if (cache!=null) {
			return terminationLength;
		}
		
		boolean match = false;
		for (int bite=offset; bite<=offset+length-terminator.length; bite++) {
			match = matchBytes(terminator, toCheck, bite);
			if (match) {
				terminationLength =  bite - offset;
//				transforming = false;
				break;
			}
		}
		return terminationLength;
	}
	
	@Override
	public byte[] getRemaining() {
		return transform.getRemaining(); // TODO need testing on this
	}
	
//	@Override
//	public byte[] transform(Object obj) {
//		if (transforming) {
//			return transform.transform(obj);
//		}
//		return super.transform(obj);
//	}
	
}
