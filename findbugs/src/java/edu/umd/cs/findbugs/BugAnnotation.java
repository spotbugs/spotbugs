package edu.umd.cs.findbugs;

/**
 * An object providing context information about a particular BugInstance.
 * @see BugInstance
 * @author David Hovemeyer
 */
public interface BugAnnotation extends Comparable<BugAnnotation> {
	/**
	 * Accept a BugAnnotationVisitor.
	 * @param visitor the visitor to accept
	 */
	public void accept(BugAnnotationVisitor visitor);

	/**
	 * Format the annotation as a String.
	 * The given key specifies additional information about how the annotation should
	 * be formatted.  If the key is empty, then the "default" format will be used.
	 * @param key how the annotation should be formatted
	 */
	public String format(String key);
}

// vim:ts=4
