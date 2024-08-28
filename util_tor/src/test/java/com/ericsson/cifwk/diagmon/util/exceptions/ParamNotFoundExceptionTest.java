package com.ericsson.cifwk.diagmon.util.exceptions;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParamNotFoundExceptionTest {

	private static ParamNotFoundException paramNotFound;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		paramNotFound = new ParamNotFoundException("");
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetMessage() {
		assertEquals("No values returned for: ",paramNotFound.getMessage());	
	}
	
	@Test
	public void testGetLocalisedMessage(){
		assertEquals("No values returned for: ",paramNotFound.getLocalizedMessage());
	}
	
	@Test
	public void testToString(){
		assertEquals("No values returned for: ",paramNotFound.toString());
	}
	
}
