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
 * (with the usual rule that the stack is cleared of all values exception for the
 * thrown exception).
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

	private static final boolean DEBUG = Boolean.getBoolean("cfgbuilder.debug");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static class WorkListItem {
		public final InstructionHandle start;
		public final BasicBlock basicBlock;
		public final LinkedList<InstructionHandle> jsrStack;
		public boolean handledPEI;

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
	private static final int BRANCH = 0;
	private static final int MERGE = 1;
	private static final int NEXT_IS_PEI = 2;

	/* ----------------------------------------------------------------------
	 * Data members
	 * ---------------------------------------------------------------------- */

	private MethodGen methodGen;
	private ConstantPoolGen cpg;
	private CFG cfg;
	private LinkedList<WorkListItem> workList;
	private IdentityHashMap<InstructionHandle, BasicBlock> basicBlockMap;
	private IdentityHashMap<InstructionHandle, BasicBlock> allHandlesToBasicBlockMap;
	private ExceptionHandlerMap exceptionHandlerMap;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	public BetterCFGBuilder(MethodGen methodGen) {
		this.methodGen = methodGen;
		this.cpg = methodGen.getConstantPool();
		this.cfg = new CFG();
		this.workList = new LinkedList<WorkListItem>();
		this.basicBlockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
		this.allHandlesToBasicBlockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
		this.exceptionHandlerMap = new ExceptionHandlerMap(methodGen);
	}

	public void setMode(int mode) {
	}

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
				basicBlock.setExceptionThrower(true);

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
					basicBlock.setExceptionThrower(true);
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
	
	public CFG getCFG() {
		return cfg;
	}

	/* ----------------------------------------------------------------------
	 * Private methods
	 * ---------------------------------------------------------------------- */

	private boolean isPEI(InstructionHandle handle) {
		Instruction ins = handle.getInstruction();
		//return (ins instanceof ExceptionThrower) && !(ins instanceof ATHROW);
		if (!(ins instanceof ExceptionThrower))
			return false;

		if (ins instanceof ATHROW)
			return false;

		if (ins instanceof ReturnInstruction && !methodGen.isSynchronized())
			return false;

		return true;
	}

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

	private LinkedList<InstructionHandle> cloneJsrStack(LinkedList<InstructionHandle> jsrStack) {
		LinkedList<InstructionHandle> dup = new LinkedList<InstructionHandle>();
		dup.addAll(jsrStack);
		return dup;
	}

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

	private void addEdge(BasicBlock sourceBlock, BasicBlock destBlock, int edgeType) {
		if (DEBUG) System.out.println("Add edge: " + sourceBlock.getId() + " -> " + destBlock.getId() + ": " + Edge.edgeTypeToString(edgeType));
		cfg.addEdge(sourceBlock, destBlock, edgeType);
	}

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
