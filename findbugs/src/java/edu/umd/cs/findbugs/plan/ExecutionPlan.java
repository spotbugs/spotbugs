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

import edu.umd.cs.findbugs.graph.DepthFirstSearch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A plan for executing Detectors on an application.
 * Automatically assigns Detectors to passes and orders
 * Detectors within each pass based on ordering constraints
 * specified in the plugin descriptor(s).
 *
 * @author David Hovemeyer
 */
public class ExecutionPlan {
	private List<Plugin> pluginList;

	/**
	 * Constructor.
	 * Creates an empty plan.
	 */
	public ExecutionPlan() {
		this.pluginList = new LinkedList<Plugin>();
	}

	/**
	 * Add a Plugin whose Detectors should be added to the execution plan.
	 */
	public void addPlugin(Plugin plugin) {
		pluginList.add(plugin);
	}

	/**
	 * Build the execution plan.
	 * Using the ordering constraints specified in the
	 * plugin descriptor(s), assigns Detectors to passes
	 * and orders the Detectors within those passes.
	 */
	public void build() throws OrderingConstraintException {
		// Build map of detector class names to their factories
		Map<String, DetectorFactory> factoryMap = buildFactoryMap();

		// Get inter-pass ordering constraints
		List<DetectorOrderingConstraint> interPassConstraintList =
			new LinkedList<DetectorOrderingConstraint>();
		for (Iterator<Plugin> i = pluginList.iterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			copyTo(plugin.interPassConstraintIterator(), interPassConstraintList);
		}

		// Build inter-pass constraint graph
		ConstraintGraph interPassConstraintGraph = buildConstraintGraph(factoryMap, interPassConstraintList);

		// Depth first search of constraint graph.  Will throw exception
		// if the graph contains a cycle.
		DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode> dfs =
			getDepthFirstSearch(interPassConstraintGraph);
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

	/**
	 * Build a constraint graph.
	 * This represents ordering constraints between Detectors.
	 * A topological sort of the constraint graph will yield the
	 * correct ordering of the detectors (which may mean either
	 * passes or an ordering within a single pass, depending on
	 * whether the constraints are inter-pass or intra-pass).
	 *
	 * @param factoryMap     map of class names to DetectorFactory objects
	 *                       (needed to create graph nodes)
	 * @param constraintList List of ordering constraints
	 * @return the ConstraintGraph
	 */
	private ConstraintGraph buildConstraintGraph(
		Map<String, DetectorFactory> factoryMap,
		List<DetectorOrderingConstraint> constraintList) throws OrderingConstraintException {

		ConstraintGraph result = new ConstraintGraph();

		Map<String, DetectorNode> nodeMap = new HashMap<String, DetectorNode>();

		for (Iterator<DetectorOrderingConstraint> i = constraintList.iterator(); i.hasNext(); ) {
			DetectorOrderingConstraint constraint = i.next();

			DetectorNode earlier =
				addOrCreateDetectorNode(constraint.getEarlierDetector(), nodeMap, factoryMap);
			DetectorNode later =
				addOrCreateDetectorNode(constraint.getLaterDetector(), nodeMap, factoryMap);

			result.addVertex(earlier);
			result.addVertex(later);

			// The constraints are specified such that the earlier detector node
			// points to the later detector node.  When a topological sort is performed,
			// the earlier detector will be ordered before the later detector.
			result.createEdge(earlier, later);
		}

		return result;
	}

	private DetectorNode addOrCreateDetectorNode(
			String className, Map<String, DetectorNode> nodeMap, Map<String, DetectorFactory> factoryMap)
		throws OrderingConstraintException {
		DetectorNode node = nodeMap.get(className);
		if (node == null) {
			DetectorFactory factory = factoryMap.get(className);
			if (factory == null)
				throw new OrderingConstraintException("Unknown detector in ordering constraint: " + className);
			node = new DetectorNode(factory);
			nodeMap.put(className, node);
		}
		return node;
	}

	/**
	 * Perform a DepthFirstSearch on a ConstraintGraph.
	 *
	 * @param constraintGraph the ConstraintGraph
	 * @return a DepthFirstSearch on the ConstraintGraph
	 * @throws OrderingConstraintException if the ConstraintGraph contains cycles
	 */
	public DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode>
			getDepthFirstSearch(ConstraintGraph constraintGraph)
		throws OrderingConstraintException {

		DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode> dfs =	
			new DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode>(constraintGraph);
		dfs.search();
		if (dfs.containsCycle())
			throw new OrderingConstraintException("Cycle in detector ordering constraints!");
		return dfs;
	}
}

// vim:ts=4
