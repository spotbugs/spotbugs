package edu.umd.cs.findbugs;

/**
 * A BugAnnotation specifying a particular field in particular class.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class FieldAnnotation extends PackageMemberAnnotation {
	public String fieldName;
	public String fieldSig;

	/**
	 * Constructor.
	 * @param className the name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig the type signature of the field
	 */
	public FieldAnnotation(String className, String fieldName, String fieldSig) {
		super(className);
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
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
}

// vim:ts=4
