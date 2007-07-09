/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.ch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Class for performing class hierarchy queries.
 * Does <em>not</em> require JavaClass objects to be in memory.
 * Instead, uses XClass objects.
 * 
 * @author David Hovemeyer
 */
public class Subtypes2 {
	public static final boolean ENABLE_SUBTYPES2 = SystemProperties.getBoolean("findbugs.subtypes2");
	private InheritanceGraph graph;
	private Map<ClassDescriptor, ClassVertex> classDescriptorToVertexMap;

	/**
	 * Constructor.
	 */
	public Subtypes2() {
		this.graph = new InheritanceGraph();
		this.classDescriptorToVertexMap = new HashMap<ClassDescriptor, ClassVertex>();
	}

	/**
	 * Add a class or interface, and its transitive supertypes, to the inheritance graph.
	 * 
	 * @param xclass XClass to add to the inheritance graph
	 */
	public void addClass(XClass xclass) {
		if (xclass == null) {
			throw new IllegalStateException();
		}

		LinkedList<XClass> workList = new LinkedList<XClass>();
		workList.add(xclass);

		while (!workList.isEmpty()) {
			XClass work = workList.removeFirst();
			ClassVertex vertex = classDescriptorToVertexMap.get(work.getClassDescriptor());
			if (vertex != null && vertex.isFinished()) {
				// This class has already been processed.
				continue;
			}

			if (vertex == null) {
				vertex = new ClassVertex(work);
				classDescriptorToVertexMap.put(work.getClassDescriptor(), vertex);
			}

			addSupertypeEdges(vertex, workList);

			vertex.setFinished(true);
		}
	}

	private void addSupertypeEdges(ClassVertex vertex, LinkedList<XClass> workList) {
		XClass xclass = vertex.getXClass();

		addInheritanceEdge(vertex, xclass.getSuperclassDescriptor(), workList);
		for (ClassDescriptor ifaceDesc : xclass.getInterfaceDescriptorList()) {
			addInheritanceEdge(vertex, ifaceDesc, workList);
		}
	}

	private void addInheritanceEdge(ClassVertex vertex, ClassDescriptor superclassDescriptor, LinkedList<XClass> workList) {
		if (superclassDescriptor == null) {
			return;
		}

		XClass superClass = AnalysisContext.currentXFactory().getXClass(superclassDescriptor);
		if (superClass == null) {
			// Hmm...inheritance graph will be incomplete.  Oh well.
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(superclassDescriptor);
			return;
		}

		ClassVertex superVertex = classDescriptorToVertexMap.get(superclassDescriptor);
		if (superVertex == null) {
			// Seeing this class for the first time

			superVertex = new ClassVertex(superClass);
			classDescriptorToVertexMap.put(superclassDescriptor, superVertex);
			workList.addLast(superClass); // recursively process supertype
		}

		InheritanceEdge edge = graph.createEdge(vertex, superVertex);
	}
}
