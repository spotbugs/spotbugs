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
 * Dataflow analysis to find the propagation of the "this" value
 * in Java stack frames.  This is a nice, simple example of a dataflow analysis
 * which determines properties of values in the Java stack frame.
 *
 * @see ThisValue
 * @see ThisValueFrame
 * @see ThisValueFrameModelingVisitor
 * @author David Hovemeyer
 */
public class ThisValueAnalysis extends ForwardDataflowAnalysis<ThisValueFrame> {
	private MethodGen methodGen;

	/**
	 * Constructor.
	 * @param methodGen the method to be analyzed - must not be static
	 */
	public ThisValueAnalysis(MethodGen methodGen) {
		if (methodGen.isStatic()) throw new IllegalArgumentException("Useless for static methods");
		this.methodGen = methodGen;
	}

	public ThisValueFrame createFact() {
		return new ThisValueFrame(methodGen.getMaxLocals());
	}

	public void copy(ThisValueFrame source, ThisValueFrame dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(ThisValueFrame result) {
		// Upon entry to the method, the "this" pointer is in local 0,
		// and all of the locals as "not this".
		result.setValid();
		result.setValue(0, ThisValue.thisValue());
		for (int i = 1; i < result.getNumLocals(); ++i)
			result.setValue(i, ThisValue.notThisValue());
	}

	public void initResultFact(ThisValueFrame result) {
		result.setTop();
	}

	public void makeFactTop(ThisValueFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(ThisValueFrame fact) {
		return fact.isValid();
	}

	public boolean same(ThisValueFrame fact1, ThisValueFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transferInstruction(InstructionHandle handle, ThisValueFrame fact) throws DataflowAnalysisException {
		handle.getInstruction().accept(new ThisValueFrameModelingVisitor(fact, methodGen.getConstantPool()));
	}

	public void meetInto(ThisValueFrame fact, Edge edge, ThisValueFrame result) throws DataflowAnalysisException {
		if (edge.getDest().isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.
			ThisValueFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			tmpFact.pushValue(ThisValue.notThisValue());
			fact = tmpFact;
		}
		result.mergeWith(fact);
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("Usage: edu.umd.cs.daveho.ba.ThisValueAnalysis <class file>");
				System.exit(1);
			}

			DataflowTestDriver<ThisValueFrame> driver = new DataflowTestDriver<ThisValueFrame>() {
				public AbstractDataflowAnalysis<ThisValueFrame> createAnalysis(MethodGen methodGen, CFG cfg) {
					return new ThisValueAnalysis(methodGen);
				}

				public void examineResults(CFG cfg, Dataflow<ThisValueFrame> dataflow) {
					Iterator<BasicBlock> i = cfg.blockIterator();
					while (i.hasNext()) {
						BasicBlock block = i.next();
						ThisValueFrame start = dataflow.getStartFact(block);
						ThisValueFrame result = dataflow.getResultFact(block);
						int numFound = 0;
						numFound += count(start);
						numFound += count(result);
						System.out.println("In block " + block.getId() + " found " + numFound + " occurrences of \"this\" value");
					}
				}
			
				private int count(ThisValueFrame frame) {
					int count = 0;
					if (frame.isTop()) {
						System.out.println("TOP frame!");
						return 0;
					}
					if (frame.isBottom()) {
						System.out.println("BOTTOM frame!");
						return 0;
					}
					int numSlots = frame.getNumSlots();
					for (int i = 0; i < numSlots; ++i) {
						if (frame.getValue(i).isThis())
							++count;
					}
					return count;
				}
			};

			driver.execute(argv[0]);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}

// vim:ts=4
