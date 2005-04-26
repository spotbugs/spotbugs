package edu.umd.cs.findbugs.ba.ch;

import edu.umd.cs.findbugs.graph.AbstractVertex;

public class ClassHierarchyGraphVertex
		extends AbstractVertex<ClassHierarchyGraphEdge, ClassHierarchyGraphVertex> {

	private String className;
	private ClassHierarchyGraphVertexType vertexType;
	private boolean finished;
	private boolean application;
	private boolean missing;
	
	public ClassHierarchyGraphVertex(String className, ClassHierarchyGraphVertexType vertexType) {
		this.className = className;
		this.vertexType = vertexType;
	}
	
	public String getClassName() {
		return className;
	}
	
	public ClassHierarchyGraphVertexType getVertexType() {
		return vertexType;
	}
	
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public void setApplication(boolean application) {
		this.application = application;
	}
	
	public boolean isApplication() {
		return application;
	}
	
	public void setMissing(boolean missing) {
		this.missing = missing;
	}
	
	public boolean isMissing() {
		return missing;
	}
	
}
