package edu.umd.cs.daveho.ba;

/**
 * A dataflow object representing a single Java stack frame,
 * where each stack slot tracks whether or not it contains
 * the "this" reference.
 *
 * @see ThisValue
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class ThisValueFrame extends Frame<ThisValue> {

	/**
	 * Constructor.
	 * @param numLocals number of locals in the frame
	 */
	public ThisValueFrame(int numLocals) {
		super(numLocals);
	}

	/**
	 * Merge two slot values.
	 * @param a a slot value
	 * @param b another slot value
	 * @return the merged value
	 */
	public ThisValue mergeValues(ThisValue a, ThisValue b) {
		if (a.isTop())
			return b;
		else if (b.isTop())
			return a;
		else if (a.isBottom() || b.isBottom())
			return ThisValue.bottom();
		else if (a.equals(b))
			return a;
		else
			return ThisValue.bottom();
	}

	/**
	 * Return the default value to be placed in uninitialized slots.
	 */
	public ThisValue getDefaultValue() {
		return ThisValue.top();
	}

	/**
	 * Convert to string.
	 */
	public String toString() {
		if (isTop()) return "[TOP]";
		if (isBottom()) return "[BOTTOM]";
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 0; i < getNumSlots(); ++i) {
			buf.append(getValue(i));
		}
		buf.append(']');
		return buf.toString();
	}
}

// vim:ts=4
