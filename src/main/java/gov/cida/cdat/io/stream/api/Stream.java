package gov.cida.cdat.io.stream.api;

import gov.cida.cdat.io.Openable;

import java.io.Closeable;

public interface Stream<S> extends Closeable, Openable<S> {
	// S should be an InputStream or OutputStream of some sort
	S getStream();
}
