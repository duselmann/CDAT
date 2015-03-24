package gov.cida.cdat.transform;

public class TerminatingTransformer extends Transformer {

	private byte[] terminator;
	private Transformer transform;
	private boolean transforming;
	private byte[] cache;

	public TerminatingTransformer(byte[] terminator, Transformer transform) {
		this.transforming = true;
		this.terminator = terminator;
		this.transform  = transform;
	}
	
	@Override
	public byte[] transform(byte[] bytes, int off, int len) {
		int terminationLength = checkForTerminator(bytes, off, len);
		// if transforming we need to know the location of the terminator
		// if terminator has been found then we need to know the full buffer length
		if (terminationLength < 0 || terminationLength >= len ) {
			return transformLocal(bytes, off, len);
		}
		
		byte[] transformed = transformLocal(bytes, off, terminationLength);
		transforming = false;
		byte[] remainder   = transformLocal(bytes, terminationLength, len-terminationLength);
		
		byte[] mixedBytes = new byte[transformed.length+remainder.length];
		System.arraycopy(transformed, 0, mixedBytes, 0, transformed.length);
		System.arraycopy(remainder, 0, mixedBytes, transformed.length, remainder.length);
		
		return mixedBytes;
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
		
		if (!transforming) {
			return -1;
		}
		int terminationLength=len;
		
		// presume we will use the given byte array
		byte[] toCheck = bytes;
		int offset = off;
		int length = len;
		
		// if there are cached bytes then we must check them
		if (cache != null) {
			toCheck = new byte[cache.length + len];
			System.arraycopy(cache, 0, toCheck, 0, cache.length);
			System.arraycopy(bytes, off, toCheck, cache.length, len);
			offset = 0;
			length = toCheck.length;
		}
		
		// if the current bytes are less than the terminator - caching them for the next segment
		if (length < terminator.length) {
			// TODO if the matchBytes method could use a off/len combo for both arrays then this could be optimized
			cache = new byte[length];
			System.arraycopy(toCheck, offset, cache, 0, length);
			return terminationLength;
		} else {
			cache = null;
		}
		
		boolean match = false;
		for (int bite=offset; bite<=length-terminator.length; bite++) {
			match = matchBytes(terminator, toCheck, bite);
			if (match) {
				terminationLength =  bite - offset;
//				transforming = false;
				break;
			}
		}
		return terminationLength;
	}

	/**
	 * find an array bytes at the given offset
	 * @param find bytes to find
	 * @param source bytes to search
	 * @param off starting index in the source
	 * @return true if the bytes are at the given offset
	 */
	public static boolean matchBytes(byte[] find, byte[] source, int off) {
		boolean match = false;
		
		// if not bytes to find or more than the source then no match
		if (find==null || find.length==0 || source==null || source.length-off < find.length ) {
			return false;
		}
		int b = 0;
		for (b=0; b<find.length; b++) {
			match = find[b]==source[b+off];
			if ( ! match ) {
				break;
			}
		}
		
		return match;
	}
	
//	@Override
//	public byte[] transform(Object obj) {
//		if (transforming) {
//			return transform.transform(obj);
//		}
//		return super.transform(obj);
//	}
	
}
