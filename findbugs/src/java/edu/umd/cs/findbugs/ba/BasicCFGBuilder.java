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
 * Build a control flow graph for a method.
 * This is an ad-hoc implementation which does not do type
 * analysis of the stack, and thus cannot precisely figure out
 * what kind of exceptions can be thrown.  So, the CFGs produced
 * contain many infeasible exception edges.
 *
 * <p> <b>Treatment of JSR and RET instructions</b>: branches to JSR subroutines
 * are <em>not</em> treated as control flow. Instead, they are logically
 * inlined into the current stream of control. Thus, the instructions
 * in a JSR subroutine may appear in multiple basic blocks.
 * This treatment accurately captures the semantics of JSR subroutines
 * in a way that is not possible with control flow edges.
 * However, for instructions in a JSR subroutine, it makes it impossible
 * to know a priori what basic block they are part of, since they
 * may be part of several.
 *
 * <p> FIXME: this class is overdue to be rewritten.
 *
 * @see CFGBuilder
 * @author David Hovemeyer
 */
public class BasicCFGBuilder extends BaseCFGBuilder implements EdgeTypes, CFGBuilderModes {

	private LinkedList<WorkListItem> workList;
	private IdentityHashMap<InstructionHandle, List<InstructionHandle>> exceptionHandlerMap;
	private IdentityHashMap<InstructionHandle, CodeExceptionGen> exceptionGenMap;
	private HashSet<InstructionHandle> bbHandles; // all InstructionHandles assigned to BasicBlocks
	private IdentityHashMap<InstructionHandle, BasicBlock> firstInsToBBMap;
	private int mode;

	private static final boolean NO_RET_MERGE = Boolean.getBoolean("cfgbuilder.noRetMerge");
	private static final boolean DEBUG = Boolean.getBoolean("cfgbuilder.debug");

	/**
	 * Constructor.
	 * @param method the Method to build the CFG for
	 * @param methodGen a MethodGen object whose InstructionHandles will be referenced by the CFG
	 */
	public BasicCFGBuilder(MethodGen methodGen) {
		super(methodGen);
		firstInsToBBMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
		workList = new LinkedList<WorkListItem>();
		exceptionHandlerMap = new IdentityHashMap<InstructionHandle, List<InstructionHandle>>();
		exceptionGenMap = new IdentityHashMap<InstructionHandle, CodeExceptionGen>();
		buildExceptionHandlerMap();
		bbHandles = new HashSet<InstructionHandle>();

		// EXCEPTION_SENSITIVE_MODE is now the default
		mode = EXCEPTION_SENSITIVE_MODE;
	}

	private static final LinkedList<InstructionHandle> emptyStack = new LinkedList<InstructionHandle>();

	/**
	 * A worklist item.
	 */
	private static final class WorkListItem {
		/** First instruction in the basic block. */
		public final InstructionHandle firstInstruction;

		/** The basic block. */
		public final BasicBlock bb;

		/** Stack of JSR instructions. The most recently executed JSR is on top. */
		public final LinkedList<InstructionHandle> jsrStack;

		/**
		 * Constructor.
		 * @param firstInstruction the first instruction in the basic block
		 * @param bb the basic block
		 * @param jsrStack stack of JSR instructions
		 */
		public WorkListItem(InstructionHandle firstInstruction, BasicBlock bb, LinkedList<InstructionHandle> jsrStack) {
			this.firstInstruction = firstInstruction;
			this.bb = bb;
			this.jsrStack = jsrStack;
		}
	}

	/**
	 * Build the control-flow graph.
	 */
	public void build() {
		InstructionHandle first = methodGen.getInstructionList().getStart();
		BasicBlock start = getBlock(first, emptyStack); // adds start block to work list
		addEdge(cfg.getEntry(), start, START_EDGE);// add edge from entry to start block

		// Keep going until worklist is empty.
		// This will construct basic blocks and add normal control flow and
		// handled exception edges.
		while (!workList.isEmpty()) {
			WorkListItem item = (WorkListItem) workList.removeFirst();
			BasicBlock bb = item.bb;
			LinkedList<InstructionHandle> jsrStack = item.jsrStack;

			// Scan through instructions, adding them to the basic block,
			// until we get to a branch or merge (either of which ends the block).
			InstructionHandle handle = item.firstInstruction, next;
			if (DEBUG) System.out.println("START: " + handle);
			TargetEnumeratingVisitor visitor = null;
			boolean isMerge = false;
			boolean isPotentialThrow = false;

			while (true) {
				if (handle == null) throw new IllegalStateException();

				if (DEBUG) System.out.println("ADDING: " + handle);
				bb.addInstruction(handle);

				// This variable will be assigned if control transfers to
				// an instruction which is not the next in the PC value sequence;
				// i.e., for JSR or RET instructions.
				next = null;

				// Mark the instruction as now being a member of a BasicBlock.
				// Note that an instruction may not be part of two basic blocks
				// UNLESS it is reachable by a JSR instruction.
				assert !bbHandles.contains(handle) || inSubroutine(jsrStack);
				bbHandles.add(handle);

				Instruction ins = handle.getInstruction();

				//
				// Handle JSRs, RETs, and branch instructions.
				//
				if (ins instanceof JsrInstruction) {
					// JSR subroutines are inlined into the current
					// control flow path.

					jsrStack.addLast(handle);	// remember where we came from
					JsrInstruction jsr = (JsrInstruction) ins;
					next = jsr.getTarget();		// transfer control to the subroutine
				} else if (ins instanceof RET) {
					// Return control to the instruction following
					// the most recently executed JSR.

					next = jsrStack.removeLast().getNext(); // return control to ins after JSR

					if (!NO_RET_MERGE) {
						isMerge = true;			// conservatively assume that RET ends the basic block
						break;
					}
				} else {
					// See if the instruction is some kind of branch.

					// See if this instruction is a branch (thus ending the BasicBlock)
					visitor = new TargetEnumeratingVisitor(handle, methodGen.getConstantPool());
					if (visitor.isEndOfBasicBlock())
						break;
				}
		
				// If we're in INSTRUCTION_MODE, each instruction is a separate basic block.
				if (mode == INSTRUCTION_MODE) {
					isMerge = true;
					break;
				}
		
				if (next == null)
					next = handle.getNext();

				if (next == null)
					throw new IllegalStateException("No next?: " + handle);

				// See if the next instruction is a control flow merge
				// (which would make the current instruction the end of
				// the basic block.)
				if (isMerge(next)) {
					isMerge = true;
					break;
				}

				// If we're in EXCEPTION_SENSITIVE_MODE and the current instruction
				// is a potential exception thrower, end the basic block.
				if (mode == EXCEPTION_SENSITIVE_MODE && (handle.getInstruction() instanceof ExceptionThrower)) {
					isPotentialThrow = true;
					break;
				}

				// Continue to next instruction
				handle = next;
				assert handle != null;
			}
			if (DEBUG) System.out.println("END: " + handle);

			// Add exception edges from the basic block to all possible handlers.
			addExceptionEdges(bb, jsrStack);

			if (isMerge) {
				// Fall through.
				addFallThroughEdge(bb, next, jsrStack);
			} else if (visitor.instructionIsReturn() || visitor.instructionIsExit()) {
				// Return from method (or process exit, which we treat as method return).
				addEdge(bb, cfg.getExit(), RETURN_EDGE);
			} else if (visitor.instructionIsThrow()) {
				// NOTE: nothing to do - unhandled exceptions will be taken
				// care of after the main worklist loop has completed.
			} else if (isPotentialThrow) {
				// Instruction has been marked as a potential exception thrower,
				// and we're in EXCEPTION_SENSITIVE_MODE.  Add a fall through
				// edge to the next instruction, if there is one.
				if (next != null)
					addFallThroughEdge(bb, next, jsrStack);
			} else {
				// Explicit branch.
				addBranchEdges(bb, visitor, jsrStack);
			}
		}

		// For each basic block containing instructions which can throw
		// exceptions, add unhandled exception edges.  We conservatively
		// assume that any basic block capable of throwing an exception
		// may throw throw the exception out of the method.
		Iterator<BasicBlock> bbIter = cfg.blockIterator();
		while (bbIter.hasNext()) {
			BasicBlock bb = bbIter.next();
			boolean canThrow = false;
			Iterator<InstructionHandle> insIter = bb.instructionIterator();

			while (insIter.hasNext()) {
				InstructionHandle handle = insIter.next();
				Instruction ins = handle.getInstruction();
				if (ins instanceof ExceptionThrower) {
					ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
					java.lang.Class[] exceptionList = exceptionThrower.getExceptions();
					if (exceptionList.length > 0) {
						canThrow = true;
						break;
					}
				}
			}

			if (canThrow)
				// Unhandled exception edges can duplicate ordinary return edges
				addDuplicateEdge(bb, cfg.getExit(), UNHANDLED_EXCEPTION_EDGE);
		}

		if (Debug.VERIFY_INTEGRITY)
			cfg.checkIntegrity();
	}

	/**
	 * Add edges for explicitly handled exceptions.
	 * @param sourceBB the source basic block
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private void addExceptionEdges(BasicBlock sourceBB, LinkedList<InstructionHandle> jsrStack) {
		if (sourceBB.isEmpty())
			return;

		HashSet<InstructionHandle> exceptionHandlerSet = new HashSet<InstructionHandle>();

		{
			// Find set of all exception handlers for each instruction
			// in the basic block
			Iterator<InstructionHandle> i = sourceBB.instructionIterator();
			while (i.hasNext()) {
				InstructionHandle cur = i.next();
				List<InstructionHandle> handlerList = exceptionHandlerMap.get(cur);
				if (handlerList != null)
					exceptionHandlerSet.addAll(handlerList);
			}
		}

		{
			// Add edges from the source BB to all exception handler BBs
			Iterator<InstructionHandle> i = exceptionHandlerSet.iterator();
			while (i.hasNext()) {
				InstructionHandle target= i.next();
				BasicBlock targetBB = getBlock(target, jsrStack);
				addEdge(sourceBB, targetBB, HANDLED_EXCEPTION_EDGE);
			}
		}
	}

	/**
	 * Determine whether or not the instruction whose handle is given
	 * is a control-flow merge.
	 * @param handle the instruction handle
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
					if (DEBUG) System.out.println("MERGE!");
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add fall through edge (when a basic block ends in a control flow merge).
	 * @param sourceBB the source basic block
	 * @param next the first instruction to be executed in the new basic block
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private void addFallThroughEdge(BasicBlock sourceBB, InstructionHandle next, LinkedList<InstructionHandle> jsrStack) {
		if (next == null) throw new IllegalStateException();

		// Fall through to the next instruction.
		BasicBlock successor = getBlock(next, jsrStack);
		addEdge(sourceBB, successor, FALL_THROUGH_EDGE);
	}

	/**
	 * Add edges for explicit branch instruction.
	 * The visitor has accumulated all of the possible branch targets.
	 * @param sourceBB the source basic block
	 * @param visitor the instruction visitor containing the branch targets
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private void addBranchEdges(BasicBlock sourceBB, TargetEnumeratingVisitor visitor, LinkedList<InstructionHandle> jsrStack) {
		// Scan through targets of the last instruction in the block.
		for (Iterator i = visitor.targetIterator(); i.hasNext(); ) {
			Target target = (Target) i.next();
			// Get the target BasicBlock.
			// This will add it to the work list if necessary.
			BasicBlock targetBB = getBlock(target.getTargetInstruction(), jsrStack);
			// Add CFG edge
			// Note that a switch statement may have many labels for the
			// same target.  We do not (currently) consider these as
			// separate edges.
			if (cfg.lookupEdge(sourceBB, targetBB) == null)
				// Add only if no edge with same source and target already exists
				addEdge(sourceBB, targetBB, target.getEdgeType());
		}
	}

	private static final boolean EXCEPTION_SELF_HANDLER_HACK = true;
		//Boolean.getBoolean("cfg.exceptionSelfHandlerHack");

	/**
	 * Build map of instructions to lists of potential exception handlers.
	 * As a side effect, this also builds a map of instructions which are the
	 * start of an exception handler to the CodeExceptionGen object
	 * representing the handler.
	 */
	private void buildExceptionHandlerMap() {
		CodeExceptionGen[] exceptionHandlerList = methodGen.getExceptionHandlers();
		for (int i = 0; i < exceptionHandlerList.length; ++i) {
			CodeExceptionGen exceptionGen = exceptionHandlerList[i];
			InstructionHandle entry = exceptionGen.getHandlerPC();

			// The classfiles for lucene 1.2 have exception handlers that
			// handle themselves!  As a hack, ignore such handlers.
			if (EXCEPTION_SELF_HANDLER_HACK) {
				if (exceptionGen.getStartPC() == entry) {
					//System.out.println("Ignoring exception self-handler in class " + methodGen.getClassName() + "." + methodGen.getName());
					continue;
				}
			}

			exceptionGenMap.put(entry, exceptionGen);

			// VM Spec, §4.7.3.  The end_pc value for an exception handler
			// is exclusive, not inclusive.
			InstructionHandle handle = exceptionGen.getStartPC();
			while (handle != exceptionGen.getEndPC()) {
				List<InstructionHandle> entryList = exceptionHandlerMap.get(handle);
				if (entryList == null) {
					entryList = new LinkedList<InstructionHandle>();
					exceptionHandlerMap.put(handle, entryList);
				}
				entryList.add(entry);
				handle = handle.getNext();
			}
		}
	}

	/**
	 * Get basic block whose given instruction is that given.
	 * If the block already exists, returns it.  Otherwise, creates
	 * a new block and adds it to the work list.
	 * @param ins the handle of the instruction for which we want the basic block
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private BasicBlock getBlock(InstructionHandle ins, LinkedList<InstructionHandle> jsrStack) {
		if (ins == null) throw new IllegalStateException();

		BasicBlock bb = firstInsToBBMap.get(ins);

		if (bb == null) {
			if (DEBUG) System.out.println("Adding BasicBlock for " + ins);
			// Allocate a new basic block
			bb = cfg.allocate();
			firstInsToBBMap.put(ins, bb);
			bb.addInstruction(ins);

			// If this is the entry point of an exception handler, mark it as such
			bb.setExceptionGen(exceptionGenMap.get(ins));

			// Add the block to the worklist
			workList.add(new WorkListItem(ins, bb, cloneJsrStack(jsrStack)));
		}

		return bb;
	}

	/**
	 * Make a copy of the given JSR stack.
	 * We do this because handling JSR and RET instructions modifies the
	 * JSR stack; hence, work list items cannot share them.
	 * @param jsrStack stack of most recently executed JSR instructions
	 */
	private LinkedList<InstructionHandle> cloneJsrStack(LinkedList<InstructionHandle> jsrStack) {
		LinkedList<InstructionHandle> dup = new LinkedList<InstructionHandle>();
		dup.addAll(jsrStack);
		return dup;
	}

	/**
	 * Based on the current JSR stack, determine whether or not
	 * we are in a JSR subroutine.
	 */
	private boolean inSubroutine(LinkedList<InstructionHandle> jsrStack) {
		return !jsrStack.isEmpty();
	}
}

// vim:ts=4
