package edu.umd.cs.daveho.ba;

/**
 * Dataflow fact to represent the depth of the Java operand stack.
 * @see StackDepthAnalysis
 */
public class StackDepth {
	private int depth;

	/**
	 * Constructor.
	 * @param depth the stack depth
	 */
	public StackDepth(int depth) {
		this.depth = depth;
	}

	/** Get the stack depth. */
	public int getDepth() { return depth; }

	/** Set the stack depth. */
	public void setDepth(int depth) { this.depth = depth; }

	public String toString() {
		if (getDepth() == StackDepthAnalysis.TOP)
			return "<uninitialized>";
		else if (getDepth() == StackDepthAnalysis.BOTTOM)
			return "<unknown, likely error>";
		else
			return String.valueOf(depth);
	}
}

// vim:ts=4
