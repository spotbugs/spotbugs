package edu.umd.cs.findbugs;

public interface BugAnnotation {
	public void accept(BugAnnotationVisitor visitor);
}

// vim:ts=4
