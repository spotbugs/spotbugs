/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import java.util.*;

import java.io.IOException;

import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;

import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;
import edu.umd.cs.findbugs.xml.XMLWriteable;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

/**
 * An instance of a bug pattern.
 * A BugInstance consists of several parts:
 * <p/>
 * <ul>
 * <li> the type, which is a string indicating what kind of bug it is;
 * used as a key for the FindBugsMessages resource bundle
 * <li> the priority; how likely this instance is to actually be a bug
 * <li> a list of <em>annotations</em>
 * </ul>
 * <p/>
 * The annotations describe classes, methods, fields, source locations,
 * and other relevant context information about the bug instance.
 * Every BugInstance must have at least one ClassAnnotation, which
 * describes the class in which the instance was found.  This is the
 * "primary class annotation".
 * <p/>
 * <p> BugInstance objects are built up by calling a string of <code>add</code>
 * methods.  (These methods all "return this", so they can be chained).
 * Some of the add methods are specialized to get information automatically from
 * a BetterVisitor or DismantleBytecode object.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class BugInstance implements Comparable, XMLWriteable {
	private String type;
	private int priority;
	private ArrayList<BugAnnotation> annotationList;
	private ClassAnnotation primaryClassAnnotation;
	private MethodAnnotation primaryMethodAnnotation;
	private FieldAnnotation primaryFieldAnnotation;
	private int cachedHashCode;
	private String annotationText;

	/**
	 * This value is used to indicate that the cached hashcode
	 * is invalid, and should be recomputed.
	 */
	private static final int INVALID_HASH_CODE = 0;
	
	/**
	 * This value is used to indicate whether BugInstances should be reprioritized very low,
	 * when the BugPattern is marked as experimental
	 */
	private static boolean adjustExperimental = false;
	
	/**
	 * Constructor.
	 *
	 * @param type     the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(String type, int priority) {
		this.type = type;
		this.priority = priority < Detector.HIGH_PRIORITY 
			? Detector.HIGH_PRIORITY : priority;
		annotationList = new ArrayList<BugAnnotation>(4);
		primaryClassAnnotation = null;
		cachedHashCode = INVALID_HASH_CODE;
		annotationText = "";
		
		if (adjustExperimental && isExperimental())
			this.priority = Detector.EXP_PRIORITY;
	}

	/**
	 * Create a new BugInstance.
	 * This is the constructor that should be used by Detectors.
	 * 
	 * @param detector the Detector that is reporting the BugInstance
	 * @param type     the bug type
	 * @param priority the bug priority
	 */
	public BugInstance(Detector detector, String type, int priority) {
		this(type, priority);
		
		if (detector != null) {
			// Adjust priority if required
			DetectorFactory factory =
				DetectorFactoryCollection.instance().getFactoryByClassName(detector.getClass().getName());
			if (factory != null) {
				this.priority += factory.getPriorityAdjustment();
				if (this.priority < 0)
					this.priority = 0;
			}
		}
		
		if (adjustExperimental && isExperimental())
			this.priority = Detector.EXP_PRIORITY;
	}
		
	public static void setAdjustExperimental(boolean adjust) {
		adjustExperimental = adjust;
	}
	
	/* ----------------------------------------------------------------------
	 * Accessors
	 * ---------------------------------------------------------------------- */

	/**
	 * Get the bug type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the BugPattern.
	 */
	public BugPattern getBugPattern() {
		return I18N.instance().lookupBugPattern(getType());
	}

	/**
	 * Get the bug priority.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Set the bug priority.
	 */
	public void setPriority(int p) {
		priority = p < Detector.HIGH_PRIORITY 
			? Detector.HIGH_PRIORITY : p;
	}

	/**
	 * Is this bug instance the result of an experimental detector?
	 */
	public boolean isExperimental() {
		BugPattern pattern = I18N.instance().lookupBugPattern(type);
		return (pattern != null) ? pattern.isExperimental() : false;
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
	 * Get the primary method annotation, which indicates where the bug occurs.
	 */
	public FieldAnnotation getPrimaryField() {
		return primaryFieldAnnotation;
	}

	/**
	 * Get the primary source line annotation.
	 *
	 * @return the source line annotation, or null if there is
	 *         no source line annotation
	 */
	public SourceLineAnnotation getPrimarySourceLineAnnotation() {
		// Highest priority: return the first top level source line annotation
		Iterator<BugAnnotation> i = annotationList.iterator();
		while (i.hasNext()) {
			BugAnnotation annotation = i.next();
			if (annotation instanceof SourceLineAnnotation)
				return (SourceLineAnnotation) annotation;
		}

		// Second priority: return the source line annotation describing the
		// primary method
		if (primaryMethodAnnotation != null)
			return primaryMethodAnnotation.getSourceLines();
		else
			return null;
	}

	/**
	 * Get an Iterator over all bug annotations.
	 */
	public Iterator<BugAnnotation> annotationIterator() {
		return annotationList.iterator();
	}

	/**
	 * Get the abbreviation of this bug instance's BugPattern.
	 * This is the same abbreviation used by the BugCode which
	 * the BugPattern is a particular species of.
	 */
	public String getAbbrev() {
		BugPattern pattern = I18N.instance().lookupBugPattern(getType());
		return pattern != null ? pattern.getAbbrev() : "<unknown bug pattern>";
	}

	/**
	 * Set the user annotation text.
	 *
	 * @param annotationText the user annotation text
	 */
	public void setAnnotationText(String annotationText) {
		this.annotationText = annotationText;
	}

	/**
	 * Get the user annotation text.
	 *
	 * @return the user annotation text
	 */
	public String getAnnotationText() {
		return annotationText;
	}

	/**
	 * Determine whether or not the annotation text contains
	 * the given word.
	 *
	 * @param word the word
	 * @return true if the annotation text contains the word, false otherwise
	 */
	public boolean annotationTextContainsWord(String word) {
		return getTextAnnotationWords().contains(word);
	}

	/**
	 * Get set of words in the text annotation.
	 */
	public Set<String> getTextAnnotationWords() {
		HashSet<String> result = new HashSet<String>();

		StringTokenizer tok = new StringTokenizer(annotationText, " \t\r\n\f.,:;-");
		while (tok.hasMoreTokens()) {
			result.add(tok.nextToken());
		}
		return result;
	}

	/* ----------------------------------------------------------------------
	 * Combined annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a class annotation and a method annotation for the class and method
	 * which the given visitor is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClassAndMethod(PreorderVisitor visitor) {
		addClass(visitor);
		addMethod(visitor);
		return this;
	}

	/**
	 * Add class and method annotations for given method.
	 *
	 * @param methodGen  the method
	 * @param sourceFile source file the method is defined in
	 * @return this object
	 */
	public BugInstance addClassAndMethod(MethodGen methodGen, String sourceFile) {
		addClass(methodGen.getClassName());
		addMethod(methodGen, sourceFile);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Class annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 *
	 * @param className the name of the class
	 * @return this object
	 */
	public BugInstance addClass(String className) {
		ClassAnnotation classAnnotation = new ClassAnnotation(className);
		add(classAnnotation);
		return this;
	}

	/**
	 * Add a class annotation.  If this is the first class annotation added,
	 * it becomes the primary class annotation.
	 *
	 * @param jclass the JavaClass object for the class
	 * @return this object
	 */
	public BugInstance addClass(JavaClass jclass) {
		addClass(jclass.getClassName());
		return this;
	}

	/**
	 * Add a class annotation for the class that the visitor is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addClass(PreorderVisitor visitor) {
		String className = visitor.getDottedClassName();
		addClass(className);
		return this;
	}

	/**
	 * Add a class annotation for the superclass of the class the visitor
	 * is currently visiting.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addSuperclass(PreorderVisitor visitor) {
		String className = visitor.getSuperclassName();
		addClass(className);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Field annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add a field annotation.
	 *
	 * @param className name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig  type signature of the field
	 * @param isStatic  whether or not the field is static
	 * @return this object
	 */
	public BugInstance addField(String className, String fieldName, String fieldSig, boolean isStatic) {
		addField(new FieldAnnotation(className, fieldName, fieldSig, isStatic));
		return this;
	}

	/**
	 * Add a field annotation
	 *
	 * @param fieldAnnotation the field annotation
	 * @return this object
	 */
	public BugInstance addField(FieldAnnotation fieldAnnotation) {
		add(fieldAnnotation);
		return this;
	}

	/**
	 * Add a field annotation for a FieldVariable matched in a ByteCodePattern.
	 *
	 * @param field the FieldVariable
	 * @return this object
	 */
	public BugInstance addField(FieldVariable field) {
		return addField(field.getClassName(), field.getFieldName(), field.getFieldSig(), field.isStatic());
	}

	/**
	 * Add a field annotation for an XField.
	 *
	 * @param xfield the XField
	 * @return this object
	 */
	public BugInstance addField(XField xfield) {
		return addField(xfield.getClassName(), xfield.getFieldName(), xfield.getFieldSignature(), xfield.isStatic());
	}

	/**
	 * Add a field annotation for the field which has just been accessed
	 * by the method currently being visited by given visitor.
	 * Assumes that a getfield/putfield or getstatic/putstatic
	 * has just been seen.
	 *
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addReferencedField(DismantleBytecode visitor) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(visitor);
		addField(f);
		return this;
	}

	/**
	 * Add a field annotation for the field referenced by the FieldAnnotation parameter
	 */
	public BugInstance addReferencedField(FieldAnnotation fa) {
		addField(fa);
		return this;
	}

	/**
	 * Add a field annotation for the field which is being visited by
	 * given visitor.
	 *
	 * @param visitor the visitor
	 * @return this object
	 */
	public BugInstance addVisitedField(PreorderVisitor visitor) {
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
	 *
	 * @param className  name of the class containing the method
	 * @param methodName name of the method
	 * @param methodSig  type signature of the method
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
	 *
	 * @param methodGen  the MethodGen object for the method
	 * @param sourceFile source file method is defined in
	 * @return this object
	 */
	public BugInstance addMethod(MethodGen methodGen, String sourceFile) {
		MethodAnnotation methodAnnotation =
		        new MethodAnnotation(methodGen.getClassName(), methodGen.getName(), methodGen.getSignature());
		addMethod(methodAnnotation);
		addSourceLinesForMethod(methodAnnotation, SourceLineAnnotation.fromVisitedMethod(methodGen, sourceFile));
		return this;
	}

	/**
	 * Add a method annotation for the method which the given visitor is currently visiting.
	 * If the method has source line information, then a SourceLineAnnotation
	 * is added to the method.
	 *
	 * @param visitor the BetterVisitor
	 * @return this object
	 */
	public BugInstance addMethod(PreorderVisitor visitor) {
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
	 *
	 * @param visitor the DismantleBytecode object
	 * @return this object
	 */
	public BugInstance addCalledMethod(DismantleBytecode visitor) {
		String className = visitor.getDottedClassConstantOperand();
		String methodName = visitor.getNameConstantOperand();
		String methodSig = visitor.getDottedSigConstantOperand();
		addMethod(className, methodName, methodSig);
		describe("METHOD_CALLED");
		return this;
	}

	/**
	 * Add a method annotation.
	 *
	 * @return this object
	 */
	public BugInstance addCalledMethod(String className, String methodName, String methodSig) {
		addMethod(className, methodName, methodSig);
		describe("METHOD_CALLED");
		return this;
	}

	/**
	 * Add a method annotation for the method which is called by given
	 * instruction.
	 *
	 * @param methodGen the method containing the call
	 * @param inv       the InvokeInstruction
	 * @return this object
	 */
	public BugInstance addCalledMethod(MethodGen methodGen, InvokeInstruction inv) {
		ConstantPoolGen cpg = methodGen.getConstantPool();
		String className = inv.getClassName(cpg);
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);
		addMethod(className, methodName, methodSig);
		describe("METHOD_CALLED");
		return this;
	}

	/**
	 * Add a method annotation.  If this is the first method annotation added,
	 * it becomes the primary method annotation.
	 *
	 * @param methodAnnotation the method annotation
	 * @return this object
	 */
	public BugInstance addMethod(MethodAnnotation methodAnnotation) {
		add(methodAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Integer annotation adders
	 * ---------------------------------------------------------------------- */

	/**
	 * Add an integer annotation.
	 *
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
	 *
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
	 *
	 * @param visitor a BetterVisitor that is currently visiting the method
	 * @param pc      bytecode offset of the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(PreorderVisitor visitor, int pc) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor, pc);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation for the given instruction in the given method.
	 * Note that if the method does not have line number information, then
	 * no source line annotation will be added.
	 *
	 * @param methodGen  the method being visited
	 * @param sourceFile source file the method is defined in
	 * @param handle     the InstructionHandle containing the visited instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(MethodGen methodGen, String sourceFile, InstructionHandle handle) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(methodGen, sourceFile, handle);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a source line annotation describing a range of instructions.
	 *
	 * @param methodGen  the method
	 * @param sourceFile source file the method is defined in
	 * @param start      the start instruction in the range
	 * @param end        the end instruction in the range (inclusive)
	 * @return this object
	 */
	public BugInstance addSourceLine(MethodGen methodGen, String sourceFile, InstructionHandle start, InstructionHandle end) {
		// Make sure start and end are really in the right order.
		if (start.getPosition() > end.getPosition()) {
			InstructionHandle tmp = start;
			start = end;
			end = tmp;
		}
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(methodGen, sourceFile, start, end);
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
	 *
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC   the bytecode offset of the end instruction in the range
	 * @return this object
	 */
	public BugInstance addSourceLineRange(PreorderVisitor visitor, int startPC, int endPC) {
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
	 *
	 * @param visitor a DismantleBytecode visitor that is currently visiting the instruction
	 * @return this object
	 */
	public BugInstance addSourceLine(DismantleBytecode visitor) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(visitor);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/**
	 * Add a non-specific source line annotation.
	 * This will result in the entire source file being displayed.
	 *
	 * @param className  the class name
	 * @param sourceFile the source file name
	 * @return this object
	 */
	public BugInstance addUnknownSourceLine(String className, String sourceFile) {
		SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.createUnknown(className, sourceFile);
		if (sourceLineAnnotation != null)
			add(sourceLineAnnotation);
		return this;
	}

	/* ----------------------------------------------------------------------
	 * Formatting support
	 * ---------------------------------------------------------------------- */

	/**
	 * Format a string describing this bug instance.
	 *
	 * @return the description
	 */
	public String getMessage() {
		String pattern = I18N.instance().getMessage(type);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format((BugAnnotation[]) annotationList.toArray(new BugAnnotation[annotationList.size()]));
	}

	/**
	 * Add a description to the most recently added bug annotation.
	 *
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
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("type", type)
			.addAttribute("priority", String.valueOf(priority));

		BugPattern pattern = getBugPattern();
		if (pattern != null) {
			// The bug abbreviation and pattern category are
			// emitted into the XML for informational purposes only.
			// (The information is redundant, but might be useful
			// for processing tools that want to make sense of
			// bug instances without looking at the plugin descriptor.)
			attributeList.addAttribute("abbrev", pattern.getAbbrev());
			attributeList.addAttribute("category", pattern.getCategory());
		}

		xmlOutput.openTag(ELEMENT_NAME, attributeList);

		if (!annotationText.equals("")) {
			xmlOutput.openTag("UserAnnotation");
			xmlOutput.writeCDATA(annotationText);
			xmlOutput.closeTag("UserAnnotation");
		}

		XMLOutputUtil.writeCollection(xmlOutput, annotationList);

		xmlOutput.closeTag(ELEMENT_NAME);
	}

	private static final String ELEMENT_NAME = "BugInstance";
	private static final String USER_ANNOTATION_ELEMENT_NAME = "UserAnnotation";

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	void add(BugAnnotation annotation) {
		if (annotation == null)
			throw new IllegalStateException("Missing BugAnnotation!");

		// Add to list
		annotationList.add(annotation);

		// This object is being modified, so the cached hashcode
		// must be invalidated
		cachedHashCode = INVALID_HASH_CODE;

		if ((annotation instanceof ClassAnnotation) && primaryClassAnnotation == null)
			primaryClassAnnotation = (ClassAnnotation) annotation;

		if ((annotation instanceof MethodAnnotation) && primaryMethodAnnotation == null)
			primaryMethodAnnotation = (MethodAnnotation) annotation;
		if ((annotation instanceof FieldAnnotation) && primaryFieldAnnotation == null)
			primaryFieldAnnotation = (FieldAnnotation) annotation;
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
			if (hashcode == INVALID_HASH_CODE)
				hashcode = INVALID_HASH_CODE+1;
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
