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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A plan for executing Detectors on an application.
 * Automatically assigns Detectors to passes and orders
 * Detectors within each pass based on ordering constraints
 * specified in the plugin descriptor(s).
 *
 * @author David Hovemeyer
 */
public class ExecutionPlan {

	private static final boolean DEBUG = Boolean.getBoolean("findbugs.execplan.debug");

	private List<Plugin> pluginList;
	private LinkedList<AnalysisPass> passList;

	/**
	 * Constructor.
	 * Creates an empty plan.
	 */
	public ExecutionPlan() {
		this.pluginList = new LinkedList<Plugin>();
		this.passList = new LinkedList<AnalysisPass>();
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
		Map<String, DetectorNode> nodeMap = new HashMap<String, DetectorNode>();
		ConstraintGraph interPassConstraintGraph = buildConstraintGraph(
			nodeMap, factoryMap, interPassConstraintList);
		if (DEBUG) System.out.println(interPassConstraintGraph.getNumVertices() +
			" nodes in inter-pass constraint graph");

		// Build list of analysis passes.
		buildPassList(interPassConstraintGraph);

		// Make sure there is at least one pass
		if (passList.isEmpty())
			passList.add(new AnalysisPass());
		AnalysisPass finalPass = passList.getLast();

		// All detectors not involved in inter-pass constraints are
		// added to the final pass.
		Set<String> remainingDetectorSet = new HashSet<String>();
		remainingDetectorSet.addAll(factoryMap.keySet());
		remainingDetectorSet.removeAll(nodeMap.keySet());
		for (Iterator<String> i = remainingDetectorSet.iterator(); i.hasNext(); ) {
			String detectorClass = i.next();
			DetectorFactory factory = factoryMap.get(detectorClass);

			finalPass.addDetectorFactory(factory);
		}

		// Get intra-pass ordering constraints
		List<DetectorOrderingConstraint> intraPassConstraintList =
			new LinkedList<DetectorOrderingConstraint>();
		for (Iterator<Plugin> i = pluginList.iterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			copyTo(plugin.intraPassConstraintIterator(), intraPassConstraintList);
		}

		// Sort detectors in each pass to satisfy intra-pass ordering constraints
		for (Iterator<AnalysisPass> i = passList.iterator(); i.hasNext(); ) {
			AnalysisPass pass = i.next();
			sortPass(intraPassConstraintList, factoryMap, pass);
		}

	}

	private static<T> void copyTo(Iterator<T> iter, Collection<T> dest) {
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
	 * A topological sort of the constraint graph will yield a
	 * correct ordering of the detectors (which may mean either
	 * passes or an ordering within a single pass, depending on
	 * whether the constraints are inter-pass or intra-pass).
	 *
	 * @param nodeMap        map to be populated with detector
	 *                       class names to constraint graph nodes for
	 *                       those detectors
	 * @param factoryMap     map of class names to DetectorFactory objects
	 *                       (needed to create graph nodes)
	 * @param constraintList List of ordering constraints
	 * @return the ConstraintGraph
	 */
	private ConstraintGraph buildConstraintGraph(
		Map<String, DetectorNode> nodeMap,
		Map<String, DetectorFactory> factoryMap,
		List<DetectorOrderingConstraint> constraintList) throws OrderingConstraintException {

		ConstraintGraph result = new ConstraintGraph();

		for (Iterator<DetectorOrderingConstraint> i = constraintList.iterator(); i.hasNext(); ) {
			DetectorOrderingConstraint constraint = i.next();

			DetectorNode earlier = addOrCreateDetectorNode(
				constraint.getEarlierDetector(), nodeMap, factoryMap, result);
			DetectorNode later = addOrCreateDetectorNode(
				constraint.getLaterDetector(), nodeMap, factoryMap, result);

			result.createEdge(earlier, later);
		}

		return result;
	}

	private DetectorNode addOrCreateDetectorNode(
			String className,
			Map<String, DetectorNode> nodeMap,
			Map<String, DetectorFactory> factoryMap,
			ConstraintGraph constraintGraph)
		throws OrderingConstraintException {
		DetectorNode node = nodeMap.get(className);
		if (node == null) {
			DetectorFactory factory = factoryMap.get(className);
			if (factory == null)
				throw new OrderingConstraintException("Unknown detector in ordering constraint: " + className);
			node = new DetectorNode(factory);
			nodeMap.put(className, node);
			constraintGraph.addVertex(node);
		}
		return node;
	}

	private void buildPassList(ConstraintGraph constraintGraph)
			throws OrderingConstraintException {
		while (constraintGraph.getNumVertices() > 0) {
			List<DetectorNode> inDegreeZeroList = new LinkedList<DetectorNode>();

			// Get all of the detectors nodes with in-degree 0.
			// These have no unsatisfied prerequisites, and thus can
			// be chosen for the current pass.
			int inDegreeZeroCount = 0;
			for (Iterator<DetectorNode> i = constraintGraph.vertexIterator(); i.hasNext(); ) {
				DetectorNode node = i.next();

				if (constraintGraph.getNumIncomingEdges(node) == 0) {
					++inDegreeZeroCount;
					inDegreeZeroList.add(node);
				}
			}

			if (inDegreeZeroCount == 0)
				throw new OrderingConstraintException("Cycle in inter-pass ordering constraints");

			// Remove all of the chosen detectors from the constraint graph.
			for (Iterator<DetectorNode> i = inDegreeZeroList.iterator(); i.hasNext(); ) {
				DetectorNode node = i.next();
				constraintGraph.removeVertex(node);
			}

			// Create analysis pass and add detector factories.
			AnalysisPass pass = new AnalysisPass();
			for (Iterator<DetectorNode> i = inDegreeZeroList.iterator(); i.hasNext(); ) {
				DetectorNode node = i.next();
				pass.addDetectorFactory(node.getFactory());
			}

			// Add pass to list of passes in the execution plan.
			passList.add(pass);
		}
	}

	private void sortPass(
			List<DetectorOrderingConstraint> constraintList,
			Map<String, DetectorFactory> factoryMap,
			AnalysisPass pass)
		throws OrderingConstraintException {

		// Preserve original order of detectors in pass
		List<DetectorFactory> origDetectorList = new LinkedList<DetectorFactory>();
		origDetectorList.addAll(pass.getDetectorFactoryList());

		// Build set of all detectors in pass.
		// Also, check to see if any detectors want to be
		// first in the pass.
		Set<String> detectorSet = new HashSet<String>();
		DetectorFactory firstDetector = null;
		for (Iterator<DetectorFactory> i = pass.detectorFactoryIterator();
			i.hasNext(); ) {
			DetectorFactory factory = i.next();

			if (factory.isFirstInPass()) {
				if (firstDetector != null) {
					throw new OrderingConstraintException("Two detectors specified as first in analysis pass: " +
						firstDetector.getFullName() + " and " + factory.getFullName());
				} else
					firstDetector = factory;
			}

			detectorSet.add(factory.getFullName());
		}

		// Build list of ordering constraints in this pass only
		List<DetectorOrderingConstraint> passConstraintList =
			new LinkedList<DetectorOrderingConstraint>();
		for (Iterator<DetectorOrderingConstraint> i = constraintList.iterator(); i.hasNext(); ) {
			DetectorOrderingConstraint constraint = i.next();

			if (detectorSet.contains(constraint.getEarlierDetector()) ||
				detectorSet.contains(constraint.getLaterDetector())) {

				// Make sure that both detectors are in this pass
				if (!(detectorSet.contains(constraint.getEarlierDetector()) &&
						detectorSet.contains(constraint.getLaterDetector()))) {
					throw new OrderingConstraintException("Intra-pass constraint " +
						constraint.getEarlierDetector() + " earlier than " +
						constraint.getLaterDetector() + " involves detectors in " +
						"different passes");
				}

				// Make sure this constraint doesn't conflict with
				// a FirstInPass directive (if any).
				if (firstDetector != null) {
					if (firstDetector.getFullName().equals(constraint.getEarlierDetector()) ||
						firstDetector.getFullName().equals(constraint.getLaterDetector())) {
						throw new OrderingConstraintException("Detector specified as FirstInPass (" +
							firstDetector.getShortName() +") also appears in an ordering constraint");
					}
				}

				passConstraintList.add(constraint);
			}
		}

		// Build intra-pass constraint graph
		Map<String, DetectorNode> nodeMap = new HashMap<String, DetectorNode>();
		ConstraintGraph constraintGraph = buildConstraintGraph(
			nodeMap, factoryMap, passConstraintList);
		if (DEBUG) {
			System.out.println("Pass constraint graph:");
			dumpGraph(constraintGraph);
		}

		// Perform DFS, check for cycles
		DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode> dfs =
			new DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode>(constraintGraph);
		dfs.search();
		if (dfs.containsCycle())
			throw new OrderingConstraintException("Cycle in intra-pass ordering constraints!");

		// Do a topological sort to put the detectors in the pass
		// in the right order.
		pass.clear();
		for (Iterator<DetectorNode> i = dfs.topologicalSortIterator(); i.hasNext(); ) {
			DetectorNode node = i.next();
			pass.addDetectorFactory(node.getFactory());
		}

		// Add back detectors that aren't involved in any intra-pass
		// ordering constraints.
		for (Iterator<DetectorFactory> i = origDetectorList.iterator(); i.hasNext(); ) {
			DetectorFactory factory = i.next();
			if (nodeMap.get(factory.getFullName()) == null) {
				if (factory.isFirstInPass())
					pass.prependDetectorFactory(factory);
				else
					pass.addDetectorFactory(factory);
			}
		}
	}

	private void print() {
		int passCount = 0;
		for (Iterator<AnalysisPass> i = passList.iterator(); i.hasNext(); ++passCount) {
			System.out.println("Pass " + passCount);
			AnalysisPass pass = i.next();
			for (Iterator<DetectorFactory> j = pass.detectorFactoryIterator(); j.hasNext(); ) {
				DetectorFactory factory = j.next();
				System.out.println("  " + factory.getFullName());
			}
		}
	}

	private void dumpGraph(ConstraintGraph graph) {
		for (Iterator<ConstraintEdge> i = graph.edgeIterator(); i.hasNext();) {
			ConstraintEdge edge = i.next();
			System.out.println(
				edge.getSource().getFactory().getFullName() + " ==> " +
				edge.getTarget().getFactory().getFullName());
		}
	}

	public static void main(String[] argv) throws Exception {
		edu.umd.cs.findbugs.DetectorFactoryCollection detectorFactoryCollection =
			edu.umd.cs.findbugs.DetectorFactoryCollection.instance();

		ExecutionPlan execPlan = new ExecutionPlan();

		for (int i = 0; i < argv.length; ++i) {
			String pluginId = argv[i];
			Plugin plugin = detectorFactoryCollection.getPluginById(pluginId);
			if (plugin != null)
				execPlan.addPlugin(plugin);
		}

		execPlan.build();

		System.out.println(execPlan.passList.size() + " passes in plan");
		execPlan.print();
	}
}

// vim:ts=4
