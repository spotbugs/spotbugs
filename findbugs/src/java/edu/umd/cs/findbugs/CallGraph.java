/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.IdentityHashMap;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.graph.AbstractGraph;

public class CallGraph extends AbstractGraph<CallGraphEdge, CallGraphNode> {
	private IdentityHashMap<Method, CallGraphNode> methodToNodeMap;

	public CallGraph() {
		this.methodToNodeMap = new IdentityHashMap<Method, CallGraphNode>();
	}

	public CallGraphEdge createEdge(CallGraphNode source, CallGraphNode target, CallSite callSite) {
		CallGraphEdge edge = createEdge(source, target);
		edge.setCallSite(callSite);
		return edge;
	}

	public CallGraphNode addNode(Method method) {
		CallGraphNode node = new CallGraphNode();
		addVertex(node);
		node.setMethod(method);
		methodToNodeMap.put(method, node);
		return node;
	}

	public CallGraphNode getNodeForMethod(Method method) {
		return methodToNodeMap.get(method);
	}

	@Override
	protected CallGraphEdge allocateEdge(CallGraphNode source, CallGraphNode target) {
		return new CallGraphEdge(source, target);
	}
}

// vim:ts=4
