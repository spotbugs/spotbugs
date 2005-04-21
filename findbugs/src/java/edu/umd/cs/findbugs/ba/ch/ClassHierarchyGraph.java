package edu.umd.cs.findbugs.ba.ch;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.graph.AbstractGraph;

public class ClassHierarchyGraph
		extends AbstractGraph<ClassHierarchyGraphEdge, ClassHierarchyGraphVertex> {
			
	private Map<String, ClassHierarchyGraphVertex> vertexMap;
	
	public ClassHierarchyGraph() {
		this.vertexMap = new HashMap<String, ClassHierarchyGraphVertex>();
	}
			
	public ClassHierarchyGraphVertex getVertex(String className) {
		ClassHierarchyGraphVertex vertex = vertexMap.get(className);
		if (vertex == null) {
			vertex = new ClassHierarchyGraphVertex(className);
			vertexMap.put(className, vertex);
		}
		return vertex;
	}

	public ClassHierarchyGraphEdge addInheritanceEdge(
			ClassHierarchyGraphVertex subtype,
			ClassHierarchyGraphVertex supertype,
			ClassHierarchyGraphEdgeType edgeType) {
		ClassHierarchyGraphEdge edge = createEdge(subtype, supertype);
		edge.setEdgeType(edgeType);
		return edge;
	}
	
	//@Override
	protected ClassHierarchyGraphEdge allocateEdge(ClassHierarchyGraphVertex source, ClassHierarchyGraphVertex target) {
		return new ClassHierarchyGraphEdge(source, target);
	}

}
