package edu.umd.cs.findbugs;

/**
 * Bug annotation class for integer values.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class IntAnnotation implements BugAnnotation {
	private int value;

	/**
	 * Constructor.
	 * @param value the integer value
	 */
	public IntAnnotation(int value) {
		this.value = value;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitIntAnnotation(this);
	}

	public String format(String key) {
		return String.valueOf(value);
	}

	public int hashCode() { return value; }

	public boolean equals(Object o) {
		if (!(o instanceof IntAnnotation))
			return false;
		return value == ((IntAnnotation) o).value;
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof IntAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return value - ((IntAnnotation) o).value;
	}
}

// vim:ts=4
