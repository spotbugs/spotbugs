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
import java.io.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Print out a representation of a control-flow graph.
 * For debugging.
 * @see CFG
 * @see CFGBuilder
 */
public class CFGPrinter {
    private CFG cfg;

    public CFGPrinter(CFG cfg) {
	this.cfg = cfg;
    }

    public void print(PrintStream out) {
	Iterator<BasicBlock> i = cfg.blockIterator();
	while (i.hasNext()) {
	    BasicBlock bb = i.next();
	    out.println("BASIC BLOCK: " + bb.getId() + (bb.isExceptionThrower() ? " [EXCEPTION THROWER]" : "") + blockStartAnnotate(bb));
	    CodeExceptionGen exceptionGen = bb.getExceptionGen();
	    if (exceptionGen != null) {
		System.out.println("    CATCHES " + exceptionGen.getCatchType());
	    }
	    Iterator<InstructionHandle> j = bb.instructionIterator();
	    while (j.hasNext()) {
		InstructionHandle handle = j.next();
		out.println(handle + instructionAnnotate(handle, bb));
	    }
	    out.println("END" + blockAnnotate(bb));
	    Iterator<Edge> edgeIter = cfg.outgoingEdgeIterator(bb);
	    while (edgeIter.hasNext()) {
		Edge edge = edgeIter.next();
		out.println("  " + edge + " " + edgeAnnotate(edge));
	    }
	}
    }

    public String edgeAnnotate(Edge edge) {
	return "";
    }

    public String blockStartAnnotate(BasicBlock block) {
	return "";
    }

    public String blockAnnotate(BasicBlock block) {
	return "";
    }

    public String instructionAnnotate(InstructionHandle handle, BasicBlock bb) {
	return "";
    }

    public static void main(String[] argv) {
	try {
	    if (argv.length != 1) {
		System.out.println("Usage: edu.umd.cs.edgecov.CFGPrinter <class file>");
		System.exit(1);
	    }

	    String className = argv[0];
	    JavaClass cls = new ClassParser(className).parse();
	    Method[] methods = cls.getMethods();
	    ConstantPoolGen cp = new ConstantPoolGen(cls.getConstantPool());
	    String methodName = System.getProperty("cfg.method");

	    for (int i = 0; i < methods.length; ++i) {
		Method method = methods[i];
		if (method.isAbstract() || method.isNative())
		    continue;
		if (methodName != null && !method.getName().equals(methodName))
		    continue;
		MethodGen methodGen = new MethodGen(method, cls.getClassName(), cp);

		CFGBuilder builder = CFGBuilderFactory.create(methodGen);
		builder.build();

		System.out.println("---------------------------------------------------");
		System.out.println("Method " + method.getName());
		CFG cfg = builder.getCFG();
		cfg.assignEdgeIds(0);
		CFGPrinter printer = new CFGPrinter(cfg);
		printer.print(System.out);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
