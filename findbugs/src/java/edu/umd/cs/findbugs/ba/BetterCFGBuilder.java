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
 * An accurate CFGBuilder.  This implementation of CFGBuilder is intended
 * to produce CFGs that are accurate with respect to exceptions.
 * Specifically, exception edges are always inserted <em>before</em>
 * potentially excepting instructions (PEIs).  In general, it is useful and
 * accurate to view the occurance of an exception as precluding the
 * execution of the instruction throwing the exception.  For example, an
 * exception thrown by a MONITORENTER instruction means that the monitor
 * was not actually acquired.
 *
 * <p> Because of the accurate treatment of exceptions, CFGs produced with this
 * CFGBuilder can be used to perform dataflow analysis on.  Assuming that the
 * Java source-to-bytecode compiler generated good code, all dataflow values
 * should merge successfully at control joins, including at exception handlers
 * (with the usual rule that the stack is cleared of all values except for the
 * thrown exception).
 *
 * <p> This CFGBuilder inlines JSR subroutines.  This is the simplest way
 * to accurately capture the semantics of JSR and RET.
 *
 * <p> Things that should be fixed or improved at some point:
 * <ul>
 * <li> ATHROW should really have exception edges both before
 * (NullPointerException if TOS has the null value) and after (the thrown exception)
 * the instruction.  Right now we only have after.
 * <li> Edges should be annotated with bytecode and source line information.
 * <li> UNHANDLED_EXCEPTION edges should be added.
 * </ul>
 *
 * @see CFG
 * @see CFGBuilder
 * @see Dataflow
 * @author David Hovemeyer
 */
public class BetterCFGBuilder implements CFGBuilder, EdgeTypes {

	/** If true, print debug messages. */
	private static final boolean DEBUG = Boolean.getBoolean("cfgbuilder.debug");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * An item on the work list used by the build() method.
	 * It represents a basic block that should be added to the CFG.
	 */
	private static class WorkListItem {
		/** The start instruction for the basic block. */
		public final InstructionHandle start;

		/** The basic block to be constructed. */
		public final BasicBlock basicBlock;

		/** Stack of the most recently executed JSR instructions. */
		public final LinkedList<InstructionHandle> jsrStack;

		/** Set to true if the first instruction in the basic block is a PEI
		    for which we've already created the ETB that has its exception edges. */
		public final boolean handledPEI;

		/**
		 * Constructor.
		 * @param start first instruction
		 * @param basicBlock the basic block to be constructed
		 * @param jsrStack stack of most recently executed JSR instructions
		 * @param handledPEI true if start is a PEI and we've already added its ETB
		 */
		public WorkListItem(InstructionHandle start, BasicBlock basicBlock,
			LinkedList<InstructionHandle> jsrStack, boolean handledPEI) {
			this.start = start;
			this.basicBlock = basicBlock;
			this.jsrStack = jsrStack;
			this.handledPEI = handledPEI;
		}
	}

	/* ----------------------------------------------------------------------
	 * Constants
	 * ---------------------------------------------------------------------- */

	// Reasons for why a basic block was ended.

	/** Basic block ends in a branch. */
	private static final int BRANCH = 0;

	/** Basic block ends in a control merge. */
	private static final int MERGE = 1;

	/** Basic block ends because its fall-through successor is a PEI. */
	private static final int NEXT_IS_PEI = 2;

	/* ----------------------------------------------------------------------
	 * Data members
	 * ---------------------------------------------------------------------- */

	/** The MethodGen we're constructing the CFG for. */
	private MethodGen methodGen;

	/** The ConstantPoolGen for the method. */
	private ConstantPoolGen cpg;

	/** The CFG being constructed. */
	private CFG cfg;

	/** Work list representing basic blocks which need to be constructed. */
	private LinkedList<WorkListItem> workList;

	/** Map of start instructions to the basic block represented by the start instruction. */
	private IdentityHashMap<InstructionHandle, BasicBlock> basicBlockMap;

	/** Map of all instructions to the first basic block they were placed in.
	    Note that because of a JSRs some instructions are in multiple basic blocks. */
	private IdentityHashMap<InstructionHandle, BasicBlock> allHandlesToBasicBlockMap;

	/** Object which allows us to conveniently find exception handlers for an instruction,
	    and to find out which instructions are the start of exception handlers. */
	private ExceptionHandlerMap exceptionHandlerMap;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * @param methodGen the method to build the CFG for
	 */
	public BetterCFGBuilder(MethodGen methodGen) {
		this.methodGen = methodGen;
		this.cpg = methodGen.getConstantPool();
		this.cfg = new CFG();
		this.workList = new LinkedList<WorkListItem>();
		this.basicBlockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
		this.allHandlesToBasicBlockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
		this.exceptionHandlerMap = new ExceptionHandlerMap(methodGen);
	}

	/**
	 * Build the CFG.
	 */
	public void build() {
		BasicBlock startBlock = getBlock(methodGen.getInstructionList().getStart(), new LinkedList<InstructionHandle>());
		addEdge(cfg.getEntry(), startBlock, START_EDGE);

	workListLoop:
		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();

			InstructionHandle start = item.start;
			BasicBlock basicBlock = item.basicBlock;
			LinkedList<InstructionHandle> jsrStack = item.jsrStack;
			boolean handledPEI = item.handledPEI;

			if (DEBUG) System.out.println("START BLOCK " + basicBlock.getId());

			boolean isCanonical = (basicBlockMap.get(start) == basicBlock);
			if (DEBUG) System.out.println("  Block " + basicBlock.getId() + (isCanonical ? " is" : " is not") +
				" the canonical block for " + start);

			// See if the block is an exception handler.
			// Note that when we create an empty ETB for a block which begins
			// with a PEI (see below), the ETB is considered the start of the
			// exception handler, not the block actually containing the PEI.
			if (!handledPEI) {
				CodeExceptionGen codeExceptionGen = exceptionHandlerMap.getHandlerForStartInstruction(start);
				if (codeExceptionGen != null)
					basicBlock.setExceptionGen(codeExceptionGen);
			}

			// If the start instruction is a PEI which we haven't handled yet, then
			// this block is an Exception Thrower Block (ETB).
			if (isPEI(start) && !handledPEI) {
				// This block is an ETB.
				basicBlock.setExceptionThrower(start);

				// Add handled exception edges.
				addExceptionEdges(start, basicBlock, jsrStack);

				// Add fall through edge. Note that we add the work list item for the
				// new block by hand, because getBlock()
				//   (1) sets handledPEI to false, and
				//   (2) maps the start instruction to the new block; the ETB is
				//       the correct block for the start instruction, not the
				//       fall through block
				BasicBlock nextBasicBlock = cfg.allocate();
				addEdge(basicBlock, nextBasicBlock, FALL_THROUGH_EDGE);
				WorkListItem nextItem = new WorkListItem(start, nextBasicBlock, jsrStack, true);
				workList.add(nextItem);

				continue workListLoop;
			}

			InstructionHandle handle = start;
			InstructionHandle next = null;
			TargetEnumeratingVisitor visitor = null;
			int endBlockMode;

			// Add instructions to the basic block
		scanInstructionsLoop:
			while (true) {
				// Add the instruction to the block
				basicBlock.addInstruction(handle);
				if (DEBUG) System.out.println("** Add " + handle + (isPEI(handle) ? " [PEI]" : ""));

				// Except for instructions in JSR subroutines, no instruction should be
				// in more than one basic block.
				if (allHandlesToBasicBlockMap.get(handle) != null && jsrStack.isEmpty())
					throw new IllegalStateException("Instruction in multiple blocks: " + handle);
				allHandlesToBasicBlockMap.put(handle, basicBlock);

				// PEIs should always be the first instruction in the basic block
				if (isPEI(handle) && handle != basicBlock.getFirstInstruction())
					throw new IllegalStateException("PEI is not first instruction in block!");

				// This will be assigned if the potential next instruction in the
				// basic block is something other than what would be returned
				// by handle.getNext().  This only happens for JSR and RET instructions.
				next = null;

				Instruction ins = handle.getInstruction();

				// Handle JSR, RET, and explicit branches.
				if (ins instanceof JsrInstruction) {
					// Remember where we came from
					jsrStack.addLast(handle);

					// Transfer control to the subroutine
					JsrInstruction jsr = (JsrInstruction) ins;
					next = jsr.getTarget();
				} else if (ins instanceof RET) {
					// Return control to instruction after JSR
					next = jsrStack.removeLast().getNext();
				} else if ((visitor = new TargetEnumeratingVisitor(handle, cpg)).isEndOfBasicBlock()) {
					// Basic block ends with explicit branch.
					endBlockMode = BRANCH;
					break scanInstructionsLoop;
				}

				// If control gets here, then the current instruction was not cause
				// for ending the basic block.  Check the next instruction, which may
				// be cause for ending the block.

				if (next == null)
					next = handle.getNext();
				if (next == null)
					throw new IllegalStateException("Falling off end of method: " + handle);

				// Control merge?
				if (isMerge(next)) {
					endBlockMode = MERGE;
					break scanInstructionsLoop;
				}

				// Next instruction is a PEI?
				if (isPEI(next)) {
					endBlockMode = NEXT_IS_PEI;
					break scanInstructionsLoop;
				}

				// The basic block definitely continues to the next instruction.
				handle = next;
			}

			if (DEBUG) dumpBlock(basicBlock);

			// Depending on how the basic block ended, add appropriate edges to the CFG.
			if (next != null) {
				// There is a successor instruction, meaning that this
				// is a control merge, or the block was ended because of a PEI.
				// In either case, just fall through to the successor.

				if (endBlockMode != MERGE && endBlockMode != NEXT_IS_PEI)
					throw new IllegalStateException("next != null, but not merge or PEI");

				BasicBlock nextBlock = getBlock(next, jsrStack);
				addEdge(basicBlock, nextBlock, FALL_THROUGH_EDGE);
			} else {
				// There is no successor instruction, meaning that the block ended
				// in an explicit branch of some sort.

				if (endBlockMode != BRANCH)
					throw new IllegalStateException("next == null, but not branch");

				if (visitor.instructionIsThrow()) {
					// Explicit ATHROW instruction.  Add exception edges,
					// and mark the block as an ETB.
					addExceptionEdges(handle, basicBlock, jsrStack);
					basicBlock.setExceptionThrower(basicBlock.getLastInstruction());
				} else if (visitor.instructionIsReturn() || visitor.instructionIsExit()) {
					// Return or call to System.exit().  In either case,
					// add a return edge.
					addEdge(basicBlock, cfg.getExit(), RETURN_EDGE);
				} else {
					// The TargetEnumeratingVisitor takes care of telling us what the targets are.
					// (This includes the fall through edges for IF branches.)
					// Note that switches may have multiple targets with the same destination;
					// we consider these as a single edge.
					Iterator<Target> i = visitor.targetIterator();
					while (i.hasNext()) {
						Target target = i.next();
						BasicBlock targetBlock = getBlock(target.getTargetInstruction(), jsrStack);

						if (cfg.lookupEdge(basicBlock, targetBlock) == null)
							// Add only if no edge with same source and target already exists
							addEdge(basicBlock, targetBlock, target.getEdgeType());
					}
				}
	
			}
		}
	}
	
	/**
	 * Get the CFG.
	 * Assumes that the build() method has already been called.
	 * @return the CFG
	 */
	public CFG getCFG() {
		return cfg;
	}

	/* ----------------------------------------------------------------------
	 * Private methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Determine whether given instruction is a PEI. PEI means "potentially 
	 * excepting instruction"; i.e., an instruction which might or might not
	 * throw an exception.  Such instructions require special treatment in
	 * the CFG because the exception generally means that the instruction did
	 * not execute.  For some kinds of instructions (e.g., MONITORENTER),
	 * whether or not the instruction executes is very important to dataflow
	 * analysis, since it will affect whether or not the dataflow values
	 * merge properly for exception handlers.
	 *
	 * <p> Note that somewhat counter-intuitively, the ATHROW instruction is
	 * <em>not</em> a PEI.  This is because we treat it as though the exception
	 * is thrown after the instruction executes.  (If we didn't, it would not
	 * be possible for ATHROW to be in a reachable basic block.)
	 *
	 * @param handle the instruction
	 * @return true if the instruction is a PEI, false otherwise
	 */
	private boolean isPEI(InstructionHandle handle) {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof ExceptionThrower))
			return false;

		if (ins instanceof ATHROW)
			return false;

		// Return instructions can throw exceptions only if the method is synchronized
		if (ins instanceof ReturnInstruction && !methodGen.isSynchronized())
			return false;

		return true;
	}

	/**
	 * Get the basic block for given start instruction.
	 * <p> If no basic block exists for this start instruction, a basic block is
	 * created and added to the work list.  Note that because of the way
	 * PEIs are handled, the basic block for a PEI is actually an empty
	 * basic block (an ETB, "exception throwing block") which is where
	 * the exception edges are.  The PEI itself is in the fall-through successor
	 * of the empty ETB.
	 *
	 * <p> If a basic block does already exist for the start instruction,
	 * it is returned.
	 *
	 * @param start the start instruction for the basic block
	 * @param jsrStack stack of the most recently executed JSR instructions
	 * @return the basic block
	 */
	private BasicBlock getBlock(InstructionHandle start, LinkedList<InstructionHandle> jsrStack) {
		BasicBlock basicBlock = basicBlockMap.get(start);
		if (basicBlock == null) {
			basicBlock = cfg.allocate();
			basicBlockMap.put(start, basicBlock);
			WorkListItem item = new WorkListItem(start, basicBlock, cloneJsrStack(jsrStack), false);
			workList.add(item);
		}
		if (DEBUG) System.out.println("** Start ins " + start + " -> block " + basicBlock.getId());
		return basicBlock;
	}

	/**
	 * Add HANDLED_EXCEPTION edges for given basic block.
	 * @param handle the execption-throwing instruction
	 * @param sourceBlock the source basic block
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private void addExceptionEdges(InstructionHandle handle, BasicBlock sourceBlock, LinkedList<InstructionHandle> jsrStack) {
		List<CodeExceptionGen> exceptionHandlerList = exceptionHandlerMap.getHandlerList(handle);
		if (exceptionHandlerList == null)
			return;
		Iterator<CodeExceptionGen> i = exceptionHandlerList.iterator();
		while (i.hasNext()) {
			CodeExceptionGen exceptionHandler = i.next();

			BasicBlock handlerBlock = getBlock(exceptionHandler.getHandlerPC(), jsrStack);
			addEdge(sourceBlock, handlerBlock, HANDLED_EXCEPTION_EDGE);
		}
	}

	/**
	 * Make a copy of given JSR stack.
	 * These are modified, so they can't be shared between work list items.
	 * @param jsrStack the JSR stack
	 */
	private LinkedList<InstructionHandle> cloneJsrStack(LinkedList<InstructionHandle> jsrStack) {
		LinkedList<InstructionHandle> dup = new LinkedList<InstructionHandle>();
		dup.addAll(jsrStack);
		return dup;
	}

	/**
	 * Determine whether or not given instruction is a control merge.
	 * @return true if the instruction is a merge, false if not
	 */
	private boolean isMerge(InstructionHandle handle) {
		if (handle.hasTargeters()) {
			// Check all targeters of this handle to see if any
			// of them are branches.  Note that we don't consider JSR
			// instructions to be branches, since we inline JSR subroutines.
			InstructionTargeter[] targeterList = handle.getTargeters();
			for (int i = 0; i < targeterList.length; ++i) {
				InstructionTargeter targeter = targeterList[i];
				if (targeter instanceof BranchInstruction && !(targeter instanceof JsrInstruction)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add an edge to the CFG.
	 * @param sourceBlock the source basic block
	 * @param destBlock the destination basic block
	 * @param edgeType the edge type (see {@link EdgeTypes EdgeTypes})
	 */
	private void addEdge(BasicBlock sourceBlock, BasicBlock destBlock, int edgeType) {
		if (DEBUG) System.out.println("Add edge: " + sourceBlock.getId() + " -> " + destBlock.getId() + ": " + Edge.edgeTypeToString(edgeType));
		cfg.addEdge(sourceBlock, destBlock, edgeType);
	}

	/**
	 * Dump a basic block for debugging.
	 * @param basicBlock the basic block
	 */
	private void dumpBlock(BasicBlock basicBlock) {
		System.out.println("BLOCK " + basicBlock.getId());
		Iterator<InstructionHandle> i = basicBlock.instructionIterator();
		while (i.hasNext()) {
			InstructionHandle handle = i.next();
			System.out.println(handle.toString());
		}
		System.out.println("END");
	}
}

// vim:ts=4
