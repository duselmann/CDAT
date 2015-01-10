package gov.cida.cdat.io;

import gov.cida.cdat.exception.StreamInitException;

public interface Openable<S> {
	S open() throws StreamInitException;
}
