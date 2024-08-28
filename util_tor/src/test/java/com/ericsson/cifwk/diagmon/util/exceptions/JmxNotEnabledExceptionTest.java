package com.ericsson.cifwk.diagmon.util.exceptions;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmxNotEnabledExceptionTest {
	
	private static DDCException jmxNotEnabled;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		jmxNotEnabled = new JmxNotEnabledException("This DDC Message");
		
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
		assertEquals("This DDC Message",jmxNotEnabled.getMessage());	
	}
	
	@Test
	public void testGetLocalisedMessage(){
		assertEquals("This DDC Message",jmxNotEnabled.getLocalizedMessage());
	}

}
