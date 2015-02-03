package gov.cida.cdat.transform;

public abstract class Transformer<T> {

	public byte[] transform(byte[] bytes, int off, int len) {
		return null;
	}
	public byte[] transform(T obj) {
		return null;
	}

}
