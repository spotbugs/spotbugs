package edu.umd.cs.findbugs;

import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * A BugAnnotation specifying a particular field in particular class.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class FieldAnnotation extends PackageMemberAnnotation implements Comparable {
	private String fieldName;
	private String fieldSig;
	private boolean isStatic;

	/**
	 * Constructor.
	 * @param className the name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig the type signature of the field
	 */
	public FieldAnnotation(String className, String fieldName, String fieldSig, boolean isStatic) {
		super(className);
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
		this.isStatic = isStatic;
	}

	/**
	 * Factory method. Class name, field name, and field signatures are taken from
	 * the given visitor, which is visiting the field.
	 * @param visitor the visitor which is visiting the field
	 * @return the FieldAnnotation object
	 */
	public static FieldAnnotation fromVisitedField(BetterVisitor visitor) {
		return new FieldAnnotation(visitor.getBetterClassName(), visitor.getFieldName(), visitor.getFieldSig(),
			visitor.getFieldIsStatic());
	}

	/**
	 * Factory method. Class name, field name, and field signatures are taken from
	 * the given visitor, which is visiting a reference to the field
	 * (i.e., a getfield or getstatic instruction).
	 * @param visitor the visitor which is visiting the field reference
	 * @return the FieldAnnotation object
	 */
	public static FieldAnnotation fromReferencedField(DismantleBytecode visitor) {
		return new FieldAnnotation(visitor.getBetterClassConstant(), visitor.getNameConstant(), visitor.getSigConstant(),
			visitor.getRefFieldIsStatic());
	}

	/**
	 * Get the field name.
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Get the type signature of the field.
	 */
	public String getFieldSignature() {
		return fieldSig;
	}

	/**
	 * Return whether or not the field is static.
	 */
	public boolean isStatic() {
		return isStatic;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitFieldAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return className + "." + fieldName;
		else if (key.equals("fullField")) {
			String pkgName = getPackageName();
			SignatureConverter converter = new SignatureConverter(fieldSig);
			StringBuffer result = new StringBuffer();
			result.append(converter.parseNext());
			result.append(' ');
			result.append(className);
			result.append('.');
			result.append(fieldName);
			return result.toString();
		} else
			throw new IllegalArgumentException("unknown key " + key);
	}

	public String toString() {
		return format("");
	}

	public int hashCode() {
		return className.hashCode() + fieldName.hashCode() + fieldSig.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FieldAnnotation))
			return false;
		FieldAnnotation other = (FieldAnnotation) o;
		return className.equals(other.className)
			&& fieldName.equals(other.fieldName)
			&& fieldSig.equals(other.fieldSig);
	}

	public int compareTo(Object o) {
		FieldAnnotation other = (FieldAnnotation) o;
		int cmp;
		cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = fieldName.compareTo(other.fieldName);
		if (cmp != 0)
			return cmp;
		return fieldSig.compareTo(other.fieldSig);
	}
}

// vim:ts=4
