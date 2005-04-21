package edu.umd.cs.findbugs.ba.ch;

public class ClassHierarchyGraphEdgeType {
	private String type;
	
	private ClassHierarchyGraphEdgeType(String type) {
		this.type = type;
	}
	
	public static final ClassHierarchyGraphEdgeType CLASS_EDGE =
		new ClassHierarchyGraphEdgeType("CLASS_EDGE");
	
	public static final ClassHierarchyGraphEdgeType INTERFACE_EDGE =
		new ClassHierarchyGraphEdgeType("INTERFACE_EDGE");
	
	public String toString() {
		return type;
	}
}
