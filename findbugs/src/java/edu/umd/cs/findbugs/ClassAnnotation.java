package edu.umd.cs.findbugs;

public class ClassAnnotation implements BugAnnotation {
	private String className;
	private String superclassName;
	private String sourceFile;

	public ClassAnnotation(String className, String superclassName, String sourceFile) {
		this.className = className;
		this.superclassName = superclassName;
		this.sourceFile = sourceFile;
	}

	public String getClassName() {
		return className;
	}

	public String getSuperclassName() {
		return superclassName;
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
