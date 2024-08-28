package com.ericsson.cifwk.diagmon.util.instr.config;

import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.mockito.Mock;
import org.omg.CORBA.NameValuePair;

import com.ericsson.cifwk.diagmon.util.instr.config.ConfigTreeNode;

public class ConfigTreeNodeTest {
    private ConfigTreeNode global;

    @Before
    public void setUp() throws Exception {
  //      global = mock(ConfigTreeNode.class);
        global = new ConfigTreeNode("test");
    }

    @Test
    public void test() {
       // when(global.getAttribute("tester")).thenReturn("2");
        global.addAttribute("tester", "2");
        assertEquals("2", global.getAttribute("tester"));
    }

}
