package edu.umd.cs.findbugs;

/**
 * A BugAnnotation object specifying a Java class involved in the bug.
 *
 * @see BugAnnotation
 * @see BugInstance
 * @author David Hovemeyer
 */
public class ClassAnnotation extends PackageMemberAnnotation {
	/**
	 * Constructor.
	 * @param className the name of the class
	 * @param sourceFile the name of the source file where the class is defined
	 */
	public ClassAnnotation(String className) {
		super(className);
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

	public int hashCode() {
		return className.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof PackageMemberAnnotation))
			return false;
		PackageMemberAnnotation other = (PackageMemberAnnotation) o;
		return className.equals(other.className);
	}

	public int compareTo(Object o) {
		PackageMemberAnnotation other = (PackageMemberAnnotation) o;
		return className.compareTo(other.className);
	}
}

// vim:ts=4
