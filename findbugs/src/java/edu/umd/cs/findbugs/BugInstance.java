package edu.umd.cs.findbugs;

import java.util.*;

public class BugInstance {
	private String type;
	private int priority;
	private ArrayList<BugAnnotation> annotationList;
	private ClassAnnotation primaryClassAnnotation;

	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority;
		annotationList = new ArrayList<BugAnnotation>();
		primaryClassAnnotation = null;
	}

	public String getType() {
		return type;
	}

	public int getPriority() {
		return priority;
	}

	public Iterator<BugAnnotation> annotationIterator() {
		return annotationList.iterator();
	}

	public BugInstance addClass(String className, String sourceFile) {
		ClassAnnotation classAnnotation = new ClassAnnotation(className, sourceFile);
		if (primaryClassAnnotation == null)
			primaryClassAnnotation = classAnnotation;
		add(classAnnotation);
		return this;
	}

	public BugInstance addField(String className, String fieldName, String fieldSig) {
		add(new FieldAnnotation(className, fieldName, fieldSig));
		return this;
	}

	public ClassAnnotation getPrimaryClass() {
		return primaryClassAnnotation;
	}

	private void add(BugAnnotation annotation) {
		annotationList.add(annotation);
	}
}

// vim:ts=4
