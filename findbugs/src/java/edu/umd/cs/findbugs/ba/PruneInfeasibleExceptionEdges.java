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
 * <p> We also attempt to classify the remaining exception edges
 * into categories:
 * <ul>
 * <li> Checked exceptions
 * <li> Explicit unchecked exceptions
 * <li> Implicit unchecked exceptions
 * </ul>
 *
 * <p> The idea is that implicit unchecked exceptions
 * are not likely to be interesting in practice, and analyses
 * may want to ignore them.
 *
 * <p> FIXME: finally blocks can end in an ATHROW.
 * Need a way to mark these as implicit.
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

	/**
	 * A momento to remind us of how we classified a particular
	 * exception edge.  If pruning and classifying succeeds,
	 * then these momentos can be applied to actually change
	 * the state of the edges.  The issue is that the entire
	 * pruning/classifying operation must either fail or succeed
	 * as a whole.  Thus, we don't commit any CFG changes until
	 * we know everything was successful.
	 */
	private static class MarkedEdge {
		private Edge edge;
		private int flag;

		public MarkedEdge(Edge edge, int flag) {
			this.edge = edge;
			this.flag = flag;
		}

		public void apply() {
			int flags = edge.getFlags();
			flags |= this.flag;
			edge.setFlags(flags);
		}
	}

	private CFG cfg;
	private MethodGen methodGen;
	private TypeDataflow typeDataflow;
	private ConstantPoolGen cpg;

	/**
	 * Constructor.
	 * @param cfg the CFG to prune
	 * @param methodGen the method
	 * @param typeDataflow initialized TypeDataflow object for the CFG,
	 *   indicating the types of all stack locations
	 * @param cpg the ConstantPoolGen for the method
	 */
	public PruneInfeasibleExceptionEdges(CFG cfg, MethodGen methodGen, TypeDataflow typeDataflow, ConstantPoolGen cpg) {
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
		List<MarkedEdge> markedEdgeList = new LinkedList<MarkedEdge>();

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
						checkReachability(edge, thrownExceptionSet, deletedEdgeSet, markedEdgeList);
					}
				}

				// If all exceptions are caught, mark the unhandled exception edge
				// for deletion
				if (thrownExceptionSet.isEmpty()) {
					Edge edge = cfg.getOutgoingEdgeWithType(basicBlock, UNHANDLED_EXCEPTION_EDGE);
					if (edge != null) {
						deletedEdgeSet.add(edge);
					}
				} else {
					// Unhandled exceptions can be thrown.
					// Check whether they are implicit or explicit.
					int flag;
					if (thrownExceptionSet.containsCheckedExceptions())
						flag = CHECKED_EXCEPTIONS_FLAG;
					else if (thrownExceptionSet.containsExplicitExceptions())
						flag = EXPLICIT_UNCHECKED_EXCEPTIONS_FLAG;
					else
						flag = IMPLICIT_UNCHECKED_EXCEPTIONS_FLAG;
					Edge edge = cfg.getOutgoingEdgeWithType(basicBlock, UNHANDLED_EXCEPTION_EDGE);
					if (edge == null) {
						// Raw CFGs are built conservatively,
						// so this exception should never happen.
						// If it does happen, it indicates a bug in the CFG builder.
						throw new DataflowAnalysisException("In method " +
							SignatureConverter.convertMethodSignature(methodGen) +
							" block " + basicBlock.getId() + " has missing unhandled exception edge");
					}
					markedEdgeList.add(new MarkedEdge(edge, flag));
				}
			}
		}

		// Remove deleted edges
		for (Iterator<Edge> j = deletedEdgeSet.iterator(); j.hasNext(); ) {
			Edge edge = j.next();
			//if (edge == null) throw new IllegalStateException("null edge");
			cfg.removeEdge(edge);
			if (STATS) ++numEdgesPruned;
		}

		// Mark edges
		for (Iterator<MarkedEdge> j = markedEdgeList.iterator(); j.hasNext(); ) {
			j.next().apply();
		}
	}

	private ExceptionSet enumerateExceptionTypes(BasicBlock basicBlock)
		throws ClassNotFoundException, DataflowAnalysisException {

		ExceptionSet exceptionTypeSet = new ExceptionSet();
		InstructionHandle pei = basicBlock.getExceptionThrower();
		Instruction ins = pei.getInstruction();

		// Get the exceptions that BCEL knows about.
		// Note that all of these are unchecked.
		ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
		Class[] exceptionList = exceptionThrower.getExceptions();
		for (int i = 0; i < exceptionList.length; ++i) {
			exceptionTypeSet.addImplicit(new ObjectType(exceptionList[i].getName()));
		}

		// Assume that an Error may be thrown by any instruction.
		exceptionTypeSet.addImplicit(Hierarchy.ERROR_TYPE);

		if (ins instanceof ATHROW) {
			// For ATHROW instructions, we generate *two* blocks
			// for which the ATHROW is an exception thrower.
			//
			// - The first, empty basic block, does the null check
			// - The second block, which actually contains the ATHROW,
			//   throws the object on the top of the operand stack
			//
			// We make a special case of the block containing the ATHROW,
			// by removing all of the implicit exceptions,
			// and using type information to figure out what is thrown.

			if (basicBlock.containsInstruction(pei)) {
				// This is the actual ATHROW, not the null check
				// and implicit exceptions.
				exceptionTypeSet.clear();

				TypeFrame frame = typeDataflow.getFactAtLocation(new Location(pei, basicBlock));
	
				// Check whether or not the frame is valid.
				// Sun's javac sometimes emits unreachable code.
				// For example, it will emit code that follows a JSR
				// subroutine call that never returns.
				// If the frame is invalid, then we can just make
				// a conservative assumption that anything could be
				// thrown at this ATHROW.
				if (!frame.isValid()) {
					exceptionTypeSet.addExplicit(Type.THROWABLE);
				} else {
					Type throwType = frame.getTopValue();
					if (!(throwType instanceof ObjectType))
						throw new DataflowAnalysisException("Non object type thrown by " + pei);
					exceptionTypeSet.addExplicit((ObjectType) throwType);
				}
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
				exceptionTypeSet.addExplicit(Hierarchy.EXCEPTION_TYPE);
			} else {
				for (int i = 0; i < declaredExceptionList.length; ++i) {
					exceptionTypeSet.addExplicit(declaredExceptionList[i]);
				}
			}

			exceptionTypeSet.addImplicit(Hierarchy.RUNTIME_EXCEPTION_TYPE);
		}

		if (DEBUG) System.out.println(pei + " can throw " + exceptionTypeSet);

		return exceptionTypeSet;
	}

	/**
	 * Check the reachability of an exception handler.
	 */
	private void checkReachability(Edge edge, ExceptionSet thrownExceptionSet, Set<Edge> deletedEdgeSet,
		List<MarkedEdge> markedEdgeList) throws ClassNotFoundException {

		if (DEBUG) System.out.println("Checking reachability of edge:\n\t" + edge);

		BasicBlock handlerBlock = edge.getTarget();
		CodeExceptionGen handler = handlerBlock.getExceptionGen();
		ObjectType catchType = handler.getCatchType();

		boolean reachable = false;

		if (Hierarchy.isUniversalExceptionHandler(catchType)) {
			// Universal handler: it catches all exceptions
			int flag = thrownExceptionSet.containsCheckedExceptions()
				? CHECKED_EXCEPTIONS_FLAG 
				: EXPLICIT_UNCHECKED_EXCEPTIONS_FLAG;
			markedEdgeList.add(new MarkedEdge(edge, flag));
			thrownExceptionSet.sawUniversal();
			reachable = true;
		} else {
			// Go through the set of thrown execeptions.
			// Any that will DEFINITELY be caught be this handler, remove.
			// Any that MIGHT be caught, but won't definitely be caught,
			// remain.
			for (Iterator<ThrownException> i = thrownExceptionSet.iterator(); i.hasNext(); ) {
				ThrownException thrownException = i.next();
	
				ObjectType thrownType = thrownException.getType();
	
				if (DEBUG) System.out.println("\texception type " + thrownType + ", catch type " + catchType);
	
				if (Hierarchy.isSubtype(thrownType, catchType)) {
					// The thrown exception is a subtype of the catch type,
					// so this exception will DEFINITELY be caught by
					// this handler.
					if (DEBUG) System.out.println("\tException is subtype of catch type: will definitely catch");
					reachable = true;
					i.remove();
				} else if (Hierarchy.isSubtype(catchType, thrownType)) {
					// The thrown exception is a supertype of the catch type,
					// so it MIGHT get caught by this handler.
					if (DEBUG) System.out.println("\tException is supertype of catch type: might catch");
					reachable = true;
				}
			}
	
			if (reachable) {
				// Classify the edge.
				// We assume that if the user bothered to write
				// a reachable exception handler, then the
				// exception edge is "explicit", meaning that it
				// should be assumed to be feasible at runtime.
				int flag = Hierarchy.isUncheckedException(catchType)
					? EXPLICIT_UNCHECKED_EXCEPTIONS_FLAG
					: CHECKED_EXCEPTIONS_FLAG;
				markedEdgeList.add(new MarkedEdge(edge, flag));
			}
		}

		if (!reachable)
			deletedEdgeSet.add(edge);
	}
}

// vim:ts=4
