package gov.cida.cdat.io.stream.api;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;

public interface Stream<S> extends Closeable, Openable<S> {
	// S should be an InputStream or OutputStream of some sort
	S getStream();
	// TODO do not like the public
	// TODO will need to examine exception list
	S init() throws StreamInitException;
}
