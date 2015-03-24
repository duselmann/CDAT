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
		checkForTerminator(bytes, off, len);
		return transformLocal(bytes, off, len);
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
	
	protected void checkForTerminator(byte[] bytes, int off, int len) {
		if (!transforming) {
			return;
		}
		
		byte[] toCheck = bytes;
		if (cache != null) {
			toCheck = new byte[cache.length + len];
			System.arraycopy(cache, 0, toCheck, 0, cache.length);
			System.arraycopy(bytes, off, toCheck, cache.length, len);
		}
		
		int length = len + ( (cache==null) ?0 :cache.length );
		if (length < terminator.length) {
			cache = new byte[length];
			System.arraycopy(toCheck, off, cache, 0, length);
			return;
		} else {
			cache = null;
		}
				
		boolean match = false;
		for (int bite=off; bite<=length-terminator.length; bite++) {
			match = matchBytes(terminator, toCheck, bite);
			if (match) {
				transforming = false;
				break;
			}
		}
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
