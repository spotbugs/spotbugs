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

/**
 * Prune a CFG to remove infeasible exception edges.
 * In order to determine what kinds of exceptions can be thrown by
 * explicit ATHROW instructions, type analysis must first be
 * performed on the unpruned CFG.
 *
 * @see CFG
 * @see TypeAnalysis
 * @author David Hovemeyer
 */
public class PruneInfeasibleExceptionEdges implements EdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("cfg.prune.debug");
	private static final boolean STATS = Boolean.getBoolean("cfg.prune.stats");
	private static int numEdgesPruned = 0;

	static {
		if (STATS) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.err.println("Exception edges pruned: " + numEdgesPruned);
				}
			});
		}
	}

	private CFG cfg;
	private TypeDataflow typeDataflow;
	private ConstantPoolGen cpg;

	/**
	 * Constructor.
	 * @param cfg the CFG to prune
	 * @param typeDataflow initialized TypeDataflow object for the CFG,
	 *   indicating the types of all stack locations
	 * @param cpg the ConstantPoolGen for the method
	 */
	public PruneInfeasibleExceptionEdges(CFG cfg, TypeDataflow typeDataflow, ConstantPoolGen cpg) {
		this.cfg = cfg;
		this.typeDataflow = typeDataflow;
		this.cpg = cpg;
	}

	/**
	 * Prune infeasible exception edges from the CFG.
	 * If the method returns normally, then the operation
	 * was successful, and the CFG should no longer contain infeasible
	 * exception edges.  If ClassNotFoundException or DataflowAnalysisException
	 * are thrown, then the operation was unsuccessful,
	 * <em>but the CFG is still valid because it was not modified</em>.
	 * If a runtime exception is thrown, then the CFG may be
	 * partially modified and should be considered invalid.
	 */
	public void execute() throws ClassNotFoundException, DataflowAnalysisException {
		HashSet<Edge> deletedEdgeSet = new HashSet<Edge>();

		// Scan all basic blocks for infeasible exception edges
		Iterator<BasicBlock> i = cfg.blockIterator();
		while (i.hasNext()) {
			BasicBlock basicBlock = i.next();
			if (basicBlock.isExceptionThrower()) {
				// Enumerate the kinds of exceptions this block can throw
				Set<ObjectType> thrownExceptionSet = enumerateExceptionTypes(basicBlock);

				// For each exception edge, determine if
				// the handler is reachable.  If not, delete it.
				// This ABSOLUTELY relies on the handled exception edges being
				// enumerated in decreasing order of priority,
				// because we eliminate thrown types as we encounter
				// handlers where they are guaranteed to be caught.
				for (Iterator<Edge> j = cfg.outgoingEdgeIterator(basicBlock); j.hasNext(); ) {
					Edge edge = j.next();
					if (edge.getType() == HANDLED_EXCEPTION_EDGE) {
						if (!reachable(edge, thrownExceptionSet)) {
							deletedEdgeSet.add(edge);
						}
					}
				}

				// If all exceptions are caught, mark the unhandled exception edge
				// for deletion
				if (thrownExceptionSet.isEmpty()) {
					Edge edge = cfg.getOutgoingEdgeWithType(basicBlock, UNHANDLED_EXCEPTION_EDGE);
					if (edge != null) {
						deletedEdgeSet.add(edge);
					}
				}
			}
		}

		// Remove deleted edges
		for (Iterator<Edge> j = deletedEdgeSet.iterator(); j.hasNext(); ) {
			Edge edge = j.next();
			cfg.removeEdge(edge);
			if (STATS) ++numEdgesPruned;
		}
	}

	private static final ObjectType ERROR_TYPE = new ObjectType("java.lang.Error");
	private static final ObjectType RUNTIME_EXCEPTION_TYPE = new ObjectType("java.lang.RuntimeException");

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

		// Assume that an Error may be thrown by any instruction.
		exceptionTypeSet.add(ERROR_TYPE);

		// If it's an ATHROW, get the type from the TypeDataflow
		if (ins instanceof ATHROW) {
			TypeFrame frame = typeDataflow.getFactAtLocation(new Location(pei, basicBlock));

			// Check whether or not the frame is valid.
			// Sun's javac sometimes emits unreachable code.
			// For example, it will emit code that follows a JSR
			// subroutine call that never returns.
			// If the frame is invalid, then we can just make
			// a conservative assumption that anything could be
			// thrown at this ATHROW.
			if (!frame.isValid()) {
				exceptionTypeSet.add(Type.THROWABLE);
			} else {
				Type throwType = frame.getTopValue();
				if (!(throwType instanceof ObjectType))
					throw new DataflowAnalysisException("Non object type thrown by " + pei);
				exceptionTypeSet.add((ObjectType) throwType);
			}
		}

		// If it's an InvokeInstruction, add declared exceptions and RuntimeException
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
				} else if (DEBUG) {
					System.out.println("No exception table for method: " + method.getName());
				}
			} else if (DEBUG) {
				System.out.println("Could not find method for " + ins);
			}

			exceptionTypeSet.add(RUNTIME_EXCEPTION_TYPE);
		}

		if (DEBUG) System.out.println(pei + " can throw " + exceptionTypeSet);

		return exceptionTypeSet;
	}

	private boolean reachable(Edge edge, Set<ObjectType> thrownExceptionSet)
		throws ClassNotFoundException {

		if (DEBUG) System.out.println("Checking reachability of edge:\n\t" + edge);

		BasicBlock handlerBlock = edge.getDest();
		CodeExceptionGen handler = handlerBlock.getExceptionGen();
		ObjectType catchType = handler.getCatchType();

		if (catchType == null) {
			// This handler catches all exceptions
			thrownExceptionSet.clear();
			return true;
		}

		String catchClassName = SignatureConverter.convert(catchType.getSignature());
		boolean reachable = false;

		// Go through the set of thrown execeptions.
		// Any that will DEFINITELY be caught be this handler, remove.
		// Any that MIGHT be caught, but won't definitely be caught,
		// remain.
		for (Iterator<ObjectType> i = thrownExceptionSet.iterator(); i.hasNext(); ) {
			ObjectType thrownException = i.next();

			String thrownClassName = SignatureConverter.convert(thrownException.getSignature());

			if (DEBUG) System.out.println("\texception type " + thrownClassName + ", catch type " + catchClassName);

			if (Repository.instanceOf(thrownClassName, catchClassName)) {
				// The thrown exception is a subtype of the catch type,
				// so this exception will DEFINITELY be caught by
				// this handler.
				if (DEBUG) System.out.println("\tException is subtype of catch type: will definitely catch");
				reachable = true;
				i.remove();
			} else if (Repository.instanceOf(catchClassName, thrownClassName)) {
				// The thrown exception is a supertype of the catch type,
				// so it MIGHT get caught by this handler.
				if (DEBUG) System.out.println("\tException is supertype of catch type: might catch");
				reachable = true;
			}
		}

		if (DEBUG) System.out.println(reachable ? "\tReachable" : "\tNot reachable");

		return reachable;
	}
}

// vim:ts=4
