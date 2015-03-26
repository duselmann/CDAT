package gov.cida.cdat.transform;

import java.util.Iterator;
import java.util.LinkedList;


public class ManyPatternTransformer extends Transformer {

	private LinkedList<RegexTransformer> transformers = new LinkedList<RegexTransformer>();
	
	public void addMapping(String pattern, String replace) {
		transformers.add(new RegexTransformer(pattern, replace));
		if (pattern.length()>cacheLength) {
			setCacheLength(pattern.length());
		}
	}

	@Override
	public byte[] transform(byte[] bytes, int off, int len) {
		byte[] result = new byte[0];
		Iterator<RegexTransformer> rts = transformers.iterator();
		if ( ! rts.hasNext() ) {
			result = new byte[len];
			System.arraycopy(bytes, off, result, 0, len);
			return result;
		}

		byte[] toTrans = bytes;
		int    offset  = off;
		int    length  = len;
		
		if (cache != null) {
			toTrans = merge(cache, 0, cache.length, bytes, off, len);
			offset  = 0;
			length  = toTrans.length;
		}
		
		manageCache(toTrans, offset, length);
		if (cache!=null) {
			return new byte[0];
		}
		
		byte[] remain;
		RegexTransformer rt = rts.next();
		
		result = rt.transform(toTrans, offset, length); // this first is special because of the byte off/len
		remain = rt.getRemaining();
		result = merge(result, remain);
		
		while (rts.hasNext()) {
			rt = rts.next();
			
			result = rt.transform(result, 0, result.length);
			remain = rt.getRemaining();
			result = merge(result, remain);
		}
		result = updateCache(result);
		
		return result;
	}
}
