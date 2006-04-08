/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

/**
 * Remove obviously infeasible exception edgges, and mark all exception
 * edges of a CFG to indicate
 * <ol>
 * <li>if they can throw a checked exception, and</li>
 * <li>if they throw "explicit" exceptions (thrown via ATHROW or
 *     from a called method which explicitly declares thrown exceptions)</li>
 * </ol>
 * 
 * <p>The goal of this class is to accomplish the same thing as
 * PruneInfeasibleExceptionEdges, but without requiring accurate
 * exception sets to be constructed (which is very slow).</p>
 * 
 * @author David Hovemeyer
 */
public class PruneInfeasibleExceptionEdges2 implements EdgeTypes {
	private static boolean STATS = Boolean.getBoolean("cfg.prune.stats");
	private static int numEdgesPruned;
	static {
		if (STATS) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
                                 public void run() {
					System.err.println("Exception edges pruned: " + numEdgesPruned);
				}
			});
		}
	}
	
	private static boolean DEBUG = Boolean.getBoolean("cfg.prune.debug");

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

	private MethodGen methodGen;
	private CFG cfg;
	private TypeDataflow typeDataflow;
	private Map<BasicBlock, Set<ObjectType>> thrownExceptionSetMap;
	private BitSet hasDeclaredExceptions;
	private List<Edge> removedEdgeList;
	private List<MarkedEdge> markedEdgeList;
	
	/**
	 * Constructor.
	 * 
	 * @param cfg          the CFG to prune
	 * @param methodGen    MethodGen for the method
	 * @param typeDataflow TypeDataflow for the method
	 */
	public PruneInfeasibleExceptionEdges2(CFG cfg, MethodGen methodGen, TypeDataflow typeDataflow) {
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.typeDataflow = typeDataflow;
		this.thrownExceptionSetMap = new HashMap<BasicBlock, Set<ObjectType>>();
		this.hasDeclaredExceptions = new BitSet();
		this.removedEdgeList = new LinkedList<Edge>();
		this.markedEdgeList = new LinkedList<MarkedEdge>();
	}

	/**
	 * Prune the CFG.
	 */
	public void execute() {
		buildThrownExceptionSetMap();
		markObviouslyInfeasibleCatchEdges();
		classifyEdges();
		updateCFG();
	}

	private void buildThrownExceptionSetMap() {
		// Get thrown exception types
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			if (!basicBlock.isExceptionThrower())
				continue;
			recordThrownExceptions(basicBlock);
		}
	}

	private void recordThrownExceptions(BasicBlock basicBlock) {
		HashSet<ObjectType> exceptionSet = new HashSet<ObjectType>();
		if (isAthrowBlock(basicBlock)) {
			exceptionSet.add(getAthrowType(basicBlock));
		} else {
			InstructionHandle thrower = basicBlock.getExceptionThrower();
			Instruction ins = thrower.getInstruction();
			
			if (ins instanceof ExceptionThrower) {
				// Get exceptions defined by the instruction.
				// These are the "implicit" exceptions.
				Class[] thrownExceptionTypes = ((ExceptionThrower) ins).getExceptions();
				for (Class thrownException : thrownExceptionTypes) {
					exceptionSet.add(ObjectTypeFactory.getInstance(thrownException.getName()));
				}
				
				if (ins instanceof InvokeInstruction) {
					// Look up the declared exceptions.
					// These are the "explicit" exceptions.
					try {
						ObjectType[] declaredExceptionList =
							Hierarchy.findDeclaredExceptions((InvokeInstruction) ins, methodGen.getConstantPool());
						if (declaredExceptionList == null) {
							if (DEBUG) {
								System.err.println("Unknown declared exceptions for call to  " +
										SignatureConverter.convertMethodSignature((InvokeInstruction)ins,methodGen.getConstantPool()) +
										" in " +
										SignatureConverter.convertMethodSignature(methodGen));
							}
							declaredExceptionList = new ObjectType[0];
						}
						
						if (declaredExceptionList.length > 0) {
							hasDeclaredExceptions.set(basicBlock.getId());
							for (ObjectType declaredException : declaredExceptionList) {
								exceptionSet.add(declaredException);
							}
						}
					} catch (ClassNotFoundException e) {
						AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
					}
				}
			}
		}
		thrownExceptionSetMap.put(basicBlock, exceptionSet);
	}

	private boolean isAthrowBlock(BasicBlock basicBlock) {
		return (basicBlock.isExceptionThrower() && !basicBlock.isEmpty());
	}

	private ObjectType getAthrowType(BasicBlock sourceBlock) {
		try {
			// See what the exception type is.
			Location location = new Location(sourceBlock.getExceptionThrower(), sourceBlock);
			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (frame.isValid()) {
				Type tos = frame.getTopValue();
				if (tos instanceof ObjectType)
					return (ObjectType) tos;
			}
		} catch (DataflowAnalysisException e) {
			// Ignore
		}
		
		return ObjectType.THROWABLE; // Should never happen
	}

	private void markObviouslyInfeasibleCatchEdges() {
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock sourceBlock = i.next();
			if (!sourceBlock.isExceptionThrower())
				continue;
			
			Set<ObjectType> exceptionSet = thrownExceptionSetMap.get(sourceBlock);
			
			for (Iterator<Edge> j = cfg.outgoingEdgeIterator(sourceBlock); j.hasNext();) {
				Edge edge = j.next();
				
				if (edge.getType() != HANDLED_EXCEPTION_EDGE)
					continue;
				
				BasicBlock catchBlock = edge.getTarget();
				
				ObjectType catchType = catchBlock.getExceptionGen().getCatchType();
				if (catchType == null) {
					catchType = ObjectType.THROWABLE;
				}

				boolean feasible = handleException(catchType, exceptionSet);
				if (!feasible) {
					removedEdgeList.add(edge);
					if (STATS) {
						++numEdgesPruned;
					}
				}
			}
		}
	}

	private boolean handleException(ObjectType catchType, Set<ObjectType> exceptionSet) {
		boolean feasible = false;
		
		for (Iterator<ObjectType> i = exceptionSet.iterator(); i.hasNext(); ) {
			ObjectType exceptionType = i.next();
			
			try {
				// If exception type is a subtype of the catch type, then it MUST
				// be caught and we can remove it from the exception set.
				// If the exception type is a supertype of the catch type, then it MAY
				// be caught, and we at least know that the catch block might be feasible.
				if (Hierarchy.isSubtype(exceptionType, catchType)) {
					feasible = true;
					i.remove();
				} else if (Hierarchy.isSubtype(catchType, exceptionType)) {
					feasible = true;
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			}
		}
		
		return feasible;
	}

	private void classifyEdges() {
		// Classify CFG edges
		for (Iterator<Edge> i = cfg.edgeIterator(); i.hasNext();) {
			Edge edge = i.next();

			if (!edge.isExceptionEdge())
				continue;

			BasicBlock sourceBlock = edge.getSource();
			Set<ObjectType> exceptionSet = thrownExceptionSetMap.get(sourceBlock);
			
			int edgeFlags = 0;
			if (isAthrowBlock(sourceBlock) || hasDeclaredExceptions.get(sourceBlock.getId())) {
				edgeFlags |= EXPLICIT_EXCEPTIONS_FLAG;
			}
			if (containsCheckedExceptions(exceptionSet)) {
				edgeFlags |= CHECKED_EXCEPTIONS_FLAG; 
			}
			
			markedEdgeList.add(new MarkedEdge(edge, edgeFlags));
		}
	}

	private boolean containsCheckedExceptions(Set<ObjectType> exceptionSet) {
		for (ObjectType exceptionType : exceptionSet) {
			if (isCheckedException(exceptionType))
				return true;
		}
		return false;
	}

	private boolean isCheckedException(ObjectType exceptionType) {
		try {
			return Hierarchy.isSubtype(exceptionType, Hierarchy.EXCEPTION_TYPE)
				&& !Hierarchy.isSubtype(exceptionType, Hierarchy.RUNTIME_EXCEPTION_TYPE);
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			return true;
		}
	}

	private void updateCFG() {
		for (MarkedEdge markedEdge : markedEdgeList) {
			markedEdge.apply();
		}
		for (Edge edge : removedEdgeList) {
			cfg.removeEdge(edge);
		}
	}
}
