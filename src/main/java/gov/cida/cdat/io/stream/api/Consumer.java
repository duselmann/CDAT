package gov.cida.cdat.io.stream.api;

import java.io.OutputStream;

public interface Consumer<S extends OutputStream> extends Stream<OutputStream> {
}
