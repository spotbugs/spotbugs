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
				Set<ObjectType> thrownExceptionSet = enumerateExceptionTypes(basicBlock);

				// For each exception edge, determine if
				// the handler is reachable.  If not, delete it.
				// This ABSOLUTELY relies on the exception handlers being
				// enumerated in decreasing order of priority,
				// because we eliminate thrown types as we encounter
				// handlers where they are guaranteed to be caught.
				HashSet<Edge> deletedEdgeSet = new HashSet<Edge>();
				for (Iterator<Edge> j = cfg.outgoingEdgeIterator(basicBlock); j.hasNext(); ) {
					Edge edge = j.next();
					if (edge.getType() == HANDLED_EXCEPTION_EDGE) {
						if (!reachable(edge, thrownExceptionSet))
							deletedEdgeSet.add(edge);
					}
				}

				// Remove deleted edges
				for (Iterator<Edge> j = deletedEdgeSet.iterator(); j.hasNext(); ) {
					Edge edge = j.next();
					cfg.removeEdge(edge);
				}

				// If all exceptions are caught, remove the unhandled exception edge
				if (thrownExceptionSet.isEmpty()) {
					Edge edge = cfg.getOutgoingEdgeWithType(basicBlock, UNHANDLED_EXCEPTION_EDGE);
					if (edge != null) {
						cfg.removeEdge(edge);
					}
				}
			}
		}
	}

	private Set<ObjectType> enumerateExceptionTypes(BasicBlock basicBlock)
		throws ClassNotFoundException, DataflowAnalysisException {

		Set<ObjectType> exceptionTypeSet = new HashSet<ObjectType>();
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
			if (!(throwType instanceof ObjectType))
				throw new DataflowAnalysisException("Non object type thrown by " + pei);
			exceptionTypeSet.add((ObjectType) throwType);
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

	private boolean reachable(Edge edge, Set<ObjectType> thrownExceptionSet)
		throws ClassNotFoundException {
		BasicBlock handlerBlock = edge.getDest();
		CodeExceptionGen handler = handlerBlock.getExceptionGen();
		ObjectType catchType = handler.getCatchType();

		if (catchType == null) {
			// This handler catches all exceptions
			thrownExceptionSet.clear();
			return true;
		}

		boolean reachable = false;

		// Go through the set of thrown execeptions.
		// Any that will DEFINITELY be caught be this handler, remove.
		// Any that MIGHT be caught, but won't definitely be caught,
		// remain.
		for (Iterator<ObjectType> i = thrownExceptionSet.iterator(); i.hasNext(); ) {
			ObjectType thrownException = i.next();

			String thrownClassName = SignatureConverter.convert(thrownException.getSignature());
			String catchClassName = SignatureConverter.convert(catchType.getSignature());

			if (Repository.instanceOf(thrownClassName, catchClassName)) {
				// The thrown exception is a subtype of the catch type,
				// so this exception will DEFINITELY be caught by
				// this handler.
				reachable = true;
				i.remove();
			} else if (Repository.instanceOf(catchClassName, thrownClassName)) {
				// The thrown exception is a supertype of the catch type,
				// so it MIGHT get caught by this handler.
				reachable = true;
			}
		}

		return reachable;
	}
}

// vim:ts=4
