package edu.umd.cs.findbugs;

import java.util.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

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
public class BugInstance implements Comparable {
	private String type;
	private int priority;
	private ArrayList<BugAnnotation> annotationList;
	private ClassAnnotation primaryClassAnnotation;
	private MethodAnnotation primaryMethodAnnotation;
	private int cachedHashCode;

	/**
	 * This value is used to indicate that the cached hashcode
	 * is invalid, and should be recomputed.
	 */
	private static final int INVALID_HASH_CODE = 0;

	/**
	 * Constructor.
	 * @param type the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority;
		annotationList = new ArrayList<BugAnnotation>();
		primaryClassAnnotation = null;
		cachedHashCode = INVALID_HASH_CODE;
	}

	/* ----------------------------------------------------------------------
	 * Accessors
	 * ---------------------------------------------------------------------- */

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

	/* ----------------------------------------------------------------------
	 * Combined annotation adders
	 * ---------------------------------------------------------------------- */

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

	/* ----------------------------------------------------------------------
	 * Class annotation adders
	 * ---------------------------------------------------------------------- */

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
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 * @param jclass the JavaClass object for the class
	 * @return this object
	 */
	public BugInstance addClass(JavaClass jclass) {
		addClass(jclass.getClassName());
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

	/* ----------------------------------------------------------------------
	 * Field annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a field annotation.
	 * @param className name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig type signature of the field
	 * @param isStatic whether or not the field is static
	 * @return this object
	 */
	public BugInstance addField(String className, String fieldName, String fieldSig, boolean isStatic) {
		addField(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
		return this;
	}

	/**
	 * Add a field annotation
	 * @param fieldAnnotation the field annotation
	 * @return this object
	 */
	public BugInstance addField(FieldAnnotation fieldAnnotation) {
		add(fieldAnnotation);
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
		FieldAnnotation f = FieldAnnotation.fromReferencedField(visitor);
		addField(f);
		return this;
	}

	/**
	 * Add a field annotation for the field which is being visited by
	 * given visitor.
	 * @param visitor the visitor
	 * @return this object
	 */
	public BugInstance addVisitedField(BetterVisitor visitor) {
		FieldAnnotation f = FieldAnnotation.fromVisitedField(visitor);
		addField(f);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Method annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * @param className name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig type signature of the method
	 * @return this object
	 */
	public BugInstance addMethod(String className, String methodName, String methodSig) {
		addMethod(new MethodAnnotation(className, methodName, methodSig));
		return this;
	}

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 * @param methodGen the MethodGen object for the method
	 * @return this object
	 */
	public BugInstance addMethod(MethodGen methodGen) {
		MethodAnnotation methodAnnotation =
			new MethodAnnotation(methodGen.getClassName(), methodGen.getName(), methodGen.getSignature());
		addMethod(methodAnnotation);
		addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(methodGen));
		return this;
	}

	/**
	 * Add a method annotation for the method which the given visitor is currently visiting.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addMethod(BetterVisitor visitor) {
		MethodAnnotation methodAnnotation = MethodAnnotation.fromVisitedMethod(visitor);
		addMethod(methodAnnotation);
		addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(visitor));
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
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 * @param methodAnnotation the method annotation
	 * @return this object
	 */
	public BugInstance addMethod(MethodAnnotation methodAnnotation) {
		if (primaryMethodAnnotation == null)
			primaryMethodAnnotation = methodAnnotation;
		add(methodAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Integer annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add an integer annotation.
	 * @param value the integer value
	 * @return this object
	 */
	public BugInstance addInt(int value) {
		add(new IntAnnotation(value));
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Source line annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a source line annotation.
	 * @param sourceLine the source line annotation
	 * @return this object
	 */
	public BugInstance addSourceLine(SourceLineAnnotation sourceLine) {
		add(sourceLine);
		return this;
	}

	/**
	 * Add a source line annotation for instruction whose PC is given
	 * in the method that the given visitor is currently visiting.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 * @param visitor a BetterVisitor that is currently visiting the method
	 * @param pc bytecode offset of the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(BetterVisitor visitor, int pc) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor, pc);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for the given instruction in the given method.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 * @param methodGen the method being visited
	 * @param handle the InstructionHandle containing the visited instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(MethodGen methodGen, InstructionHandle handle) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(methodGen, handle);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation describing the
	 * source line numbers for a range of instructions in the method being
	 * visited by the given visitor.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC the bytecode offset of the end instruction in the range
	 * @return this object
	 */
	public BugInstance addSourceLineRange(BetterVisitor visitor, int startPC, int endPC) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(visitor, startPC, endPC);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for instruction currently being visited
	 * by given visitor.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 * @param visitor a DismantleBytecode visitor that is currently visiting the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(DismantleBytecode visitor) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Formatting support
	 * ---------------------------------------------------------------------- */

	/**
	 * Format a string describing this bug instance.
	 * @return the description
	 */
	public String getMessage() {
		String pattern = I18N.instance().getMessage(type);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format((BugAnnotation[]) annotationList.toArray(new BugAnnotation[0]));
	}

	/**
	 * Add a description to the most recently added bug annotation.
	 * @param description the description to add
	 * @return this object
	 */
	public BugInstance describe(String description) {
		annotationList.get(annotationList.size() - 1).setDescription(description);
		return this;
	}

	/**
	 * Convert to String.
	 * This method returns the "short" message describing the bug,
	 * as opposed to the longer format returned by getMessage().
	 * The short format is appropriate for the tree view in a GUI,
	 * where the annotations are listed separately as part of the overall
	 * bug instance.
	 */
	public String toString() {
		return I18N.instance().getShortMessage(type);
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private void add(BugAnnotation annotation) {
		if (annotation == null)
			throw new IllegalStateException("Missing BugAnnotation!");
		// This object is being modified, so the cached hashcode
		// must be invalidated
		cachedHashCode = INVALID_HASH_CODE;
		annotationList.add(annotation);
	}

	private void addSourceLinesForMethod(MethodAnnotation methodAnnotation, SourceLineAnnotation sourceLineAnnotation) {
		if (sourceLineAnnotation != null) {
			// Note: we don't add the source line annotation directly to
			// the bug instance.  Instead, we stash it in the MethodAnnotation.
			// It is much more useful there, and it would just be distracting
			// if it were displayed in the UI, since it would compete for attention
			// with the actual bug location source line annotation (which is much
			// more important and interesting).
			methodAnnotation.setSourceLines(sourceLineAnnotation);
		}
	}

	public int hashCode() {
		if (cachedHashCode == INVALID_HASH_CODE) {
			int hashcode = type.hashCode() + priority;
			Iterator<BugAnnotation> i = annotationIterator();
			while (i.hasNext())
				hashcode += i.next().hashCode();

			cachedHashCode = hashcode;
		}

		return cachedHashCode;
	}

	public boolean equals(Object o) {
		if (!(o instanceof BugInstance))
			return false;
		BugInstance other = (BugInstance) o;
		if (!type.equals(other.type) || priority != other.priority)
			return false;
		if (annotationList.size() != other.annotationList.size())
			return false;
		int numAnnotations = annotationList.size();
		for (int i = 0; i < numAnnotations; ++i) {
			BugAnnotation lhs = annotationList.get(i);
			BugAnnotation rhs = other.annotationList.get(i);
			if (!lhs.equals(rhs))
				return false;
		}

		return true;
	}

	public int compareTo(Object o) {
		BugInstance other = (BugInstance) o;
		int cmp;
		cmp = type.compareTo(other.type);
		if (cmp != 0)
			return cmp;
		cmp = priority - other.priority;
		if (cmp != 0)
			return cmp;

		// Compare BugAnnotations lexicographically
		int pfxLen = Math.min(annotationList.size(), other.annotationList.size());
		for (int i = 0; i < pfxLen; ++i) {
			BugAnnotation lhs = annotationList.get(i);
			BugAnnotation rhs = other.annotationList.get(i);
			cmp = lhs.compareTo(rhs);
			if (cmp != 0)
				return cmp;
		}

		// All elements in prefix were the same,
		// so use number of elements to decide
		return annotationList.size() - other.annotationList.size();
	}
}

// vim:ts=4
