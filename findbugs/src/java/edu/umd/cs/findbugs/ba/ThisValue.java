package edu.umd.cs.daveho.ba;

/**
 * A dataflow value class for representing whether or not a slot
 * (local or stack location) in the Java stack frame contains the
 * "this" pointer.
 *
 * @see ThisValueAnalysis
 * @see ThisValueFrame
 * @author David Hovemeyer
 */
public class ThisValue {
	/** Top value - uninitialized location. */
	public static final int TOP = 0;

	/** Bottom value - might or might not contain "this". */
	public static final int BOTTOM = 1;

	/** Definitely contains "this". */
	public static final int TRUE = 2;

	/**
     * Most likely does not contain "this".
	 * The value <em>could</em> still contain "this" through a heap assignment.
     */
	public static final int FALSE = 3;

	/** Single instance of top value. */
	private final static ThisValue top = new ThisValue(TOP);

	/** Single instance of bottom value. */
	private final static ThisValue bottom = new ThisValue(BOTTOM);

	/** Single instance of "contains this" value. */
	private final static ThisValue thisValue = new ThisValue(TRUE);

	/** Single instance of "does not contain this" value. */
	private final static ThisValue notThisValue = new ThisValue(FALSE);

	/** Value of the object. */
	private int value;

	/** Constructor. */
	private ThisValue(int value) {
		this.value = value;
	}

	/** Is this object the top element? */
	public boolean isTop() { return value == TOP; }

	/** Is this object the bottom element? */
	public boolean isBottom() { return value == BOTTOM; }

	/** Is this object the "contains this" element? */
	public boolean isThis() { return value == TRUE; }

	/** Is this object the "does not contain this" element? */
	public boolean isNotThis() { return value == FALSE; }

	/**
	 * Compare to another object.
	 * Because all of the instances are singletons,
	 * this is just a comparison for reference equality.
	 * @param o the other object
	 */
	public boolean equals(Object o) { return this == o; }

	/** Get the single instance of the top value. */
	public static ThisValue top() { return top; }

	/** Get the single instance of the bottom value. */
	public static ThisValue bottom() { return bottom; }

	/** Get the single instance of the "contains this" value. */
	public static ThisValue thisValue() { return thisValue; }

	/** Get the single instance of the (most likely) "does not contain this" value. */
	public static ThisValue notThisValue() { return notThisValue; }

	/**
	 * Convert to string.
	 */
	public String toString() {
		switch(value) {
		case TOP: return "T";
		case BOTTOM: return "B";
		case TRUE: return "t";
		case FALSE: return "-";
		default: return "?";
		}
	}
}

// vim:ts=4
