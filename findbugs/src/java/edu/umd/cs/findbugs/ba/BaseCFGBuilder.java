/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Abstract base class for control flow graph builders.
 * @see CFG
 * @see BasicBlock
 * @see Edge
 */
public abstract class BaseCFGBuilder implements CFGBuilder, EdgeTypes, Debug {
    protected MethodGen methodGen;
    protected LineNumberMap lineNumberMap;
    protected CFG cfg;
    protected boolean hasLineNumbers;

    /**
     * Constructor.
     * @param methodGen method to build CFG for
     */
    protected BaseCFGBuilder(MethodGen methodGen) {
	this.methodGen = methodGen;
	lineNumberMap = new LineNumberMap(methodGen);
	lineNumberMap.build();
	if (DEBUG && !lineNumberMap.hasLineNumbers())
	    System.out.println("No line numbers for method " + methodGen.getSignature());
	hasLineNumbers = lineNumberMap.hasLineNumbers();
	cfg = new CFG();
    }

    /**
     * Get the control-flow graph.
     */
    public CFG getCFG() {
	return cfg;
    }

    /**
     * Does the method have line number information?
     */
    public boolean hasLineNumbers() {
	return hasLineNumbers;
    }

    /**
     * Get an instance of the default kind of CFGBuilder.
     */
    public static CFGBuilder getDefaultInstance(MethodGen methodGen) {
	return new BasicCFGBuilder(methodGen);
    }

    /**
     * Add an edge to the control flow graph.
     * Annotates the edge with information about the source and destination
     * of the edge.
     * @param source source basic block
     * @param dest destination basic block
     * @param edgeType the edge type
     */
    protected void addEdge(BasicBlock source, BasicBlock dest, int edgeType) {
	// Add edge to the CFG
	Edge edge = cfg.addEdge(source, dest, edgeType);
	annotateEdge(edge, source, dest);
    }

    /**
     * Add a potentially duplicate edge to the control flow graph.
     * A common reason multiple edges with the same source and destination
     * block might be added is because both a normal return and an
     * unhandled exception might cause control to transfer from a block
     * to the exit node.
     * (TODO: perhaps we should make a special exit node for unhandled exceptions,
     * to avoid this confusion.)
     * @param source source basic block
     * @param dest destination basic block
     * @param edgeType the edge type
     */
    protected void addDuplicateEdge(BasicBlock source, BasicBlock dest, int edgeType) {
	Edge edge = cfg.addDuplicateEdge(source, dest, edgeType);
	annotateEdge(edge, source, dest);
    }

    private void annotateEdge(Edge edge, BasicBlock source, BasicBlock dest) {
	// Annotate with source/destination bytecode and source line.
	InstructionHandle sourceHandle = source.getLastInstruction();
	InstructionHandle destHandle = dest.getFirstInstruction();
	int sourceBytecode = getBytecode(sourceHandle);
	int destBytecode = getBytecode(destHandle);
	int sourceLine = getSourceLine(sourceHandle);
	int destLine = getSourceLine(destHandle);
	edge.setSourceAndDest(sourceBytecode, destBytecode, sourceLine, destLine);
    }

    /**
     * Get bytecode offset of instruction represented by given InstructionHandle.
     * (Returns -1 if no such instruction.)
     */
    protected int getBytecode(InstructionHandle handle) {
	if (handle == null)
	    return -1;
	return handle.getPosition();
    }

    /**
     * Get source line number of instruction represented by given InstructionHandle.
     * (Returns -1 if no such instruction.)
     */
    protected int getSourceLine(InstructionHandle handle) {
	if (handle == null)
	    return -1;
	LineNumber lineNumber = lineNumberMap.lookupLineNumber(handle);
	if (lineNumber == null) {
	    if (DEBUG) System.out.println("No source line for bytecode " + handle);
	    return -1;
	}
	int line = lineNumber.getLineNumber();
	return line;
    }

}
