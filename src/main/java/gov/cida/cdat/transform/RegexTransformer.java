package gov.cida.cdat.transform;


public class RegexTransformer extends Transformer {

	private String pattern;
	private String replace;
	
	
	public RegexTransformer(String pattern, String replace) {
		this.pattern = pattern;
		this.replace = replace;
		cacheLength  = pattern.length();
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
			toTrans = merge(cache, 0, cache.length, bytes, off, len);
			offset  = 0;
			length  = toTrans.length;
		}
		
		manageCache(toTrans, offset, length);
		if (cache!=null) {
			return new byte[0];
		}
		
		String buff = new String(toTrans, offset, length);
		byte[] tran = buff.replaceAll(pattern, replace).getBytes();

		byte[] finished = updateCache(tran);
		
		return finished;
	}

}
