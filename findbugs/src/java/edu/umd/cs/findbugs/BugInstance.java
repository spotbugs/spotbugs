package edu.umd.cs.findbugs;

import java.util.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;

public class BugInstance {
	private String type;
	private int priority;
	private int count;
	private ArrayList<BugAnnotation> annotationList;
	private ClassAnnotation primaryClassAnnotation;
	private MethodAnnotation primaryMethodAnnotation;

	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority;
		this.count = count;
		annotationList = new ArrayList<BugAnnotation>();
		primaryClassAnnotation = null;
	}

	public String getType() {
		return type;
	}

	public int getPriority() {
		return priority;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public ClassAnnotation getPrimaryClass() {
		return primaryClassAnnotation;
	}

	public MethodAnnotation getPrimaryMethod() {
		return primaryMethodAnnotation;
	}

	public Iterator<BugAnnotation> annotationIterator() {
		return annotationList.iterator();
	}

	public BugInstance addClass(String className, String superclassName, String sourceFile) {
		ClassAnnotation classAnnotation = new ClassAnnotation(className, superclassName, sourceFile);
		if (primaryClassAnnotation == null)
			primaryClassAnnotation = classAnnotation;
		add(classAnnotation);
		return this;
	}

	public BugInstance addField(String className, String fieldName, String fieldSig) {
		add(new FieldAnnotation(className, fieldName, fieldSig));
		return this;
	}

	public BugInstance addMethod(String className, String methodName, String methodSig) {
		MethodAnnotation methodAnnotation = new MethodAnnotation(className, methodName, methodSig);
		if (primaryMethodAnnotation == null)
			primaryMethodAnnotation = methodAnnotation;
		add(methodAnnotation);
		return this;
	}

	public BugInstance addClassAndMethod(BetterVisitor betterVisitor) {
		String className = betterVisitor.getBetterClassName();
		String superclassName = betterVisitor.getSuperclassName();
		String sourceFile = betterVisitor.getSourceFile();
		String methodName = betterVisitor.getMethodName();
		String methodSig = betterVisitor.getMethodSig();

		addClass(className, superclassName, sourceFile);
		addMethod(className, methodName, methodSig);
		return this;
	}

	public String getMessage() {
		// TODO:
		return "";
	}

	private void add(BugAnnotation annotation) {
		annotationList.add(annotation);
	}
}

// vim:ts=4
