package edu.umd.cs.findbugs.ba.ch;

public class ClassHierarchyGraphVertexType {
	private String type;
	
	private ClassHierarchyGraphVertexType(String type) {
		this.type = type;
	}
	
	public static final ClassHierarchyGraphVertexType CLASS_VERTEX =
		new ClassHierarchyGraphVertexType("CLASS_VERTEX");
	
	public static final ClassHierarchyGraphVertexType INTERFACE_VERTEX =
		new ClassHierarchyGraphVertexType("INTERFACE_VERTEX");
	
	public String toString() {
		return type;
	}
}
