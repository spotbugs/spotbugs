package edu.umd.cs.findbugs;

public interface BugAnnotationVisitor {
	public void visitClassAnnotation(ClassAnnotation classAnnotation);
	public void visitFieldAnnotation(FieldAnnotation fieldAnnotation);
	public void visitMethodAnnotation(MethodAnnotation methodAnnotation);
}

// vim:ts=4
