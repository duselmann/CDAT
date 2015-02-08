package gov.cida.cdat.control;

import static org.junit.Assert.*;

import org.junit.Test;

public class StatusEnumTests {

	@Test
	public void testIs_null() {
		assertFalse("Null should return false", Status.isAlive.is(null));
	}
	@Test
	public void testIs_stringMatches() {
		assertTrue("Matching Strings should return true", Status.isAlive.is("isAlive"));
	}
	@Test
	public void testIs_stringNonMatch() {
		assertFalse("Nonmatching Strings should return false", Status.isAlive.is("nonMatchingStatusString"));
	}

}
