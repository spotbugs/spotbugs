package edu.umd.cs.findbugs;

public interface BugAnnotation {
	public void accept(BugAnnotationVisitor visitor);
	public String format(String key);
}

// vim:ts=4
