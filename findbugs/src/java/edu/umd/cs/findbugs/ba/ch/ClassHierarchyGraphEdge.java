package edu.umd.cs.findbugs.ba.ch;

import edu.umd.cs.findbugs.graph.AbstractEdge;

public class ClassHierarchyGraphEdge
		extends AbstractEdge<ClassHierarchyGraphEdge, ClassHierarchyGraphVertex>{

	public ClassHierarchyGraphEdge(ClassHierarchyGraphVertex source, ClassHierarchyGraphVertex target) {
		super(source, target);
	}
			
}
