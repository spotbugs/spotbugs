package edu.umd.cs.findbugs;

import java.util.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * An instance of a bug pattern.
 * A BugInstance consists of several parts:
 *
 * <ul>
 * <li> the type, which is a string indicating what kind of bug it is;
 *      used as a key for the FindBugsMessages resource bundle
 * <li> the priority; how likely this instance is to actually be a bug
 * <li> a list of <em>annotations</em>
 * </ul>
 *
 * The annotations describe classes, methods, fields, source locations,
 * and other relevant context information about the bug instance.
 * Every BugInstance must have at least one ClassAnnotation, which
 * describes the class in which the instance was found.  This is the
 * "primary class annotation".
 *
 * <p> BugInstance objects are built up by calling a string of <code>add</code>
 * methods.  (These methods all "return this", so they can be chained).
 *  Some of the add methods are specialized to get information automatically from
 * a BetterVisitor or DismantleBytecode object.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class BugInstance {
	private String type;
	private int priority;
	private int count;
	private ArrayList<BugAnnotation> annotationList;
	private ClassAnnotation primaryClassAnnotation;
	private MethodAnnotation primaryMethodAnnotation;

	/**
	 * Constructor.
	 * @param type the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority;
		this.count = count;
		annotationList = new ArrayList<BugAnnotation>();
		primaryClassAnnotation = null;
	}

	/** Get the bug type. */
	public String getType() {
		return type;
	}

	/** Get the bug priority. */
	public int getPriority() {
		return priority;
	}

	/**
	 * Get the primary class annotation, which indicates where the bug occurs.
	 */
	public ClassAnnotation getPrimaryClass() {
		return primaryClassAnnotation;
	}

	/**
	 * Get the primary method annotation, which indicates where the bug occurs.
	 */
	public MethodAnnotation getPrimaryMethod() {
		return primaryMethodAnnotation;
	}

	/**
	 * Get an Iterator over all bug annotations.
	 */
	public Iterator<BugAnnotation> annotationIterator() {
		return annotationList.iterator();
	}

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 * @param className the name of the class
	 * @return this object
	 */
	public BugInstance addClass(String className) {
		ClassAnnotation classAnnotation = new ClassAnnotation(className);
		if (primaryClassAnnotation == null)
			primaryClassAnnotation = classAnnotation;
		add(classAnnotation);
		return this;
	}

	/**
	 * Add a field annotation.
	 * @param className name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig type signature of the field
	 * @return this object
	 */
	public BugInstance addField(String className, String fieldName, String fieldSig) {
		add(new FieldAnnotation(className, fieldName, fieldSig));
		return this;
	}

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * @param className name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig type signature of the method
	 * @return this object
	 */
	public BugInstance addMethod(String className, String methodName, String methodSig) {
		MethodAnnotation methodAnnotation = new MethodAnnotation(className, methodName, methodSig);
		if (primaryMethodAnnotation == null)
			primaryMethodAnnotation = methodAnnotation;
		add(methodAnnotation);
		return this;
	}

	/**
	 * Add a class annotation for the class that the visitor is currently visiting.
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClass(BetterVisitor visitor) {
		String className = visitor.getBetterClassName();
		addClass(className);
		return this;
	}

	/**
	 * Add a class annotation for the superclass of the class the visitor
	 * is currently visiting.
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addSuperclass(BetterVisitor visitor) {
		String className = visitor.getSuperclassName();
		addClass(className);
		return this;
	}

	/**
	 * Add a method annotation for the method which has been called
	 * by the method currently being visited by given visitor.
	 * Assumes that the visitor has just looked at an invoke instruction
	 * of some kind.
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addCalledMethod(DismantleBytecode visitor) {
		String className = visitor.getBetterClassConstant();
		String methodName = visitor.getNameConstant();
		String methodSig = visitor.getBetterSigConstant();
		addMethod(className, methodName, methodSig);
		return this;
	}

	/**
	 * Add a field annotation for the field which has just been accessed
	 * by the method currently being visited by given visitor.
	 * Assumes that a getfield/putfield or getstatic/putstatic
	 * has just been seen.
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addReferencedField(DismantleBytecode visitor) {
		String className = visitor.getBetterClassConstant();
		String fieldName = visitor.getNameConstant();
		String fieldSig = visitor.getBetterSigConstant();
		addField(className, fieldName, fieldSig);
		return this;
	}

	/**
	 * Add a method annotation for the method which the given visitor is currently visiting.
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addMethod(BetterVisitor visitor) {
		String className = visitor.getBetterClassName();
		String methodName = visitor.getMethodName();
		String methodSig = visitor.getMethodSig();
		addMethod(className, methodName, methodSig);
		return this;
	}

	/**
	 * Add a class annotation and a method annotation for the class and method
	 * which the given visitor is currently visiting.
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClassAndMethod(BetterVisitor visitor) {
		addClass(visitor);
		addMethod(visitor);
		return this;
	}

	private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsMessages");

	/**
	 * Format a string describing this bug instance.
	 * @return the description
	 */
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
