package edu.umd.cs.findbugs;

public class FieldAnnotation extends PackageMemberAnnotation {
	public String fieldName;
	public String fieldSig;

	public FieldAnnotation(String className, String fieldName, String fieldSig) {
		super(className);
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
	}

	public String getClassName() {
		return className;
	}

	public String getFieldName() {
		return fieldName;
	}

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
