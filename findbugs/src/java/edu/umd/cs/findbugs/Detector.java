package edu.umd.cs.findbugs;

public interface Detector {
	public static final int NORMAL_PRIORITY = 0;

	public void visitClassContext(ClassContext classContext);
	public void report();
}

// vim:ts=4
