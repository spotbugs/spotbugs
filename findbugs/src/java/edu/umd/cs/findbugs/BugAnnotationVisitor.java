package edu.umd.cs.findbugs;

public interface BugAnnotationVisitor {
	public void visitFieldAnnotation(FieldAnnotation fieldAnnotation);
	public void visitClassAnnotation(ClassAnnotation classAnnotation);
}

// vim:ts=4
