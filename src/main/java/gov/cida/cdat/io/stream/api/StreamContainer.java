package gov.cida.cdat.io.stream.api;

import gov.cida.cdat.exception.StreamInitException;
import gov.cida.cdat.io.Openable;

import java.io.Closeable;

public interface StreamContainer<S> extends Closeable, Openable<S> {
	// S should be an InputStream or OutputStream of some sort
	S getStream();
	// TODO do not like the public, rename to StreamFactory?
	// TODO will need to examine exception list, maybe just CdatException
	// TODO do not like the init and open pattern - investigate a possible cleaner impl
	S init() throws StreamInitException;
}
