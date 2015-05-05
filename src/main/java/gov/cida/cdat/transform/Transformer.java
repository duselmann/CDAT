package gov.cida.cdat.transform;

public abstract class Transformer {

	protected byte[] cache;
	protected int cacheLength;
	
	
	public byte[] transform(byte[] bytes, int off, int len) {
		return null;
	}
	
	public <T> byte[] transform(T obj) {
		return null;
	}
	
	public byte[] getRemaining() {
		byte[] theCache = cache;
		cache = null;
		return theCache;
	}
	
	/**
	 * The buffer cached must be the length of the search pattern less on char.
	 * When used in the ManyPatternTransforms it will be set to the largest search pattern.
	 * 
	 * @param cacheLength
	 */
	void setCacheLength(int cacheLength) {
		this.cacheLength = cacheLength;
	}
	
	/**
	 * This manages the transform buffer which is called cache because it persists between writes to ensure that 
	 * partial writes are accumulated before transforming.
	 * 
	 * Suppose we have "The minor Lubowsky" and "minor" is to be transformed to "GREATE". Then if the write was performed in the
	 * following manner. 
	 * 
	 * write("The min");
	 * write("or Lubowsky");
	 * 
	 * Notice that the "min" in the first write is not "minor" but is a portion.
	 * The cache will persist " min" so that when the "or Lubowsky" is written it can work with " minor Lubowsky".
	 * The result will be a write of "The" followed by a write of the transformed into " GREATE Lubowsky"
	 * 
	 * @param results
	 * @param offset
	 * @param length
	 */
	void manageCache(byte[] results, int offset, int length) {
		if (length < cacheLength) {
			// TODO if the matchBytes method could use a off/len combo for both arrays then this could be optimized
			cache = new byte[length];
			System.arraycopy(results, offset, cache, 0, length);
		} else {
			cache = null;
		}
	}
	/**
	 * Replace or append to the cache buffer.
	 * 
	 * With the Lubowsky example above, see the examples below.
	 * 
	 * write("The");
	 * write(" ");
	 * write("minor");
	 * write(" Lubowsky");
	 * 
	 * The first string is cached entirely because it is less than the replacement "minor".
	 * Upon writing the second string the cache will be extended to "the " which is 1 less than the search string length.
	 * The third write will target "The minor" for replace because the cache buffer is prepended to the following write.
	 * Then the replace will occur to make it "The GREATE" and "EATE" will replace the cache for possible replace next write.
	 * (There could be some optimization here but when performing a ManyPatterTransform it is more complicated and makes
	 * initial sense to leave it as is.) Finally, the last write leave us with "wsky" because the transforming stream does
	 * not know when it is the last write occurs. A flush during close will write the last cache. 
	 * 
	 * @param results
	 * @return
	 */
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
	
	public String encode(String value) {
		return value;
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
