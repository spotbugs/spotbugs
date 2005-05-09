package edu.umd.cs.findbugs.ba;

public interface PackageMember extends Comparable<PackageMember>  {

	/**
	 * Get the name of the field.
	 */
	public abstract String getName();

	/**
	 * Get the name of the class the field is defined in.
	 */
	public abstract String getClassName();

	/**
	 * Get the signature representing the field's type.
	 */
	public abstract String getSignature();

	/**
	 * Get the field's access flags.
	 */
	public abstract int getAccessFlags();

	/**
	 * Is this a static field?
	 */
	public abstract boolean isStatic();

	/**
	 * Is this a final field?
	 */
	public abstract boolean isFinal();

	/**
	 * Is this a public field?
	 */
	public abstract boolean isPublic();

}