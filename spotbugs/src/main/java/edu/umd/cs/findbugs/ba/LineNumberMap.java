/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.IdentityHashMap;

import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Summarize line numbers (and other source information) for a method.
 */
public class LineNumberMap {
    /**
     * Set this property to true to get debug print statements.
     */
    private static final boolean DEBUG = SystemProperties.getBoolean("lnm.debug");

    /**
     * When this is true, the workaround for the bug in BCEL 5.0's
     * LineNumberTable class is disabled.
     */
    private static final boolean LINE_NUMBER_BUG = SystemProperties.getBoolean("lineNumberBug");

    private final MethodGen methodGen;

    private final IdentityHashMap<InstructionHandle, LineNumber> lineNumberMap;

    private boolean hasLineNumbers;

    /**
     * Constructor.
     *
     * @param methodGen
     *            the method to summarize line numbers for
     */
    public LineNumberMap(MethodGen methodGen) {
        this.methodGen = methodGen;
        lineNumberMap = new IdentityHashMap<InstructionHandle, LineNumber>();
        hasLineNumbers = false;
    }

    /**
     * Build the line number information. Should be called before any other
     * methods.
     */
    public void build() {
        int numGood = 0, numBytecodes = 0;

        if (DEBUG) {
            System.out.println("Method: " + methodGen.getName() + " - " + methodGen.getSignature() + "in class "
                    + methodGen.getClassName());
        }

        // Associate line number information with each InstructionHandle
        LineNumberTable table = methodGen.getLineNumberTable(methodGen.getConstantPool());

        if (table != null && table.getTableLength() > 0) {
            checkTable(table);
            InstructionHandle handle = methodGen.getInstructionList().getStart();
            while (handle != null) {
                int bytecodeOffset = handle.getPosition();
                if (bytecodeOffset < 0) {
                    throw new IllegalStateException("Bad bytecode offset: " + bytecodeOffset);
                }
                if (DEBUG) {
                    System.out.println("Looking for source line for bytecode offset " + bytecodeOffset);
                }
                int sourceLine;
                try {
                    sourceLine = table.getSourceLine(bytecodeOffset);
                } catch (ArrayIndexOutOfBoundsException e) {
                    if (LINE_NUMBER_BUG) {
                        throw e;
                    } else {
                        sourceLine = -1;
                    }
                }
                if (sourceLine >= 0) {
                    ++numGood;
                }
                lineNumberMap.put(handle, new LineNumber(bytecodeOffset, sourceLine));
                handle = handle.getNext();
                ++numBytecodes;
            }
            hasLineNumbers = true;

            if (DEBUG) {
                System.out.println("\t" + numGood + "/" + numBytecodes + " had valid line numbers");
            }
        }
    }

    private void checkTable(LineNumberTable table) {
        if (DEBUG) {
            System.out.println("line number table has length " + table.getTableLength());
        }
        LineNumber[] entries = table.getLineNumberTable();
        int lastBytecode = -1;
        for (int i = 0; i < entries.length; ++i) {
            LineNumber ln = entries[i];
            if (DEBUG) {
                System.out.println("Entry " + i + ": pc=" + ln.getStartPC() + ", line=" + ln.getLineNumber());
            }
            int pc = ln.getStartPC();
            if (pc <= lastBytecode) {
                throw new IllegalStateException("LineNumberTable is not sorted");
            }
        }
    }

    /**
     * Does this method have line number information?
     */
    public boolean hasLineNumbers() {
        return hasLineNumbers;
    }

    /**
     * Find the line number information for instruction whose handle is given.
     *
     * @param handle
     *            the InstructionHandle
     * @return the LineNumber object containing bytecode offset and source line
     *         number
     */
    public LineNumber lookupLineNumber(InstructionHandle handle) {
        return lineNumberMap.get(handle);
    }
}
