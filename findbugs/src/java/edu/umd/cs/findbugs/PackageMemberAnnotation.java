package edu.umd.cs.findbugs;

public abstract class PackageMemberAnnotation implements BugAnnotation {
	protected String className;

	public PackageMemberAnnotation(String className) {
		this.className = className;
	}

	public String getClassName() { return className; }

	public String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			return "";
		else
			return className.substring(0, lastDot);
	}

	public final String format(String key) {
		if (key.equals("class"))
			return className;
		else if (key.equals("package"))
			return getPackageName();
		else
			return formatPackageMember(key);
	}

	protected static String shorten(String pkgName, String typeName) {
		if (typeName.startsWith(pkgName + "."))
			typeName = typeName.substring(pkgName.length() + 1);
		else if (typeName.startsWith("java.lang."))
			typeName = typeName.substring("java.lang.".length());
		return typeName;
	}

	protected abstract String formatPackageMember(String key);
}

// vim:ts=4
