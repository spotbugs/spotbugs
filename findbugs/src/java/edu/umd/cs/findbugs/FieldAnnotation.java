package edu.umd.cs.findbugs;

public class FieldAnnotation implements BugAnnotation {
	public String className;
	public String fieldName;
	public String fieldSig;

	public FieldAnnotation(String className, String fieldName, String fieldSig) {
		this.className = className;
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

	public String toString() {
		return className + "." + fieldName;
	}
}

// vim:ts=4
