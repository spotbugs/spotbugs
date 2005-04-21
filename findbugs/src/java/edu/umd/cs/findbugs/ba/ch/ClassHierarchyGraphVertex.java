package edu.umd.cs.findbugs.ba.ch;

import edu.umd.cs.findbugs.graph.AbstractVertex;

public class ClassHierarchyGraphVertex
		extends AbstractVertex<ClassHierarchyGraphEdge, ClassHierarchyGraphVertex> {

	private String className;
	
	public ClassHierarchyGraphVertex(String className) {
		this.className = className;
	}
	
	public String getClassName() {
		return className;
	}
	
}
