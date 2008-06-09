/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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
import java.util.Map;

import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.graph.AbstractGraph;

/**
 * Class representing the interprocedural call graph.
 * Vertices represent methods.
 * Edges represent method calls.
 * 
 * @author David Hovemeyer
 */
public class InterproceduralCallGraph extends AbstractGraph<InterproceduralCallGraphEdge, InterproceduralCallGraphVertex> {

	private Map<MethodDescriptor, InterproceduralCallGraphVertex> methodDescToVertexMap;

	/**
	 * Constructor.
	 */
	public InterproceduralCallGraph() {
		this.methodDescToVertexMap = new HashMap<MethodDescriptor, InterproceduralCallGraphVertex>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.graph.AbstractGraph#addVertex(edu.umd.cs.findbugs.graph.AbstractVertex)
	 */
	@Override
	public void addVertex(InterproceduralCallGraphVertex v) {
		super.addVertex(v);
		methodDescToVertexMap.put(v.getXmethod().getMethodDescriptor(), v);
	}

	/**
	 * Look up vertex corresponding to given method.
	 * 
	 * @param methodDesc a MethodDescriptor specifying a method
	 * @return the InterproceduralCallGraphVertex representing that method, or null
	 *         if no such vertex exists
	 */
	public InterproceduralCallGraphVertex lookupVertex(MethodDescriptor methodDesc) {
		return methodDescToVertexMap.get(methodDesc);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.graph.AbstractGraph#allocateEdge(edu.umd.cs.findbugs.graph.AbstractVertex, edu.umd.cs.findbugs.graph.AbstractVertex)
	 */
	@Override
	protected InterproceduralCallGraphEdge allocateEdge(InterproceduralCallGraphVertex source, InterproceduralCallGraphVertex target) {
		return new InterproceduralCallGraphEdge(source, target);
	}
}
