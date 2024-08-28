package com.ericsson.cifwk.diagmon.util.instr;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InstrExceptionTest {
	private InstrException instrExcep;
	private InstrException e;
	private InstrException instrEx;
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		instrExcep = new InstrException("Instr Exception", e);
		instrEx =new InstrException("Instr Ex");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetLocalisedMessage() {
		assertEquals("Instr Exception", instrExcep.getLocalizedMessage());
	}
	
	@Test
	public void testGetMessage(){
		assertEquals("Instr Exception", instrExcep.getMessage());
	}
	
	@Test
	public void testGetMessage2(){
		assertEquals("Instr Ex", instrEx.getMessage());
	}

}
