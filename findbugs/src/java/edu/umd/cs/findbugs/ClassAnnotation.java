package edu.umd.cs.findbugs;

/**
 * A BugAnnotation object specifying a Java class involved in the bug.
 *
 * @see BugAnnotation
 * @see BugInstance
 * @author David Hovemeyer
 */
public class ClassAnnotation extends PackageMemberAnnotation {
	private String sourceFile;

	/**
	 * Constructor.
	 * @param className the name of the class
	 * @param sourceFile the name of the source file where the class is defined
	 */
	public ClassAnnotation(String className, String sourceFile) {
		super(className);
		this.sourceFile = sourceFile;
	}

	/**
	 * Get the source file.
	 */
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
