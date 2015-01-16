package gov.cida.cdat.io;

import gov.cida.cdat.exception.StreamInitException;

/**
 *  analigous to the Closeable interface
 *  
 *  the reason this is here is so we can construct a stream and then initiate it with open 
 * @author duselmann
 *
 * @param <S> inputstream or outputstream
 */
public interface Openable<S> {
	S open() throws StreamInitException;
}
