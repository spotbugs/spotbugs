package edu.umd.cs.findbugs;

public class ClassAnnotation implements BugAnnotation {
	private String className;
	private String sourceFile;

	public ClassAnnotation(String className, String sourceFile) {
		this.className = className;
		this.sourceFile = sourceFile;
	}

	public String getClassName() {
		return className;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitClassAnnotation(this);
	}

	public String toString() {
		return className;
	}
}

// vim:ts=4
