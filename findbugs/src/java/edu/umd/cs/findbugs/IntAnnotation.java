package edu.umd.cs.findbugs;

/**
 * Bug annotation class for integer values.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class IntAnnotation implements BugAnnotation {
	private int value;
	private String description;

	/**
	 * Constructor.
	 * @param value the integer value
	 */
	public IntAnnotation(int value) {
		this.value = value;
		this.description = "INT_DEFAULT";
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitIntAnnotation(this);
	}

	public String format(String key) {
		return String.valueOf(value);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
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
	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this});
	}
}

// vim:ts=4
