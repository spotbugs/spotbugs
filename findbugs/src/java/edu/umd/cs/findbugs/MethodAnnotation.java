package edu.umd.cs.findbugs;

public class MethodAnnotation implements BugAnnotation {
	private String className;
	private String methodName;
	private String methodSig;

	public MethodAnnotation(String className, String methodName, String methodSig) {
		this.className = className;
		this.methodName = methodName;
		this.methodSig = methodSig;
	}

	public String getClassName() { return className; }

	public String getMethodName() { return methodName; }

	public String getMethodSignature() { return methodSig; }

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	public String toString() {
		// TODO: Convert to "nice" representation
		return className + "." + methodName + ":" + methodSig;
	}
}

// vim:ts=4
