package com.ericsson.cifwk.diagmon.util.instr.providers.versant;

import com.ericsson.cifwk.diagmon.util.instr.config.ConfigTreeNode;
import com.ericsson.cifwk.diagmon.util.instr.InstrException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class VersantConfig {
	public final String name;
	public final List<String> metrics = new ArrayList<String>();

	public VersantConfig(final ConfigTreeNode base) throws InstrException {
		name = base.getAttribute("name");
		for (final Iterator<ConfigTreeNode> i = base.getChildren().iterator() ; i.hasNext() ; ) {
			final ConfigTreeNode node = i.next();
			final String nodeName = node.baseName();
			if ( nodeName.equals("metric") ) {
				metrics.add(node.getAttribute("name"));
			}
		}
	}    
}
