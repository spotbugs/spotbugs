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
	
	public ClassHierarchyGraphVertex lookupVertex(String className) {
		return vertexMap.get(className);
	}
			
	public ClassHierarchyGraphVertex addVertex(
			String className,
			ClassHierarchyGraphVertexType vertexType) {
		ClassHierarchyGraphVertex vertex = lookupVertex(className);
		if (vertex == null) {
			vertex = new ClassHierarchyGraphVertex(className, vertexType);
			vertexMap.put(className, vertex);
		}
		return vertex;
	}
	
	//@Override
	protected ClassHierarchyGraphEdge allocateEdge(ClassHierarchyGraphVertex source, ClassHierarchyGraphVertex target) {
		return new ClassHierarchyGraphEdge(source, target);
	}

}
