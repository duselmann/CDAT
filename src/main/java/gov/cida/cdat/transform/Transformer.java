package gov.cida.cdat.transform;

public interface Transformer {

	byte[] transform(byte[] bytes, int off, int len);

}
