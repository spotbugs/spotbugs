package edu.umd.cs.daveho.ba;

/**
 * Extended type codes used by StackAndLocalTypes and StackAndLocalTypeAnalysis
 * for typing locals and stack values used in Java bytecode.
 * @see TypedFrame
 * @see FrameTypeAnalysis
 * @author David Hovemeyer
 */
public interface ExtendedTypes {
	// FIXME: these values depend on those in org.apache.bcel.Constants.
	// They need to be distinct from all type codes defined there.
	// It would be nice if BCEL provided built-in functionality for
	// some or all of these.

	/**
	 * Special type code for the "Top" type in the lattice.
	 */
	public static final byte T_TOP = 17;

	/**
	 * Special type that represents the value store in
	 * local <i>n+1</i> when a long value is stored in
	 * local <i>n</i>.
	 */
	public static final byte T_LONG_EXTRA = 18;

	/**
	 * Special type that represents the value store in
	 * local <i>n+1</i> when a double value is stored in
	 * local <i>n</i>.
	 */
	public static final byte T_DOUBLE_EXTRA = 19;

	/**
	 * Special type code for the "Bottom" type in the lattice.
	 */
	public static final byte T_BOTTOM = 20;

	/**
	 * Special type code for the "Null" type.
	 * This is a type which is higher in the lattice than any object type,
	 * but lower than the overall Top type.  It represents the type
	 * of the null value, which may logically be merged with any
	 * object type without loss of information.
	 */
	public static final byte T_NULL = 21;
}

// vim:ts=4
