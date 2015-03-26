package gov.cida.cdat.transform;

public abstract class Transformer {

	protected byte[] cache;
	protected int cacheLength;
	
	
	public byte[] transform(byte[] bytes, int off, int len) {
		return null;
	}
	
	public byte[] transform(Object obj) {
		return null;
	}
	
	public byte[] getRemaining() {
		byte[] theCache = cache;
		cache = null;
		return theCache;
	}
	
	void setCacheLength(int cacheLength) {
		this.cacheLength = cacheLength;
	}
	
	void manageCache(byte[] results, int offset, int length) {
		if (length < cacheLength) {
			// TODO if the matchBytes method could use a off/len combo for both arrays then this could be optimized
			cache = new byte[length];
			System.arraycopy(results, offset, cache, 0, length);
		} else {
			cache = null;
		}
	}
	byte[] updateCache(byte[] results) {
		int length = cacheLength-1;
		if (length > results.length) {
			cache = results;
			return new byte[0];
		}
		cache = new byte[cacheLength-1];
		System.arraycopy(results, results.length-cache.length, cache, 0, cache.length);
		
		byte[] result = new byte[results.length-cache.length];
		System.arraycopy(results, 0, result, 0, results.length-cache.length);
		
		return result;
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
	
	public static byte[] merge(byte[] ... arrays) {
		int length = 0;
		for (byte[] array : arrays) {
			if (array!=null) {
				length += array.length;
			}
		}
		byte[] merged = new byte[length];

		length = 0;
		for (byte[] array : arrays) {
			if (array!=null) {
				System.arraycopy(array, 0, merged, length, array.length);
				length += array.length;
			}
		}
				
		return merged;
	}
		
	public static byte[] merge(byte[] abytes, int aoff, int alen, byte[] bbytes, int boff, int blen) {
		byte[] merged = new byte[alen+blen];

		System.arraycopy(abytes, aoff, merged, 0, alen);
		System.arraycopy(bbytes, boff, merged, alen, blen);
				
		return merged;
	}
		
//	/**
//	 * find an array bytes at the given offset
//	 * @param find bytes to find
//	 * @param source bytes to search
//	 * @param off starting index in the source
//	 * @return true if the bytes are at the given offset
//	 */
//	public static boolean partialEndMatchBytes(byte[] find, byte[] source, int off) {
//		boolean match = false;
//		
//		// if not bytes to find or more than the source then no match
//		if (find==null || find.length==0 || source==null || source.length-off < find.length ) {
//			return false;
//		}
//		int b = 0;
//		for (b=0; b<find.length; b++) {
//			match = find[b]==source[b+off];
//			if ( ! match ) {
//				break;
//			}
//		}
//		
//		return match;
//	}
		
}
