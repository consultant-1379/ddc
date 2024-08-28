package com.ericsson.cifwk.diagmon.util.exceptions;

import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.Mockito;


public class DDCExceptionTest {
	
	private static DDCException dDCException;
	private static Throwable throwable;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dDCException = new DDCException("This DDC Message");
		
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
		assertEquals("This DDC Message",dDCException.getMessage());	
	}
	
	@Test
	public void testGetLocalisedMessage(){
		assertEquals("This DDC Message",dDCException.getLocalizedMessage());
	}

}
