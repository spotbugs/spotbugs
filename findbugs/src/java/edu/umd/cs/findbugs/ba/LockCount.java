package edu.umd.cs.daveho.ba;

/**
 * Dataflow value for representing the number of locks held.
 *
 * @see LockCountAnalysis
 * @author David Hovemeyer
 */
public class LockCount {
	private int count;

	/** Top value. */
	public static final int TOP = -1;

	/** Bottom value. */
	public static final int BOTTOM = -2;

	/**
	 * Constructor.
	 * @param count the lock count, or the special TOP or BOTTOM values
	 */
	public LockCount(int count) {
		this.count = count;
	}

	/** Get the lock count. */
	public int getCount() { return count; }

	/**
	 * Set the lock count.
	 * @param count the lock count
	 */
	public void setCount(int count) { this.count = count; }

	/** Is this the top value? */
	public boolean isTop() { return count == TOP; }

	/** Is this the bottom value? */
	public boolean isBottom() { return count == BOTTOM; }

	/** Convert to string. */
	public String toString() {
		if (isTop()) return "(TOP)";
		else if (isBottom()) return "(BOTTOM)";
		else return "(" + count + ")";
	}
}

// vim:ts=4
