package edu.umd.cs.findbugs;

public class ClassAnnotation extends PackageMemberAnnotation {
	private String sourceFile;

	public ClassAnnotation(String className, String sourceFile) {
		super(className);
		this.sourceFile = sourceFile;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitClassAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return className;
		else
			throw new IllegalArgumentException("unknown key " + key);
	}
}

// vim:ts=4
