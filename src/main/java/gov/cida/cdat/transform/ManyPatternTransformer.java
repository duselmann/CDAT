package gov.cida.cdat.transform;

import java.util.Iterator;
import java.util.LinkedList;


public class ManyPatternTransformer extends Transformer {

	private LinkedList<RegexTransformer> transformers = new LinkedList<RegexTransformer>();
	
	public void addMapping(String pattern, String replace) {
		validate(pattern, replace);
		
		int index = findInsertIndex(pattern);
		transformers.add(index, new RegexTransformer(pattern, replace));
		
		if (pattern.length()>cacheLength) {
			setCacheLength(pattern.length());
		}
	}

	protected void validate(String pattern, String replace) {
		if (pattern == null || pattern.length()==0) {
			throw new RuntimeException("Pattern may not be null or empty. Replace string is " + replace);
		}
		if (replace == null) {
			throw new RuntimeException("Replace may not be null but may be empty. Pattern string is " + pattern);
		}
		if (pattern.equals(replace)) {
			throw new RuntimeException("Not action will take place if pattern==replace. Pattern string is" + pattern);
		}
		for (RegexTransformer rt : transformers) {
			if (rt.getPattern().equals(pattern)) {
				throw new RuntimeException("Duplicate patterns are not acceptable. Pattern string is " + pattern);
			}
			if (rt.getReplace().equals(pattern)) {
				throw new RuntimeException("This pattern will be subsequently replaced by another. I.E. A=>B B=>C The two pattern strings are "
						+ pattern + " and " + rt.getPattern());
			}
			if (rt.getPattern().equals(replace)) {
				throw new RuntimeException("This pattern will be subsequently replaced by another. I.E. A=>B B=>C The two pattern strings are "
						+ pattern + " and " + rt.getPattern());
			}
		}
	}

	protected int findInsertIndex(String pattern) {
		int index = 0;
		for (RegexTransformer rt : transformers) {
			if (pattern.length() < rt.getPattern().length()) {
				index++;
			} else {
				break;
			}
		}
		return index;
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
