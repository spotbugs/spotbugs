/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

import java.io.IOException;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * A BugAnnotation that records a range of source lines
 * in a class.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class SourceLineAnnotation implements BugAnnotation {
	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_ROLE = "SOURCE_LINE_DEFAULT";
	public static final String DEFAULT_ROLE_UNKNOWN_LINE = "SOURCE_LINE_DEFAULT_UNKNOWN_LINE";
	public static final String ROLE_ANOTHER_INSTANCE = "SOURCE_LINE_ANOTHER_INSTANCE";


	/**
	 * String returned if the source file is unknown.
	 * This must match what BCEL uses when the source file is unknown.
	 */
	public static final String UNKNOWN_SOURCE_FILE = "<Unknown>";

	private String description;
	final private String className;
	private String sourceFile;
	final private int startLine;
	final private int endLine;
	final private int startBytecode;
	final private int endBytecode;
	private boolean synthetic;

	/**
	 * Constructor.
	 *
	 * @param className     the class to which the line number(s) refer
	 * @param sourceFile    the name of the source file
	 * @param startLine     the first line (inclusive)
	 * @param endLine       the ending line (inclusive)
	 * @param startBytecode the first bytecode offset (inclusive)
	 * @param endBytecode   the end bytecode offset (inclusive)
	 */
	public SourceLineAnnotation(@NonNull String className, @NonNull String sourceFile, int startLine, int endLine,
								int startBytecode, int endBytecode) {
		if (className == null) throw new IllegalArgumentException("class name is null");
		if (sourceFile == null) throw new IllegalArgumentException("source file is null");
		this.description = DEFAULT_ROLE;
		this.className = className;
		this.sourceFile = sourceFile;
		this.startLine = startLine;
		this.endLine = endLine;
		this.startBytecode = startBytecode;
		this.endBytecode = endBytecode;
	}


	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Factory method to create an unknown source line annotation.
	 *
	 * @param className the class name
	 * @param sourceFile the source file name
	 * @return the SourceLineAnnotation
	 */
	public static SourceLineAnnotation createUnknown(String className, String sourceFile) {
		return createUnknown(className, sourceFile, -1, -1);
	}

	/**
	 * Factory method to create an unknown source line annotation.
	 * This variant looks up the source filename automatically
	 * based on the class using best effort.
	 *
	 * @param className the class name
	 * @return the SourceLineAnnotation
	 */
	public static SourceLineAnnotation createUnknown(String className) {
		return createUnknown(
				className,
				AnalysisContext.currentAnalysisContext().lookupSourceFile(className),
				-1,
				-1);
	}
	/**
	 * Factory method to create an unknown source line annotation.
	 * This doesn't use the analysis context.
	 *
	 * @param className the class name
	 * @return the SourceLineAnnotation
	 */
	public static SourceLineAnnotation createReallyUnknown(String className) {
		return createUnknown(
				className,
				SourceLineAnnotation.UNKNOWN_SOURCE_FILE,
				-1,
				-1);
	}

	/**
	 * Factory method to create an unknown source line annotation.
	 * This variant is used when bytecode offsets are known,
	 * but not source lines.
	 *
	 * @param className the class name
	 * @param sourceFile the source file name
	 * @return the SourceLineAnnotation
	 */
	public static SourceLineAnnotation createUnknown(String className, String sourceFile, int startBytecode, int endBytecode) {
		SourceLineAnnotation result = new SourceLineAnnotation(className, sourceFile, -1, -1, startBytecode, endBytecode);
		// result.setDescription("SOURCE_LINE_UNKNOWN");
		return result;
	}

	/**
	 * Factory method for creating a source line annotation describing
	 * an entire method.
	 *
	 * @param visitor a BetterVisitor which is visiting the method
	 * @return the SourceLineAnnotation
	 */
	public static SourceLineAnnotation fromVisitedMethod(PreorderVisitor visitor) {

		SourceLineAnnotation sourceLines = getSourceAnnotationForMethod(
				visitor.getDottedClassName(), visitor.getMethodName(), visitor.getMethodSig());
		return sourceLines;
	}

	/**
	 * Factory method for creating a source line annotation describing an entire
	 * method.
	 * 
	 * @param methodGen
	 *            the method being visited
	 * @return the SourceLineAnnotation, or null if we do not have line number
	 *         information for the method
	 */
	public static SourceLineAnnotation fromVisitedMethod(MethodGen methodGen, String sourceFile) {
		LineNumberTable lineNumberTable = methodGen.getLineNumberTable(methodGen.getConstantPool());
		String className = methodGen.getClassName();
		int codeSize = methodGen.getInstructionList().getLength();
		if (lineNumberTable == null)
			return createUnknown(className, sourceFile, 0, codeSize - 1);
		return forEntireMethod(className, sourceFile, lineNumberTable, codeSize);
	}

	/**
	 * Create a SourceLineAnnotation covering an entire method.
	 * 
	 * @param className       name of the class the method is in
	 * @param sourceFile      source file containing the method
	 * @param lineNumberTable the method's LineNumberTable
	 * @param codeSize        size in bytes of the method's code
	 * @return a SourceLineAnnotation covering the entire method
	 */
	public static SourceLineAnnotation forEntireMethod(String className, String sourceFile,
														LineNumberTable lineNumberTable, int codeSize) {
		LineNumber[] table = lineNumberTable.getLineNumberTable();
		if (table != null && table.length > 0) {
			LineNumber first = table[0];
			LineNumber last = table[table.length - 1];
			return new SourceLineAnnotation(className, sourceFile, first.getLineNumber(), last.getLineNumber(),
					0, codeSize - 1);
		} else {
			return createUnknown(className, sourceFile, 0, codeSize - 1);
		}
	}

	/**
	 * Create a SourceLineAnnotation covering an entire method.
	 * 
	 * @param javaClass JavaClass containing the method
	 * @param method    the method
	 * @return a SourceLineAnnotation for the entire method
	 */
	public static SourceLineAnnotation forEntireMethod(JavaClass javaClass, @CheckForNull Method method) {
		String sourceFile = javaClass.getSourceFileName();
		if (method == null) 
			return createUnknown(javaClass.getClassName(), sourceFile);
		Code code = method.getCode();
		LineNumberTable lineNumberTable = method.getLineNumberTable();
		if (code == null || lineNumberTable == null) {
			return createUnknown(javaClass.getClassName(), sourceFile);
		}

		return forEntireMethod(javaClass.getClassName(), sourceFile, lineNumberTable, code.getLength());
	}

	/**
	 * Create a SourceLineAnnotation covering an entire method.
	 * 
	 * @param javaClass JavaClass containing the method
	 * @param xmethod    the method
	 * @return a SourceLineAnnotation for the entire method
	 */
	public static SourceLineAnnotation forEntireMethod(JavaClass javaClass, XMethod xmethod) {
		JavaClassAndMethod m = Hierarchy.findMethod(javaClass, xmethod.getName(), xmethod.getSignature());
		if (m == null) {
			return createUnknown(javaClass.getClassName(), javaClass.getSourceFileName());
		} else {
			return forEntireMethod(javaClass, m.getMethod());
		}
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for the instruction being visited by given visitor.
	 *
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param pc      the bytecode offset of the instruction in the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(BytecodeScanningDetector visitor, int pc) {
		return fromVisitedInstructionRange(visitor.getClassContext(), visitor, pc, pc);
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for the instruction being visited by given visitor.
	 *
	 * @param classContext the ClassContext
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param pc      the bytecode offset of the instruction in the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(ClassContext classContext, PreorderVisitor visitor, int pc) {
		return fromVisitedInstructionRange(classContext, visitor, pc, pc);
	}

	/**
	 * Create from Method and Location in a visited class.
	 * 
	 * @param classContext ClassContext of visited class
	 * @param method       Method in visited class
	 * @param loc          Location in visited class
	 * @return SourceLineAnnotation describing visited Location
	 */
	public static SourceLineAnnotation fromVisitedInstruction(ClassContext classContext, Method method, Location loc) {
		return fromVisitedInstruction(classContext, method, loc.getHandle());
	}

	/**
	 * Create from Method and InstructionHandle in a visited class.
	 * 
	 * @param classContext ClassContext of visited class
	 * @param method       Method in visited class
	 * @param handle       InstructionHandle in visited class
	 * @return SourceLineAnnotation describing visited instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(ClassContext classContext, Method method, InstructionHandle handle) {
		return fromVisitedInstruction(classContext, method, handle.getPosition());
	}

	/**
	 * Create from MethodDescriptor and Location of visited instruction.
	 * 
	 * @param methodDescriptor MethodDescriptor identifying analyzed method
	 * @param location         Location of instruction within analyed method
	 * @return SourceLineAnnotation describing visited instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(MethodDescriptor methodDescriptor, Location location) {
		return fromVisitedInstruction(methodDescriptor, location.getHandle().getPosition());
	}
	public static SourceLineAnnotation fromVisitedInstruction(MethodDescriptor methodDescriptor, int position) {
		try {
			IAnalysisCache analysisCache = Global.getAnalysisCache();
			JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, methodDescriptor.getClassDescriptor());
			Method method = analysisCache.getMethodAnalysis(Method.class, methodDescriptor);
			return fromVisitedInstruction(jclass, method, position);
		} catch (CheckedAnalysisException e) {
			return createReallyUnknown(methodDescriptor.getClassDescriptor().toDottedClassName());
		}
	}

	/**
	 * Create from Method and bytecode offset in a visited class.
	 * 
	 * @param classContext ClassContext of visited class
	 * @param method       Method in visited class
	 * @param pc           bytecode offset in visited method
	 * @return SourceLineAnnotation describing visited instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(ClassContext classContext, Method method, int pc) {
		return fromVisitedInstruction(classContext.getJavaClass(), method, pc);
	}
	
	/**
	 * Create from Method and bytecode offset in a visited class.
	 * 
	 * @param jclass       JavaClass of visited class
	 * @param method       Method in visited class
	 * @param pc           bytecode offset in visited method
	 * @return SourceLineAnnotation describing visited instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(JavaClass jclass, Method method, int pc) {
		LineNumberTable lineNumberTable = method.getCode().getLineNumberTable();
		String className = jclass.getClassName();
		String sourceFile = jclass.getSourceFileName();
		if (lineNumberTable == null)
			return createUnknown(className, sourceFile, pc, pc);

		int startLine = lineNumberTable.getSourceLine(pc);
		return new SourceLineAnnotation(className, sourceFile, startLine, startLine, pc, pc);
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line numbers for a range of instructions in the method being
	 * visited by the given visitor.
	 *
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC   the bytecode offset of the end instruction in the range
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstructionRange(
			BytecodeScanningDetector visitor, int startPC, int endPC) {
		LineNumberTable lineNumberTable = getLineNumberTable(visitor);
		String className = visitor.getDottedClassName();
		String sourceFile = visitor.getSourceFile();

		if (lineNumberTable == null)
			return createUnknown(className, sourceFile, startPC, endPC);

		int startLine = lineNumberTable.getSourceLine(startPC);
		int endLine = lineNumberTable.getSourceLine(endPC);
		return new SourceLineAnnotation(className, sourceFile, startLine, endLine, startPC, endPC);
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line numbers for a range of instructions in the method being
	 * visited by the given visitor.
	 *
	 * @param classContext the ClassContext
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param startPC the bytecode offset of the start instruction in the range
	 * @param endPC   the bytecode offset of the end instruction in the range
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstructionRange(
			ClassContext classContext, PreorderVisitor visitor, int startPC, int endPC) {
		LineNumberTable lineNumberTable = getLineNumberTable(visitor);
		String className = visitor.getDottedClassName();
		String sourceFile = visitor.getSourceFile();

		if (lineNumberTable == null)
			return createUnknown(className, sourceFile, startPC, endPC);

		int startLine = lineNumberTable.getSourceLine(startPC);
		int endLine = lineNumberTable.getSourceLine(endPC);
		return new SourceLineAnnotation(className, sourceFile, startLine, endLine, startPC, endPC);
	}

	public static SourceLineAnnotation fromRawData(String className, String sourceFile, int startLine, int endLine, int startPC, int endPC) {
		if (startLine == -1)
			return createUnknown(className, sourceFile, startPC, endPC);

		return new SourceLineAnnotation(className, sourceFile, startLine, endLine, startPC, endPC);
	}
	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for the instruction being visited by given visitor.
	 *
	 * @param visitor a DismantleBytecode visitor which is visiting the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(BytecodeScanningDetector visitor) {
		return fromVisitedInstruction(visitor.getClassContext(), visitor, visitor.getPC());
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for a visited instruction.
	 *
	 * @param classContext the ClassContext
	 * @param methodGen the MethodGen object representing the method
	 * @param handle    the InstructionHandle containing the visited instruction
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *         for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(ClassContext classContext, MethodGen methodGen, String sourceFile, @NonNull InstructionHandle handle) {
		LineNumberTable table = methodGen.getLineNumberTable(methodGen.getConstantPool());
		String className = methodGen.getClassName();

		int bytecodeOffset = handle.getPosition();

		if (table == null)
			return createUnknown(className, sourceFile, bytecodeOffset, bytecodeOffset);

		int lineNumber = table.getSourceLine(handle.getPosition());
		return new SourceLineAnnotation(
				className, sourceFile, lineNumber, lineNumber, bytecodeOffset, bytecodeOffset);
	}

	/**
	 * Factory method for creating a source line annotation describing
	 * the source line numbers for a range of instruction in a method.
	 *
	 * @param classContext theClassContext
	 * @param methodGen the method
	 * @param start     the start instruction
	 * @param end       the end instruction (inclusive)
	 */
	public static SourceLineAnnotation fromVisitedInstructionRange(
			ClassContext classContext, MethodGen methodGen, String sourceFile, InstructionHandle start, InstructionHandle end) {
		LineNumberTable lineNumberTable = methodGen.getLineNumberTable(methodGen.getConstantPool());
		String className = methodGen.getClassName();

		if (lineNumberTable == null)
			return createUnknown(className, sourceFile, start.getPosition(), end.getPosition());

		int startLine = lineNumberTable.getSourceLine(start.getPosition());
		int endLine = lineNumberTable.getSourceLine(end.getPosition());
		return new SourceLineAnnotation(
				className, sourceFile, startLine, endLine, start.getPosition(), end.getPosition());
	}

	private static LineNumberTable getLineNumberTable(PreorderVisitor visitor) {
		Code code = visitor.getMethod().getCode();
		if (code == null)
			return null;
		return code.getLineNumberTable();
	}

	/**
	 * Get the class name.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Get the source file name.
	 */
	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Is the source file known?
	 */
	public boolean isSourceFileKnown() {
		return !sourceFile.equals(UNKNOWN_SOURCE_FILE);
	}

	/**
	 * Set the source file name.
	 *
	 * @param sourceFile the source file name
	 */
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}


	/**
	 * Get the simple class name (the part of the name after the dot)
	 */
	public String getSimpleClassName() {
		int lastDot = className.lastIndexOf('.');
		return className.substring(lastDot+1);
	}

	/**
	 * Get the package name.
	 */
	public String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			return "";
		else
			return className.substring(0, lastDot);
	}

	/**
	 * Get the start line (inclusive).
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * Get the ending line (inclusive).
	 */
	public int getEndLine() {
		return endLine;
	}

	/**
	 * Get start bytecode (inclusive).
	 */
	public int getStartBytecode() {
		return startBytecode;
	}

	/**
	 * Get end bytecode (inclusive).
	 */
	public int getEndBytecode() {
		return endBytecode;
	}

	/**
	 * Is this an unknown source line annotation?
	 */
	public boolean isUnknown() {
		return startLine < 0 || endLine < 0;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitSourceLineAnnotation(this);
	}

	public String format(String key, ClassAnnotation primaryClass) {
		if (key.equals("hash")) return "";
		if (key.equals("")) {
			StringBuffer buf = new StringBuffer();
			buf.append(sourceFile);
			appendLines(buf);
			return buf.toString();
		} else if (key.equals("lineNumber")) {
			StringBuffer buf = new StringBuffer();
			appendLinesRaw(buf);
			return buf.toString();
		} else if (key.equals("full")) {
			StringBuffer buf = new StringBuffer();
			String pkgName = getPackageName();
			if (!pkgName.equals("")) {
				buf.append(pkgName.replace('.', '/'));
				buf.append('/');
			}
			buf.append(sourceFile);
			appendLines(buf);
			return buf.toString();
		} else
			throw new IllegalArgumentException("Unknown format key " + key);
	}

	private void appendLines(StringBuffer buf) {
		if (isUnknown()) return;
		buf.append(":[");
		appendLinesRaw(buf);
		buf.append(']');
	}

	private void appendLinesRaw(StringBuffer buf) {
		if (isUnknown()) return;
		if (startLine == endLine) {
			buf.append("line ");
			buf.append(startLine);
		} else {
			buf.append("lines ");
			buf.append(startLine);
			buf.append('-');
			buf.append(endLine);
		}

	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		String desc = description;
		if (desc.equals(DEFAULT_ROLE) && isUnknown())
			desc = DEFAULT_ROLE_UNKNOWN_LINE;
		String pattern = I18N.instance().getAnnotationDescription(desc);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this}, null);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof SourceLineAnnotation)) // All BugAnnotations must be comparable
			return this.getClass().getName().compareTo(o.getClass().getName());

		SourceLineAnnotation other = (SourceLineAnnotation) o;

		int cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = startLine - other.startLine;
		if (cmp != 0)
			return cmp;
		return endLine - other.endLine;
	}

	@Override
	public int hashCode() {
		return className.hashCode() + startLine + 3 * endLine;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SourceLineAnnotation))
			return false;
		SourceLineAnnotation other = (SourceLineAnnotation) o;
		return className.equals(other.className)
				&& startLine == other.startLine
				&& endLine == other.endLine;
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "SourceLine";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		writeXML(xmlOutput, false);
	}

	public void writeXML(XMLOutput xmlOutput, boolean addMessages) throws IOException {
		String classname = getClassName();
		String packageName = "";
		if (classname.indexOf('.') > 0) 
			packageName = classname.substring(0,1+classname.lastIndexOf('.'));
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("classname", classname);
		int n = getStartLine(); // start/end are now optional (were too many "-1"s in the xml)
		if (n >= 0) attributeList.addAttribute("start", String.valueOf(n));
		n = getEndLine();
		if (n >= 0) attributeList.addAttribute("end", String.valueOf(n));
		n = getStartBytecode(); // startBytecode/endBytecode haven't been set for a while now
		if (n >= 0) attributeList.addAttribute("startBytecode", String.valueOf(n));
		n = getEndBytecode();
		if (n >= 0) attributeList.addAttribute("endBytecode", String.valueOf(n));

		if (isSourceFileKnown()) {
			attributeList.addAttribute("sourcefile", sourceFile);
			attributeList.addAttribute("sourcepath", packageName.replace('.', '/')+sourceFile);
		}

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", getDescription());
		if (synthetic) 
			attributeList.addAttribute("synthetic", "true");
		if (addMessages) {
			xmlOutput.openTag(ELEMENT_NAME, attributeList);
			xmlOutput.openTag("Message");
			xmlOutput.writeText(this.toString());
			xmlOutput.closeTag("Message");
			xmlOutput.closeTag(ELEMENT_NAME);
		} else {
			xmlOutput.openCloseTag(ELEMENT_NAME, attributeList);
		}
	}

	/**
	 * @param synthetic The synthetic to set.
	 */
	public void setSynthetic(boolean synthetic) {
		this.synthetic = synthetic;
	}

	/**
	 * @return Returns the synthetic.
	 */
	public boolean isSynthetic() {
		return synthetic;
	}


	public boolean isSignificant() {
		return false;
	}


	/**
	 * @param className
	 * @param methodName
	 * @param methodSig
	 * @return
	 */
	static SourceLineAnnotation getSourceAnnotationForMethod(
			String className, String methodName, String methodSig) {
		JavaClassAndMethod targetMethod = null;
		Code code = null;

		try {
			JavaClass targetClass = AnalysisContext.currentAnalysisContext().lookupClass(className);
			targetMethod = Hierarchy.findMethod(targetClass, methodName, methodSig);
			if (targetMethod != null) {
				Method method = targetMethod.getMethod();
				if (method != null) code = method.getCode();
			}

		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		SourceInfoMap sourceInfoMap = AnalysisContext.currentAnalysisContext().getSourceInfoMap();
		SourceInfoMap.SourceLineRange range = sourceInfoMap.getMethodLine(className, methodName, methodSig);

		if (range != null) 
			return new SourceLineAnnotation(
					className,
					AnalysisContext.currentAnalysisContext().lookupSourceFile(className),
					range.getStart(),
					range.getEnd(),
					0,
					code == null ? -1 : code.getLength());

		if (sourceInfoMap.fallBackToClassfile() && targetMethod != null) 
			return  forEntireMethod(
					targetMethod.getJavaClass(), targetMethod.getMethod());


		// If we couldn't find the source lines,
		// create an unknown source line annotation referencing
		// the class and source file.

		return createUnknown(className);
	}


/**
 * @param className
 * @return
 */
	static SourceLineAnnotation getSourceAnnotationForClass(String className, String sourceFileName) {

		int lastLine = -1;
		int firstLine = Integer.MAX_VALUE;

		try {
			JavaClass targetClass = AnalysisContext.currentAnalysisContext().lookupClass(className);
			for (Method m : targetClass.getMethods()) {
				Code c = m.getCode();
				if (c != null) {
					LineNumberTable table = c.getLineNumberTable();
					if (table != null)
						for (LineNumber line : table.getLineNumberTable()) {
							lastLine = Math.max(lastLine, line.getLineNumber());
							firstLine = Math.min(firstLine, line.getLineNumber());
						}
				}
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		if (firstLine < Integer.MAX_VALUE)
			return new SourceLineAnnotation(className, sourceFileName,
					firstLine, lastLine, -1, -1);
		return   SourceLineAnnotation.createUnknown(className, sourceFileName);
	}
}
// vim:ts=4
