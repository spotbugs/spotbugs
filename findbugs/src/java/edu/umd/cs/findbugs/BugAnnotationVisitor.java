package edu.umd.cs.findbugs;

public interface BugAnnotationVisitor {
	public void visitClassAnnotation(ClassAnnotation classAnnotation);
	public void visitFieldAnnotation(FieldAnnotation fieldAnnotation);
	public void visitMethodAnnotation(MethodAnnotation methodAnnotation);
	public void visitIntAnnotation(IntAnnotation intAnnotation);
}

// vim:ts=4
