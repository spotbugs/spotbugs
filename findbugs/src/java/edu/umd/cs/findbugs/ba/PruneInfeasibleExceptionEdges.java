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
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class PruneInfeasibleExceptionEdges implements EdgeTypes {
	private CFG cfg;
	private TypeDataflow typeDataflow;
	private ConstantPoolGen cpg;

	public PruneInfeasibleExceptionEdges(CFG cfg, TypeDataflow typeDataflow, ConstantPoolGen cpg) {
		this.cfg = cfg;
		this.typeDataflow = typeDataflow;
		this.cpg = cpg;
	}

	public void execute() throws ClassNotFoundException, DataflowAnalysisException {
		Iterator<BasicBlock> i = cfg.blockIterator();
		while (i.hasNext()) {
			BasicBlock basicBlock = i.next();
			if (basicBlock.isExceptionThrower()) {
				// Enumerate the kinds of exceptions this block can throw
				Set<Type> thrownExceptionSet = enumerateExceptionTypes(basicBlock);

				// For each exception edge, determine if
				// the handler is reachable.  If not, delete it.
				HashSet<Edge> deletedEdgeSet = new HashSet<Edge>();
				Iterator<Edge> j = cfg.outgoingEdgeIterator(basicBlock);
				while (j.hasNext()) {
					Edge edge = j.next();
					if (edge.getType() == HANDLED_EXCEPTION_EDGE) {
						if (!reachable(edge, thrownExceptionSet))
							deletedEdgeSet.add(edge);
					}
				}
			}
		}
	}

	private Set<Type> enumerateExceptionTypes(BasicBlock basicBlock)
		throws ClassNotFoundException, DataflowAnalysisException {

		Set<Type> exceptionTypeSet = new HashSet<Type>();
		InstructionHandle pei = basicBlock.getExceptionThrower();
		Instruction ins = pei.getInstruction();

		// Get the exceptions that BCEL knows about
		ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
		Class[] exceptionList = exceptionThrower.getExceptions();
		for (int i = 0; i < exceptionList.length; ++i) {
			exceptionTypeSet.add(new ObjectType(exceptionList[i].getName()));
		}

		// If it's an ATHROW, get the type from the TypeDataflow
		if (ins instanceof ATHROW) {
			TypeFrame frame = typeDataflow.getFactAtLocation(new Location(pei, basicBlock));
			Type throwType = frame.getTopValue();
			exceptionTypeSet.add(throwType);
		}

		// If it's an InvokeInstruction, add declared exceptions
		if (ins instanceof InvokeInstruction) {
			InvokeInstruction inv = (InvokeInstruction) ins;
			Method method = Lookup.findExactMethod(inv, cpg);
			if (method != null) {
				ExceptionTable exceptionTable = method.getExceptionTable();
				if (exceptionTable != null) {
					String[] exceptionNameList = exceptionTable.getExceptionNames();
					for (int i = 0; i < exceptionNameList.length; ++i ) {
						exceptionTypeSet.add(new ObjectType(exceptionNameList[i]));
					}
				}
			}
		}

		return exceptionTypeSet;
	}

	private boolean reachable(Edge edge, Set<Type> thrownExceptionSet)
		throws ClassNotFoundException {
		BasicBlock handlerBlock = edge.getDest();

		return true;
	}
}

// vim:ts=4
