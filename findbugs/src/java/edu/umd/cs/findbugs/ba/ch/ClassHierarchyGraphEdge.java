package edu.umd.cs.findbugs.ba.ch;

import edu.umd.cs.findbugs.graph.AbstractEdge;

public class ClassHierarchyGraphEdge
		extends AbstractEdge<ClassHierarchyGraphEdge, ClassHierarchyGraphVertex>{

	private ClassHierarchyGraphEdgeType edgeType;
	
	public void setEdgeType(ClassHierarchyGraphEdgeType edgeType) {
		this.edgeType = edgeType;
	}
	
	public ClassHierarchyGraphEdgeType getEdgeType() {
		return edgeType;
	}

	public ClassHierarchyGraphEdge(ClassHierarchyGraphVertex source, ClassHierarchyGraphVertex target) {
		super(source, target);
	}
			
}
