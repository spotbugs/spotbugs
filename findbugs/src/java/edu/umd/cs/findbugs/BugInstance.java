package edu.umd.cs.findbugs;

import java.util.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

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

	public BugInstance addMethod(String className, String methodName, String methodSig) {
		MethodAnnotation methodAnnotation = new MethodAnnotation(className, methodName, methodSig);
		if (primaryMethodAnnotation == null)
			primaryMethodAnnotation = methodAnnotation;
		add(methodAnnotation);
		return this;
	}

	public BugInstance addClass(BetterVisitor visitor) {
		String className = visitor.getBetterClassName();
		String sourceFile = visitor.getSourceFile();
		addClass(className, sourceFile);
		return this;
	}

	public BugInstance addSuperclass(BetterVisitor visitor) {
		String className = visitor.getSuperclassName();
		String sourceFile = "<unknown>";
		addClass(className, sourceFile);
		return this;
	}

	public BugInstance addCalledMethod(DismantleBytecode visitor) {
		String className = visitor.getBetterClassConstant();
		String methodName = visitor.getNameConstant();
		String methodSig = visitor.getBetterSigConstant();
		addMethod(className, methodName, methodSig);
		return this;
	}

	public BugInstance addMethod(BetterVisitor visitor) {
		String className = visitor.getBetterClassName();
		String methodName = visitor.getMethodName();
		String methodSig = visitor.getMethodSig();
		addMethod(className, methodName, methodSig);
		return this;
	}

	public BugInstance addClassAndMethod(BetterVisitor visitor) {
		addClass(visitor);
		addMethod(visitor);
		return this;
	}

	private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsMessages");

	public String getMessage() {
		String pattern = resourceBundle.getString(type);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format((BugAnnotation[]) annotationList.toArray(new BugAnnotation[0]));
	}

	private void add(BugAnnotation annotation) {
		annotationList.add(annotation);
	}
}

// vim:ts=4
