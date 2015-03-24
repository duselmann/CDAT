package gov.cida.cdat.transform;


public class RegexTransformer extends Transformer {

	String pattern;
	String replace;
	int buffsize;
	
	
	public RegexTransformer(String pattern, String replace) {
		this.pattern = pattern;
		this.replace = replace;
		buffsize = pattern.length();
	}
	
	
	@Override
	public byte[] transform(byte[] bytes, int off, int len) {
		if (len<=0 || off+len>bytes.length) {
			return new byte[0];
		}
		byte[] toTrans = bytes;
		int    offset  = off;
		int    length  = len;
		
		if (cache != null) {
//			toTrans = new byte[cache.length + len];
//			System.arraycopy(cache, 0, toTrans, 0, cache.length);
//			System.arraycopy(bytes, off, toTrans, cache.length, len);
			toTrans = merge(cache, 0, cache.length, bytes, off, len);
			offset  = 0;
			length  = toTrans.length;
		}
		
		if (length < buffsize) {
			// TODO if the matchBytes method could use a off/len combo for both arrays then this could be optimized
			cache = new byte[length];
			System.arraycopy(toTrans, offset, cache, 0, length);
			return new byte[0];
		} else {
			cache = null;
		}
		
		String buff = new String(toTrans, offset, length);
		byte[] tran = buff.replaceAll(pattern, replace).getBytes();
		
		cache = new byte[pattern.length()-1];
		System.arraycopy(tran, tran.length-cache.length, cache, 0, cache.length);
		
		byte[] finished = new byte[tran.length-cache.length];
		System.arraycopy(tran, 0, finished, 0, tran.length-cache.length);

		return finished;
	}

}
