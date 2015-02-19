package gov.cida.cdat.transform;

public abstract class Transformer {

	public byte[] transform(byte[] bytes, int off, int len) {
		return null;
	}
	
	public byte[] transform(Object obj) {
		return null;
	}
}
