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

import java.util.*;
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

	/**
	 * Property to prune all implicit unchecked exception edges.
	 * For analyses which find bugs, this is a reasonable behavior:
	 * runtime exceptions and errors should not occur (and in general
	 * should never be caught).
	 *
	 * Note that this will preserve edges for unchecked exceptions that are:
	 * <ul>
	 * <li> Declared to be thrown from a called method
	 * <li> Thrown directly (i.e., with ATHROW)
	 * <li> Caught explicitly - if the user thinks it can happen,
	 *      we'll assume it can happen
	 * </ul>
	 */
	private static final boolean PRUNE_IMPLICIT_UNCHECKED_EXCEPTIONS = Boolean.getBoolean("cfg.prune.implicitUnchecked");

	static {
		if (STATS) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.err.println("Exception edges pruned: " + numEdgesPruned);
				}
			});
		}
	}

	private static class ExceptionSet {
		private Set<ObjectType> set = new HashSet<ObjectType>();
		private boolean universalHandler = false;

		public Iterator<ObjectType> iterator() { return set.iterator(); }

		public boolean isEmpty() { return set.isEmpty(); }

		public boolean add(ObjectType type) { return set.add(type); }

		public boolean remove(ObjectType type) { return set.remove(type); }

		public void sawUniversal() {
			universalHandler = true;
			set.clear();
		}

		public boolean sawUniversalHandler() {
			return universalHandler;
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
				ExceptionSet thrownExceptionSet = enumerateExceptionTypes(basicBlock);

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

	private static final ObjectType EXCEPTION_TYPE = new ObjectType("java.lang.Exception");
	private static final ObjectType ERROR_TYPE = new ObjectType("java.lang.Error");
	private static final ObjectType RUNTIME_EXCEPTION_TYPE = new ObjectType("java.lang.RuntimeException");

	private ExceptionSet enumerateExceptionTypes(BasicBlock basicBlock)
		throws ClassNotFoundException, DataflowAnalysisException {

		ExceptionSet exceptionTypeSet = new ExceptionSet();
		InstructionHandle pei = basicBlock.getExceptionThrower();
		Instruction ins = pei.getInstruction();

		// Get the exceptions that BCEL knows about.
		// Note that all of these are unchecked.
		if (!PRUNE_IMPLICIT_UNCHECKED_EXCEPTIONS) {
			ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
			Class[] exceptionList = exceptionThrower.getExceptions();
			for (int i = 0; i < exceptionList.length; ++i) {
				exceptionTypeSet.add(new ObjectType(exceptionList[i].getName()));
			}
		}

		// Assume that an Error may be thrown by any instruction.
		if (!PRUNE_IMPLICIT_UNCHECKED_EXCEPTIONS) {
			exceptionTypeSet.add(ERROR_TYPE);
		}

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
			ObjectType[] declaredExceptionList = Hierarchy.findDeclaredExceptions(inv, cpg);
			if (declaredExceptionList == null) {
				// Couldn't find declared exceptions,
				// so conservatively assume it could thrown any checked exception.
				if (DEBUG) System.out.println("Couldn't find declared exceptions for " +
					SignatureConverter.convertMethodSignature(inv, cpg));
				exceptionTypeSet.add(EXCEPTION_TYPE);
			} else {
				for (int i = 0; i < declaredExceptionList.length; ++i) {
					exceptionTypeSet.add(declaredExceptionList[i]);
				}
			}

			if (!PRUNE_IMPLICIT_UNCHECKED_EXCEPTIONS) {
				exceptionTypeSet.add(RUNTIME_EXCEPTION_TYPE);
			}
		}

		if (DEBUG) System.out.println(pei + " can throw " + exceptionTypeSet);

		return exceptionTypeSet;
	}

	private boolean reachable(Edge edge, ExceptionSet thrownExceptionSet)
		throws ClassNotFoundException {

		if (DEBUG) System.out.println("Checking reachability of edge:\n\t" + edge);

		BasicBlock handlerBlock = edge.getTarget();
		CodeExceptionGen handler = handlerBlock.getExceptionGen();
		ObjectType catchType = handler.getCatchType();

		if (catchType == null || catchType.equals(Type.THROWABLE)) {
			// Universal handler: it catches all exceptions
			thrownExceptionSet.sawUniversal();
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

			if (Hierarchy.isSubtype(thrownClassName, catchClassName)) {
				// The thrown exception is a subtype of the catch type,
				// so this exception will DEFINITELY be caught by
				// this handler.
				if (DEBUG) System.out.println("\tException is subtype of catch type: will definitely catch");
				reachable = true;
				i.remove();
			} else if (Hierarchy.isSubtype(catchClassName, thrownClassName)) {
				// The thrown exception is a supertype of the catch type,
				// so it MIGHT get caught by this handler.
				if (DEBUG) System.out.println("\tException is supertype of catch type: might catch");
				reachable = true;
			}
		}

		// Special case: if the handler is for an unchecked exception,
		// and we haven't seen a universal handler, then assume
		// the handler is reachable.
		if (!thrownExceptionSet.sawUniversalHandler()
			&& (Hierarchy.isSubtype(catchType, RUNTIME_EXCEPTION_TYPE) ||
				Hierarchy.isSubtype(catchType, ERROR_TYPE)))
			reachable = true;

		if (DEBUG) System.out.println(reachable ? "\tReachable" : "\tNot reachable");

		return reachable;
	}
}

// vim:ts=4
