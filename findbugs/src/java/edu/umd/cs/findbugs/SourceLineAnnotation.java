package edu.umd.cs.findbugs;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;

/**
 * A BugAnnotation that records a range of source lines
 * in a class.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class SourceLineAnnotation implements BugAnnotation {

	private String description;
	private String className;
	private int startLine;
	private int endLine;

	/**
	 * Constructor.
	 * @param className the class to which the line number(s) refer
	 * @param startLine the first line (inclusive)
	 * @param endLine the ending line (inclusive)
	 */
	public SourceLineAnnotation(String className, int startLine, int endLine) {
		this.description = "SOURCE_LINE_DEFAULT";
		this.className = className;
		this.startLine = startLine;
		this.endLine = endLine;
	}

	/**
	 * Factory method for creating a source line annotation describing
	 * an entire method.
	 * @param visitor a BetterVisitor which is visiting the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *   for the method
	 */
	public static SourceLineAnnotation fromVisitedMethod(BetterVisitor visitor) {
		LineNumberTable lineNumberTable = getLineNumberTable(visitor);
		if (lineNumberTable == null)
			return null;

		LineNumber[] table = lineNumberTable.getLineNumberTable();
		return new SourceLineAnnotation(visitor.getBetterClassName(), table[0].getLineNumber(), table[table.length-1].getLineNumber());
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for the instruction being visited by given visitor.
	 * @param visitor a BetterVisitor which is visiting the method
	 * @param pc the bytecode offset of the instruction in the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *   for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(BetterVisitor visitor, int pc) {
		LineNumberTable lineNumberTable = getLineNumberTable(visitor);
		if (lineNumberTable == null)
			return null;

		int lineNumber = lineNumberTable.getSourceLine(pc);
		return new SourceLineAnnotation(visitor.getBetterClassName(), lineNumber, lineNumber);
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for the instruction being visited by given visitor.
	 * @param visitor a DismantleBytecode visitor which is visiting the method
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *   for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(DismantleBytecode visitor) {
		return fromVisitedInstruction(visitor, visitor.getPC());
	}

	/**
	 * Factory method for creating a source line annotation describing the
	 * source line number for a visited instruction.
	 * @param methodGen the MethodGen object representing the method
	 * @param handle the InstructionHandle containing the visited instruction
	 * @return the SourceLineAnnotation, or null if we do not have line number information
	 *   for the instruction
	 */
	public static SourceLineAnnotation fromVisitedInstruction(MethodGen methodGen, InstructionHandle handle) {
		LineNumberTable table = methodGen.getLineNumberTable(methodGen.getConstantPool());
		if (table == null)
			return null;

		int lineNumber = table.getSourceLine(handle.getPosition());
		return new SourceLineAnnotation(methodGen.getClassName(), lineNumber, lineNumber);
	}

	private static LineNumberTable getLineNumberTable(BetterVisitor visitor) {
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

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitSourceLineAnnotation(this);
	}

	public String format(String key) {
		if (key.equals("")) {
			StringBuffer buf = new StringBuffer();
			buf.append(className);
			buf.append(":[");
			if (startLine == endLine) {
				buf.append("line ");
				buf.append(startLine);
			} else {
				buf.append("lines ");
				buf.append(startLine);
				buf.append('-');
				buf.append(endLine);
			}
			buf.append(']');
			return buf.toString();
		} else
			throw new IllegalStateException("Unknown format key " + key);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this});
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

	public int hashCode() {
		return className.hashCode() + startLine + endLine;
	}

	public boolean equals(Object o) {
		if (!(o instanceof SourceLineAnnotation))
			return false;
		SourceLineAnnotation other = (SourceLineAnnotation) o;
		return className.equals(other.className)
			&& startLine == other.startLine
			&& endLine == other.endLine;
	}
}

// vim:ts=4
