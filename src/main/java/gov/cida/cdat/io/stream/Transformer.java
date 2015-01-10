package gov.cida.cdat.io.stream;

public class Transformer {

	public byte[] transform(byte[] bytes, int off, int len) {

		String buff = new String(bytes, off, len);
		return buff.replaceAll("div", "span").getBytes();
		
	}

}
