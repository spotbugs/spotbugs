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

import java.io.PrintStream;
import java.util.Iterator;

import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;

/**
 * Print out a representation of a control-flow graph. For debugging.
 *
 * @see CFG
 * @see CFGBuilder
 */
public class CFGPrinter {
    private final CFG cfg;

    private boolean isForwards;

    public CFGPrinter(CFG cfg) {
        this.cfg = cfg;
        this.isForwards = true;
    }

    public void setIsForwards(boolean isForwards) {
        this.isForwards = isForwards;
    }

    /**
     * @return Returns the isForwards.
     */
    public boolean isForwards() {
        return isForwards;
    }

    public void print(PrintStream out) {
        Iterator<BasicBlock> i = cfg.blockIterator();
        while (i.hasNext()) {
            BasicBlock bb = i.next();
            out.println();
            out.println("BASIC BLOCK: " + bb.getLabel() + (bb.isExceptionThrower() ? " [EXCEPTION THROWER]" : "")
                    + blockStartAnnotate(bb));
            if (bb.isExceptionThrower()) {
                out.println("  Exception thrower: " + bb.getExceptionThrower());
            }
            CodeExceptionGen exceptionGen = bb.getExceptionGen();
            if (exceptionGen != null) {
                out.println("\tCATCHES " + exceptionGen.getCatchType());
            }
            Iterator<InstructionHandle> j = instructionIterator(bb);
            while (j.hasNext()) {
                InstructionHandle handle = j.next();
                out.println(handle + instructionAnnotate(handle, bb));
            }
            out.println("END" + blockAnnotate(bb));
            Iterator<Edge> edgeIter = isForwards ? cfg.outgoingEdgeIterator(bb) : cfg.incomingEdgeIterator(bb);
            while (edgeIter.hasNext()) {
                Edge edge = edgeIter.next();
                out.println("  " + edge.formatAsString(!isForwards) + " " + edgeAnnotate(edge));
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

    protected Iterator<InstructionHandle> instructionIterator(BasicBlock bb) {
        if (isForwards) {
            return bb.instructionIterator();
        } else {
            return bb.instructionReverseIterator();
        }
    }

    // public static void main(String[] argv) throws Exception {
    //
    // if (argv.length == 0 || argv.length > 2) {
    // System.out.println("Usage: " + CFGPrinter.class.getName() +
    // " <class file> [outputFile]");
    // System.exit(1);
    // }
    //
    // String className = argv[0];
    // JavaClass cls = new ClassParser(className).parse();
    // RepositoryLookupFailureCallback lookupFailureCallback = new
    // DebugRepositoryLookupFailureCallback();
    //
    // AnalysisContext analysisContext =
    // AnalysisContext.create(lookupFailureCallback);
    // ClassContext classContext = analysisContext.getClassContext(cls);
    //
    // Method[] methods = cls.getMethods();
    // String methodName = SystemProperties.getProperty("cfg.method");
    // PrintStream out = System.err;
    // if (argv.length == 2)
    // out = UTF8.printStream(new FileOutputStream(argv[1]));
    // for (Method method : methods) {
    // MethodGen methodGen = classContext.getMethodGen(method);
    // if (methodGen == null)
    // continue;
    //
    // if (methodName != null && !method.getName().equals(methodName))
    // continue;
    //
    //
    // out.println();
    // out.println("----------------------------------------------------------------------------");
    // out.println("Method " +
    // SignatureConverter.convertMethodSignature(methodGen));
    // out.println("----------------------------------------------------------------------------");
    //
    // CFG cfg = classContext.getCFG(method);
    // CFGPrinter printer = new CFGPrinter(cfg);
    // printer.print(out);
    // }
    // }
}

