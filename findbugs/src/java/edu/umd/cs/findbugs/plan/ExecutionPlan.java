/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryChooser;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.graph.DepthFirstSearch;

/**
 * A plan for executing Detectors on an application.
 * Automatically assigns Detectors to passes and orders
 * Detectors within each pass based on ordering constraints
 * specified in the plugin descriptor(s).
 *
 * @author David Hovemeyer
 */
public class ExecutionPlan {

	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.execplan.debug");

	private List<Plugin> pluginList;
	private DetectorFactoryChooser factoryChooser;
	private LinkedList<AnalysisPass> passList;
	private Map<String, DetectorFactory> factoryMap;
	private List<DetectorOrderingConstraint> interPassConstraintList;
	private List<DetectorOrderingConstraint> intraPassConstraintList;
	private Set<DetectorFactory> assignedToPassSet;

	/**
	 * Constructor.
	 * Creates an empty plan.
	 */
	public ExecutionPlan() {
		this.pluginList = new LinkedList<Plugin>();
		this.factoryChooser = new DetectorFactoryChooser() {
			public boolean choose(DetectorFactory factory) {
				return true;
			}

			public void enable(DetectorFactory factory) {
			   // OK...
			}
		};
		this.passList = new LinkedList<AnalysisPass>();
		this.factoryMap = new HashMap<String, DetectorFactory>();
		this.interPassConstraintList = new LinkedList<DetectorOrderingConstraint>();
		this.intraPassConstraintList = new LinkedList<DetectorOrderingConstraint>();
		this.assignedToPassSet = new HashSet<DetectorFactory>();
	}

	/**
	 * Set the DetectorFactoryChooser to use to select which
	 * detectors to enable.  This must be called before any Plugins
	 * are added to the execution plan.
	 */
	public void setDetectorFactoryChooser(DetectorFactoryChooser factoryChooser) {
		this.factoryChooser = factoryChooser;
	}

	/**
	 * Add a Plugin whose Detectors should be added to the execution plan.
	 */
	public void addPlugin(Plugin plugin) throws OrderingConstraintException {
		pluginList.add(plugin);

		// Add ordering constraints
		copyTo(plugin.interPassConstraintIterator(), interPassConstraintList);
		copyTo(plugin.intraPassConstraintIterator(), intraPassConstraintList);

		// Add detector factories
		for (Iterator<DetectorFactory> i = plugin.detectorFactoryIterator(); i.hasNext(); ) {
			DetectorFactory factory = i.next();
			if (factoryMap.put(factory.getFullName(), factory) != null) {
				throw new OrderingConstraintException("Detector " + factory.getFullName() +
						" is defined by more than one plugin");
			}
		}
	}

	/**
	 * Build the execution plan.
	 * Using the ordering constraints specified in the
	 * plugin descriptor(s), assigns Detectors to passes
	 * and orders the Detectors within those passes.
	 */
	public void build() throws OrderingConstraintException {


		ArrayList<DetectorOrderingConstraint> allConstraints = new ArrayList<DetectorOrderingConstraint>(interPassConstraintList.size() + intraPassConstraintList.size());
		allConstraints.addAll(interPassConstraintList);
		allConstraints.addAll(intraPassConstraintList);

		Map<String, DetectorNode> nodeMapAll = new HashMap<String, DetectorNode>();
		ConstraintGraph allPassConstraintGraph = buildConstraintGraph(
			nodeMapAll,
			new HashSet<DetectorFactory>(factoryMap.values()),
			allConstraints);
		boolean change;
		do {
			change = false;
			for(Iterator<DetectorNode> i = allPassConstraintGraph.vertexIterator(); i.hasNext(); ) {
				DetectorNode end = i.next();
				if (factoryChooser.choose(end.getFactory())) {
					for(Iterator<ConstraintEdge> j = allPassConstraintGraph.incomingEdgeIterator(end); j.hasNext(); ) {
						DetectorNode start = j.next().getSource();
						DetectorFactory startFactory = start.getFactory();
						if (!factoryChooser.choose(startFactory)) {
							factoryChooser.enable(startFactory);
							change = true;
							if (DEBUG || FindBugs2.DEBUG)
								System.out.println("Dependences force enabling of " + startFactory.getFullName());
						}

					}
				}

			}
		} while (change);

		for(Iterator<Map.Entry<String,DetectorFactory>> i = factoryMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<String,DetectorFactory> e = i.next();
			if (!factoryChooser.choose(e.getValue()))
					i.remove();
		}

		// Build inter-pass constraint graph
		Map<String, DetectorNode> nodeMap = new HashMap<String, DetectorNode>();
		ConstraintGraph interPassConstraintGraph = buildConstraintGraph(
			nodeMap,
			new HashSet<DetectorFactory>(factoryMap.values()),
			interPassConstraintList);
		if (DEBUG) System.out.println(interPassConstraintGraph.getNumVertices() +
			" nodes in inter-pass constraint graph");

		// Build list of analysis passes.
		// This will assign all detectors referenced in inter- or intra-pass
		// ordering constraints to passes.  Detectors with any ordering
		// constraint will be left unassigned.
		buildPassList(interPassConstraintGraph);

		// Sort each pass by intra-pass ordering constraints.
		// This may assign some previously unassigned detectors to passes.
		for (AnalysisPass pass : passList) {
			sortPass(intraPassConstraintList, factoryMap, pass);
		}

		// If there are any unassigned detectors remaining,
		// add them to the final pass.
		if (factoryMap.size() > assignedToPassSet.size()) {
			AnalysisPass lastPass;
			if (passList.isEmpty()) {
				lastPass = new AnalysisPass();
				addPass(lastPass);
			} else {
				lastPass = passList.getLast();
			}

			Set<DetectorFactory> unassignedSet = getUnassignedSet();
			for (DetectorFactory factory : unassignedSet) {
				assignToPass(factory, lastPass);
			}
			appendDetectorsToPass(unassignedSet, lastPass);
		}
	}

	/**
	 * Get an Iterator over the AnalysisPasses.
	 */
	public Iterator<AnalysisPass> passIterator() {
		return passList.iterator();
	}

	/**
	 * Get the number of passes in the execution plan.
	 * 
	 * @return the number of passes in the execution plan
	 */
	public int getNumPasses() {
		return passList.size();
	}

	private static<T> void copyTo(Iterator<T> iter, Collection<T> dest) {
		while (iter.hasNext()) {
			dest.add(iter.next());
		}
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
	 * @param factorySet     build the graph using these DetectorFactories as nodes
	 * @param constraintList List of ordering constraints
	 * @return the ConstraintGraph
	 */
	private ConstraintGraph buildConstraintGraph(
		Map<String, DetectorNode> nodeMap,
		Set<DetectorFactory> factorySet,
		List<DetectorOrderingConstraint> constraintList) throws OrderingConstraintException {

		ConstraintGraph result = new ConstraintGraph();

		for (DetectorOrderingConstraint constraint : constraintList) {
			Set<DetectorNode> earlierSet = addOrCreateDetectorNodes(
					constraint.getEarlier(), nodeMap, factorySet, result);
			Set<DetectorNode> laterSet = addOrCreateDetectorNodes(
					constraint.getLater(), nodeMap, factorySet, result);

			createConstraintEdges(result, earlierSet, laterSet, constraint);
		}

		return result;
	}

	private Set<DetectorFactory> selectDetectors(
			DetectorFactorySelector selector, Set<DetectorFactory> candidateSet) {
		Set<DetectorFactory> result = new HashSet<DetectorFactory>();
		for (DetectorFactory factory : candidateSet) {
			if (selector.selectFactory(factory)) {
				result.add(factory);
			}
		}
		return result;
	}

	private Set<DetectorNode> addOrCreateDetectorNodes(
			DetectorFactorySelector selector,
			Map<String, DetectorNode> nodeMap,
			Set<DetectorFactory> factorySet,
			ConstraintGraph constraintGraph) throws OrderingConstraintException {
		HashSet<DetectorNode> result = new HashSet<DetectorNode>();

		Set<DetectorFactory> chosenSet = selectDetectors(selector, factorySet);

		for (DetectorFactory factory : chosenSet) {
			DetectorNode node = addOrCreateDetectorNode(factory, nodeMap, constraintGraph);
			result.add(node);
		}

		return result;
	}

	private DetectorNode addOrCreateDetectorNode(
			DetectorFactory factory,
			Map<String, DetectorNode> nodeMap,
			ConstraintGraph constraintGraph)
		throws OrderingConstraintException {
		DetectorNode node = nodeMap.get(factory.getFullName());
		if (node == null) {
			node = new DetectorNode(factory);
			nodeMap.put(factory.getFullName(), node);
			constraintGraph.addVertex(node);
		}
		return node;
	}

	private void createConstraintEdges(
			ConstraintGraph result,
			Set<DetectorNode> earlierSet,
			Set<DetectorNode> laterSet, DetectorOrderingConstraint constraint) throws OrderingConstraintException {

		// It is perfectly fine for a constraint to produce no edges
		// if any detector it specifies is not enabled.
		if (earlierSet.isEmpty() || laterSet.isEmpty())
			return;

		for (DetectorNode earlier : earlierSet) {
			for (DetectorNode later : laterSet) {
				result.createEdge(earlier, later);
			}
		}
	}

	private void buildPassList(ConstraintGraph constraintGraph)
			throws OrderingConstraintException {

		while (constraintGraph.getNumVertices() > 0) {
			List<DetectorNode> inDegreeZeroList = new LinkedList<DetectorNode>();
			// Get all of the detectors nodes with in-degree 0.
			// These have no unsatisfied prerequisites, and thus can
			// be chosen for the current pass.
			for (Iterator<DetectorNode> i = constraintGraph.vertexIterator(); i.hasNext(); ) {
				DetectorNode node = i.next();
				if (constraintGraph.getNumIncomingEdges(node) == 0) 
					inDegreeZeroList.add(node);

			}

			if (inDegreeZeroList.isEmpty())
				throw new OrderingConstraintException("Cycle in inter-pass ordering constraints");

			// Remove all of the chosen detectors from the constraint graph.
			for (DetectorNode node : inDegreeZeroList) {
				constraintGraph.removeVertex(node);
			}

			// Create analysis pass and add detector factories.
			// Note that this just makes the detectors members of the pass:
			// it doesn't assign them a position in the pass.
			AnalysisPass pass = new AnalysisPass();
			addPass(pass);
			for (DetectorNode node : inDegreeZeroList) {
				assignToPass(node.getFactory(), pass);
			}

		}
	}

	private void addPass(AnalysisPass pass) {
		if (DEBUG) {
			System.out.println("Adding pass " + passList.size());
		}
		passList.add(pass);
	}

	private void sortPass(
			List<DetectorOrderingConstraint> constraintList,
			Map<String, DetectorFactory> factoryMap,
			AnalysisPass pass)
		throws OrderingConstraintException {

		// Build set of all (initial) detectors in pass
		Set<DetectorFactory> detectorSet = new HashSet<DetectorFactory>(pass.getMembers());
		if (DEBUG) {
			System.out.println(detectorSet.size() + " detectors currently in this pass");
		}

		// Build list of ordering constraints in this pass only
		List<DetectorOrderingConstraint> passConstraintList =
			new LinkedList<DetectorOrderingConstraint>();
		for (DetectorOrderingConstraint constraint : constraintList) {
			// Does this constraint specify any detectors in this pass?
			// If so, add it to the pass constraints
			if (selectDetectors(constraint.getEarlier(), detectorSet).size() > 0
					|| selectDetectors(constraint.getLater(), detectorSet).size() > 0) {
				passConstraintList.add(constraint);
			}
		}
		if (DEBUG) {
			System.out.println(passConstraintList.size() + " constraints are applicable for this pass");
		}

		// Build set of all detectors available to be added to this pass
		HashSet<DetectorFactory> availableSet = new HashSet<DetectorFactory>();
		availableSet.addAll(detectorSet);
		availableSet.addAll(getUnassignedSet());

		// Build intra-pass constraint graph
		Map<String, DetectorNode> nodeMap = new HashMap<String, DetectorNode>();
		ConstraintGraph constraintGraph = buildConstraintGraph(
			nodeMap, availableSet, passConstraintList);
		if (DEBUG) {
			System.out.println("Pass constraint graph:");
			dumpGraph(constraintGraph);
		}

		// See if any detectors were brought into the pass by an intrapass ordering constraint.
		// Assign them to the pass officially.
		for (DetectorNode node : nodeMap.values()) {
			if (!pass.contains(node.getFactory())) {
				assignToPass(node.getFactory(), pass);
			}
		}

		// Perform DFS, check for cycles
		DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode> dfs =
			new DepthFirstSearch<ConstraintGraph, ConstraintEdge, DetectorNode>(constraintGraph);
		dfs.search();
		if (dfs.containsCycle())
			throw new OrderingConstraintException("Cycle in intra-pass ordering constraints!");

		// Do a topological sort to put the detectors in the pass
		// in the right order.
		for (Iterator<DetectorNode> i = dfs.topologicalSortIterator(); i.hasNext(); ) {
			DetectorNode node = i.next();
			appendToPass(node.getFactory(), pass);
		}

		// Add any detectors not explicitly involved in intra-pass ordering constraints
		// to the end of the pass.
		appendDetectorsToPass(pass.getUnpositionedMembers(), pass);
	}

	private Set<DetectorFactory> getUnassignedSet() {
		Set<DetectorFactory> unassignedSet = new HashSet<DetectorFactory>();
		unassignedSet.addAll(factoryMap.values());
		unassignedSet.removeAll(assignedToPassSet);
		return unassignedSet;
	}

	/**
	 * Make a DetectorFactory a member of an AnalysisPass.
	 */
	private void assignToPass(DetectorFactory factory, AnalysisPass pass) {
		pass.addToPass(factory);
		assignedToPassSet.add(factory);
	}

	/**
	 * Append a DetectorFactory to the end position in an AnalysisPass.
	 * The DetectorFactory must be a member of the pass.
	 */
	private void appendToPass(DetectorFactory factory, AnalysisPass pass)
			throws OrderingConstraintException {
		pass.append(factory);
	}

	private void appendDetectorsToPass(Collection<DetectorFactory> detectorSet, AnalysisPass pass)
			throws OrderingConstraintException {
		DetectorFactory[] unassignedList = detectorSet.toArray(new DetectorFactory[detectorSet.size()]);
		Arrays.sort(unassignedList, new Comparator<DetectorFactory>() {
			public int compare(DetectorFactory a, DetectorFactory b) {
				// Sort first by plugin id...
				int cmp = a.getPlugin().getPluginId().compareTo(b.getPlugin().getPluginId());
				if (cmp != 0)
					return cmp;
				// Then by order specified in plugin descriptor
				return a.getPositionSpecifiedInPluginDescriptor() - b.getPositionSpecifiedInPluginDescriptor();
			}
		});
		for (DetectorFactory factory : unassignedList) {
			appendToPass(factory, pass);
		}
	}

	private void print() {
		int passCount = 0;
		for (Iterator<AnalysisPass> i = passList.iterator(); i.hasNext(); ++passCount) {
			System.out.println("Pass " + passCount);
			AnalysisPass pass = i.next();
			for (Iterator<DetectorFactory> j = pass.iterator(); j.hasNext(); ) {
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
		DetectorFactoryCollection detectorFactoryCollection =
			DetectorFactoryCollection.instance();

		ExecutionPlan execPlan = new ExecutionPlan();

		for (String pluginId : argv) {
			Plugin plugin = detectorFactoryCollection.getPluginById(pluginId);
			if (plugin != null)
				execPlan.addPlugin(plugin);
		}

		execPlan.build();

		System.out.println(execPlan.getNumPasses() + " passes in plan");
		execPlan.print();
	}
}

// vim:ts=4
