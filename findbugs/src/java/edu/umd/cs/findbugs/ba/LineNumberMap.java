package edu.umd.cs.daveho.ba;

import java.util.*;
import java.io.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Summarize line numbers (and other source information)
 * for a method.
 */
public class LineNumberMap implements Debug {
    /**
     * When this is true, the workaround for the bug in BCEL 5.0's
     * LineNumberTable class is disabled.
     */
    private static final boolean LINE_NUMBER_BUG = Boolean.getBoolean("lineNumberBug");

    private MethodGen methodGen;
    private IdentityHashMap<InstructionHandle, LineNumber> lineNumberMap;
    private boolean hasLineNumbers;

    /**
     * Constructor.
     * @param methodGen the method to summarize line numbers for
     */
    public LineNumberMap(MethodGen methodGen) {
	this.methodGen = methodGen;
	lineNumberMap = new IdentityHashMap<InstructionHandle, LineNumber>();
	hasLineNumbers = false;
    }

    /**
     * Build the line number information.
     * Should be called before any other methods.
     */
    public void build() {
	int numGood = 0, numBytecodes = 0;

	if (DEBUG) {
	    System.out.println("Method: " + methodGen.getName() + " - " + methodGen.getSignature() +
		"in class " + methodGen.getClassName());
	}

	// Associate line number information with each InstructionHandle
	LineNumberTable table = methodGen.getLineNumberTable(methodGen.getConstantPool());

	if (table != null && table.getTableLength() > 0) {
	    checkTable(table);
	    InstructionHandle handle = methodGen.getInstructionList().getStart();
	    while (handle != null) {
		int bytecodeOffset = handle.getPosition();
		if (bytecodeOffset < 0)
		    throw new IllegalStateException("Bad bytecode offset: " + bytecodeOffset);
		if (DEBUG) System.out.println("Looking for source line for bytecode offset " + bytecodeOffset);
		int sourceLine;
		try {
		    sourceLine = table.getSourceLine(bytecodeOffset);
		} catch (ArrayIndexOutOfBoundsException e) {
		    if (LINE_NUMBER_BUG)
			throw e;
		    else
			sourceLine = -1;
		}
		if (sourceLine >= 0)
		    ++numGood;
		lineNumberMap.put(handle,
		    new LineNumber(bytecodeOffset, sourceLine));
		handle = handle.getNext();
		++numBytecodes;
	    }
	    hasLineNumbers = true;

	    if (DEBUG) System.out.println("\t" + numGood + "/" + numBytecodes + " had valid line numbers");
	}
    }

    private void checkTable(LineNumberTable table) {
	if (DEBUG) System.out.println("line number table has length " + table.getTableLength());
	LineNumber[] entries = table.getLineNumberTable();
	int lastBytecode = -1;
	for (int i = 0; i < entries.length; ++i) {
	    LineNumber ln = entries[i];
	    if (DEBUG) System.out.println("Entry " + i + ": pc=" + ln.getStartPC() + ", line=" + ln.getLineNumber());
	    int pc = ln.getStartPC();
	    if (pc <= lastBytecode) throw new IllegalStateException("LineNumberTable is not sorted");
	}
    }

    /** Does this method have line number information? */
    public boolean hasLineNumbers() {
	return hasLineNumbers;
    }

    /**
     * Find the line number information for instruction whose
     * handle is given.
     * @param handle the InstructionHandle
     * @return the LineNumber object containing bytecode offset and source line number
     */
    public LineNumber lookupLineNumber(InstructionHandle handle) {
	return lineNumberMap.get(handle);
    }
}
