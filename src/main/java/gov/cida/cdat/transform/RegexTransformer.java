package gov.cida.cdat.transform;

public class RegexTransformer implements Transformer{

	String pattern;
	String replace;
	
	public RegexTransformer(String pattern, String replace) {
		this.pattern = pattern;
		this.replace = replace;
	}
	
	
	@Override
	public byte[] transform(byte[] bytes, int off, int len) {

		String buff = new String(bytes, off, len);
		return buff.replaceAll(pattern, replace).getBytes();
		
	}

}
