package edu.umd.cs.findbugs;

/**
 * Abstract base class for BugAnnotations describing constructs
 * which are contained in a Java package.  Specifically,
 * this includes classes, methods, and fields.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public abstract class PackageMemberAnnotation implements BugAnnotation {
	protected String className;

	/**
	 * Constructor.
	 * @param className name of the class
	 */
	public PackageMemberAnnotation(String className) {
		this.className = className;
	}

	/**
	 * Get the class name.
	 */
	public final String getClassName() { return className; }

	/**
	 * Get the package name.
	 */
	public final String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			return "";
		else
			return className.substring(0, lastDot);
	}

	/**
	 * Format the annotation.
	 * Note that this version (defined by PackageMemberAnnotation)
	 * only handles the "class" and "package" keys, and calls
	 * formatPackageMember() for all other keys.
	 * @param key the key
	 * @return the formatted annotation
	 */
	public final String format(String key) {
		if (key.equals("class"))
			return className;
		else if (key.equals("package"))
			return getPackageName();
		else
			return formatPackageMember(key);
	}

	/**
	 * Shorten a type name of remove extraneous components.
	 * Candidates for shortening are classes in same package as this annotation and
	 * classes in the <code>java.lang</code> package.
	 */
	protected static String shorten(String pkgName, String typeName) {
		int index = typeName.lastIndexOf('.');
		if (index >= 0 ) {
			String otherPkg = typeName.substring(0, index);
			if (otherPkg.equals(pkgName) || otherPkg.equals("java.lang"))
				typeName = typeName.substring(index + 1);
		}
		return typeName;
	}

	/**
	 * Do default and subclass-specific formatting.
	 * @param key the key specifying how to do the formatting
	 */
	protected abstract String formatPackageMember(String key);
}

// vim:ts=4
