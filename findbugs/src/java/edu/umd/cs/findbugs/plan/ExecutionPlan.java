/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorOrderingConstraint;
import edu.umd.cs.findbugs.Plugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A plan for executing Detectors on an application.
 * Automatically assigns Detectors into passes and orders
 * Detectors within each pass based on ordering constraints
 * specified in the plugin descriptor(s).
 *
 * @author David Hovemeyer
 */
public class ExecutionPlan {
	private List<Plugin> pluginList;

	public ExecutionPlan() {
		this.pluginList = new LinkedList<Plugin>();
	}

	public void addPlugin(Plugin plugin) {
		pluginList.add(plugin);
	}

	public void build() {
		// Build map of detector class names to their factories
		Map<String, DetectorFactory> factoryMap = buildFactoryMap();

		// Get inter-pass ordering constraints
		List<DetectorOrderingConstraint> interPassConstraintList =
			new LinkedList<DetectorOrderingConstraint>();
		for (Iterator<Plugin> i = pluginList.iterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			copyTo(plugin.interPassConstraintIterator(), interPassConstraintList);
		}
	}

	private static<T> void copyTo(Iterator<T> iter, List<T> dest) {
		while (iter.hasNext()) {
			dest.add(iter.next());
		}
	}

	private Map<String, DetectorFactory> buildFactoryMap() {
		Map<String, DetectorFactory> factoryMap = new HashMap<String, DetectorFactory>();

		for (Iterator<Plugin> j = pluginList.iterator(); j.hasNext(); ) {
			Plugin plugin = j.next();
			for (Iterator<DetectorFactory> i = plugin.detectorFactoryIterator(); i.hasNext(); ) {
				DetectorFactory factory = i.next();
				factoryMap.put(factory.getFullName(), factory);
			}
		}

		return factoryMap;
	}
}

// vim:ts=4
