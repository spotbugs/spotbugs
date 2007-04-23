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

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ExceptionThrower;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.JsrInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A CFGBuilder that really tries to construct accurate control flow graphs.
 * The CFGs it creates have accurate exception edges, and have accurately
 * inlined JSR subroutines.
 *
 * @author David Hovemeyer
 * @see CFG
 */
public class BetterCFGBuilder2 implements CFGBuilder, EdgeTypes, Debug {

	private static final boolean DEBUG = SystemProperties.getBoolean("cfgbuilder.debug");

	// TODO: don't forget to change BasicBlock so ATHROW is considered to have a null check

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * A work list item for creating the CFG for a subroutine.
	 */
	private static class WorkListItem {
		private final InstructionHandle start;
		private final BasicBlock basicBlock;

		/**
		 * Constructor.
		 *
		 * @param start      first instruction in the basic block
		 * @param basicBlock the basic block to build
		 */
		public WorkListItem(InstructionHandle start, BasicBlock basicBlock) {
			this.start = start;
			this.basicBlock = basicBlock;
		}

		/**
		 * Get the start instruction.
		 */
		public InstructionHandle getStartInstruction() {
			return start;
		}

		/**
		 * Get the basic block.
		 */
		public BasicBlock getBasicBlock() {
			return basicBlock;
		}
	}

	/**
	 * A placeholder for a control edge that escapes its subroutine to return
	 * control back to an outer (calling) subroutine.  It will turn into a
	 * real edge during inlining.
	 */
	private static class EscapeTarget {
		private final InstructionHandle target;
		private final int edgeType;

		/**
		 * Constructor.
		 *
		 * @param target   the target instruction in a calling subroutine
		 * @param edgeType the type of edge that should be created when the
		 *                 subroutine is inlined into its calling context
		 */
		public EscapeTarget(InstructionHandle target, int edgeType) {
			this.target = target;
			this.edgeType = edgeType;
		}

		/**
		 * Get the target instruction.
		 */
		public InstructionHandle getTarget() {
			return target;
		}

		/**
		 * Get the edge type.
		 */
		public int getEdgeType() {
			return edgeType;
		}
	}

	private static final LinkedList<EscapeTarget> emptyEscapeTargetList = new LinkedList<EscapeTarget>();

	/**
	 * JSR subroutine.  The top level subroutine is where execution starts.
	 * Each subroutine has its own CFG.  Eventually,
	 * all JSR subroutines will be inlined into the top level subroutine,
	 * resulting in an accurate CFG for the overall method.
	 */
	private class Subroutine {
		private final InstructionHandle start;
		private final BitSet instructionSet;
		private final CFG cfg;
		private IdentityHashMap<InstructionHandle, BasicBlock> blockMap;
		private IdentityHashMap<BasicBlock, List<EscapeTarget>> escapeTargetListMap;
		private BitSet returnBlockSet;
		private BitSet exitBlockSet;
		private BitSet unhandledExceptionBlockSet;
		private LinkedList<WorkListItem> workList;

		/**
		 * Constructor.
		 *
		 * @param start the start instruction for the subroutine
		 */
		public Subroutine(InstructionHandle start) {
			this.start = start;
			this.instructionSet = new BitSet();
			this.cfg = new CFG();
			this.blockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
			this.escapeTargetListMap = new IdentityHashMap<BasicBlock, List<EscapeTarget>>();
			this.returnBlockSet = new BitSet();
			this.exitBlockSet = new BitSet();
			this.unhandledExceptionBlockSet = new BitSet();
			this.workList = new LinkedList<WorkListItem>();
		}

		/**
		 * Get the start instruction.
		 */
		public InstructionHandle getStartInstruction() {
			return start;
		}

		/**
		 * Allocate a new basic block in the subroutine.
		 */
		public BasicBlock allocateBasicBlock() {
			return cfg.allocate();
		}

		/**
		 * Add a work list item for a basic block to be constructed.
		 */
		public void addItem(WorkListItem item) {
			workList.add(item);
		}

		/**
		 * Are there more work list items?
		 */
		public boolean hasMoreWork() {
			return !workList.isEmpty();
		}

		/**
		 * Get the next work list item.
		 */
		public WorkListItem nextItem() {
			return workList.removeFirst();
		}

		/**
		 * Get the entry block for the subroutine's CFG.
		 */
		public BasicBlock getEntry() {
			return cfg.getEntry();
		}

		/**
		 * Get the exit block for the subroutine's CFG.
		 */
		public BasicBlock getExit() {
			return cfg.getExit();
		}

		/**
		 * Get the start block for the subroutine's CFG.
		 * (I.e., the block containing the start instruction.)
		 */
		public BasicBlock getStartBlock() {
			return getBlock(start);
		}

		/**
		 * Get the subroutine's CFG.
		 */
		public CFG getCFG() {
			return cfg;
		}

		/**
		 * Add an instruction to the subroutine.
		 * We keep track of which instructions are part of which subroutines.
		 * No instruction may be part of more than one subroutine.
		 *
		 * @param handle the instruction to be added to the subroutine
		 */
		public void addInstruction(InstructionHandle handle) throws CFGBuilderException {
			int position = handle.getPosition();
			if (usedInstructionSet.get(position))
				throw new CFGBuilderException("Instruction " + handle + " visited in multiple subroutines");
			instructionSet.set(position);
			usedInstructionSet.set(position);
		}

		/**
		 * Is the given instruction part of this subroutine?
		 */
		public boolean containsInstruction(InstructionHandle handle) {
			return instructionSet.get(handle.getPosition());
		}

		/**
		 * Get the basic block in the subroutine for the given instruction.
		 * If the block doesn't exist yet, it is created, and a work list
		 * item is added which will populate it.  Note that if start is
		 * an exception thrower, the block returned will be its ETB.
		 *
		 * @param start the start instruction for the block
		 * @return the basic block for the instruction
		 */
		public BasicBlock getBlock(InstructionHandle start) {
			BasicBlock block = blockMap.get(start);
			if (block == null) {
				block = allocateBasicBlock();
				blockMap.put(start, block);

				// Block is an exception handler?
				CodeExceptionGen exceptionGen = exceptionHandlerMap.getHandlerForStartInstruction(start);
				if (exceptionGen != null)
					block.setExceptionGen(exceptionGen);

				addItem(new WorkListItem(start, block));
			}
			return block;
		}

		/**
		 * Indicate that the method returns at the end of the given block.
		 *
		 * @param block the returning block
		 */
		public void setReturnBlock(BasicBlock block) {
			returnBlockSet.set(block.getId());
		}

		/**
		 * Does the method return at the end of this block?
		 */
		public boolean isReturnBlock(BasicBlock block) {
			return returnBlockSet.get(block.getId());
		}

		/**
		 * Indicate that System.exit() is called at the end of the given block.
		 *
		 * @param block the exiting block
		 */
		public void setExitBlock(BasicBlock block) {
			exitBlockSet.set(block.getId());
		}

		/**
		 * Is System.exit() called at the end of this block?
		 */
		public boolean isExitBlock(BasicBlock block) {
			return exitBlockSet.get(block.getId());
		}

		/**
		 * Indicate that an unhandled exception may be thrown by
		 * the given block.
		 *
		 * @param block the block throwing an unhandled exception
		 */
		public void setUnhandledExceptionBlock(BasicBlock block) {
			unhandledExceptionBlockSet.set(block.getId());
		}

		/**
		 * Does this block throw an unhandled exception?
		 */
		public boolean isUnhandledExceptionBlock(BasicBlock block) {
			return unhandledExceptionBlockSet.get(block.getId());
		}

		/**
		 * Add a control flow edge to the subroutine.
		 * If the control target has not yet been added to the subroutine,
		 * a new work list item is added.  If the control target is in
		 * another subroutine, an EscapeTarget is added.
		 *
		 * @param sourceBlock the source basic block
		 * @param target      the control target
		 * @param edgeType    the type of control edge
		 */
		public void addEdgeAndExplore(BasicBlock sourceBlock, InstructionHandle target, int edgeType) {
			if (usedInstructionSet.get(target.getPosition()) && !containsInstruction(target)) {
				// Control escapes this subroutine
				List<EscapeTarget> escapeTargetList = escapeTargetListMap.get(sourceBlock);
				if (escapeTargetList == null) {
					escapeTargetList = new LinkedList<EscapeTarget>();
					escapeTargetListMap.put(sourceBlock, escapeTargetList);
				}
				escapeTargetList.add(new EscapeTarget(target, edgeType));
			} else {
				// Edge within the current subroutine
				BasicBlock targetBlock = getBlock(target);
				addEdge(sourceBlock, targetBlock, edgeType);
			}
		}

		/**
		 * Add an edge to the subroutine's CFG.
		 *
		 * @param sourceBlock the source basic block
		 * @param destBlock   the destination basic block
		 * @param edgeType    the type of edge
		 */
		public void addEdge(BasicBlock sourceBlock, BasicBlock destBlock, int edgeType) {
			if (VERIFY_INTEGRITY) {
				if (destBlock.isExceptionHandler() && edgeType != HANDLED_EXCEPTION_EDGE)
					throw new IllegalStateException("In method " + SignatureConverter.convertMethodSignature(methodGen) +
							": exception handler " + destBlock.getFirstInstruction() + " reachable by non exception edge type " +
							edgeType);
			}
			cfg.createEdge(sourceBlock, destBlock, edgeType);
		}

		/**
		 * Get an Iterator over the EscapeTargets of given basic block.
		 *
		 * @param sourceBlock the basic block
		 * @return an Iterator over the EscapeTargets
		 */
		public Iterator<EscapeTarget> escapeTargetIterator(BasicBlock sourceBlock) {
			List<EscapeTarget> escapeTargetList = escapeTargetListMap.get(sourceBlock);
			if (escapeTargetList == null)
				escapeTargetList = emptyEscapeTargetList;
			return escapeTargetList.iterator();
		}
	}

	/**
	 * Inlining context.
	 * This essentially consists of a inlining site and
	 * a subroutine to be inlined.  A stack of calling contexts
	 * is maintained in order to resolve EscapeTargets.
	 */
	private static class Context {
		private final Context caller;
		private final Subroutine subroutine;
		private final CFG result;
		private final IdentityHashMap<BasicBlock, BasicBlock> blockMap;
		private final LinkedList<BasicBlock> workList;

		/**
		 * Constructor.
		 *
		 * @param caller     the calling context
		 * @param subroutine the subroutine being inlined
		 * @param result     the result CFG
		 */
		public Context(@Nullable Context caller, Subroutine subroutine, CFG result) {
			this.caller = caller;
			this.subroutine = subroutine;
			this.result = result;
			this.blockMap = new IdentityHashMap<BasicBlock, BasicBlock>();
			this.workList = new LinkedList<BasicBlock>();
		}

		/**
		 * Get the calling context.
		 */
		public Context getCaller() {
			return caller;
		}

		/**
		 * Get the subroutine being inlined.
		 */
		public Subroutine getSubroutine() {
			return subroutine;
		}

		/**
		 * Get the result CFG.
		 */
		public CFG getResult() {
			return result;
		}

		/**
		 * Add a basic block to the inlining work list.
		 */
		public void addItem(BasicBlock item) {
			workList.add(item);
		}

		/**
		 * Are there more work list items?
		 */
		public boolean hasMoreWork() {
			return !workList.isEmpty();
		}

		/**
		 * Get the next work list item (basic block to be inlined).
		 */
		public BasicBlock nextItem() {
			return workList.removeFirst();
		}

		/**
		 * Map a basic block in a subroutine to the corresponding block
		 * in the resulting CFG.
		 *
		 * @param subBlock    the subroutine block
		 * @param resultBlock the result CFG block
		 */
		public void mapBlock(BasicBlock subBlock, BasicBlock resultBlock) {
			blockMap.put(subBlock, resultBlock);
		}

		/**
		 * Get the block in the result CFG corresponding to the given
		 * subroutine block.
		 *
		 * @param subBlock the subroutine block
		 * @return the result CFG block
		 */
		public BasicBlock getBlock(BasicBlock subBlock) {
			BasicBlock resultBlock = blockMap.get(subBlock);
			if (resultBlock == null) {
				resultBlock = result.allocate();
				blockMap.put(subBlock, resultBlock);
				workList.add(subBlock);
			}
			return resultBlock;
		}

		/**
		 * Check to ensure that this context is not the result of recursion.
		 */
		public void checkForRecursion() throws CFGBuilderException {
			Context callerContext = caller;

			while (callerContext != null) {
				if (callerContext.subroutine == this.subroutine)
					throw new CFGBuilderException("JSR recursion detected!");
				callerContext = callerContext.caller;
			}
		}
	}

	/* ----------------------------------------------------------------------
	 * Instance data
	 * ---------------------------------------------------------------------- */

	private MethodGen methodGen;
	private ConstantPoolGen cpg;
	private ExceptionHandlerMap exceptionHandlerMap;
	private BitSet usedInstructionSet;
	private LinkedList<Subroutine> subroutineWorkList;
	private IdentityHashMap<InstructionHandle, Subroutine> jsrSubroutineMap;
	private Subroutine topLevelSubroutine;
	private CFG cfg;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 *
	 * @param methodGen the method to build a CFG for
	 */
	public BetterCFGBuilder2(@NonNull MethodGen methodGen) {
		this.methodGen = methodGen;
		this.cpg = methodGen.getConstantPool();
		this.exceptionHandlerMap = new ExceptionHandlerMap(methodGen);
		this.usedInstructionSet = new BitSet();
		this.jsrSubroutineMap = new IdentityHashMap<InstructionHandle, Subroutine>();
		this.subroutineWorkList = new LinkedList<Subroutine>();
	}

	public void build() throws CFGBuilderException {
		topLevelSubroutine = new Subroutine(methodGen.getInstructionList().getStart());
		subroutineWorkList.add(topLevelSubroutine);

		// Build top level subroutine and all JSR subroutines
		while (!subroutineWorkList.isEmpty()) {
			Subroutine subroutine = subroutineWorkList.removeFirst();
			if (DEBUG) System.out.println("Starting subroutine " + subroutine.getStartInstruction());
			build(subroutine);
		}

		// Inline everything into the top level subroutine
		cfg = inlineAll();

		// Add a NOP instruction to the entry block.
		// This allows analyses to construct a Location
		// representing the entry to the method.
		BasicBlock entryBlock = cfg.getEntry();
		InstructionList il = new InstructionList();
		entryBlock.addInstruction(il.append(new NOP()));

		if (VERIFY_INTEGRITY)
			cfg.checkIntegrity();
	}

	public CFG getCFG() {
		return cfg;
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	/**
	 * Build a subroutine.
	 * We iteratively add basic blocks to the subroutine
	 * until there are no more blocks reachable from the calling context.
	 * As JSR instructions are encountered, new Subroutines are added
	 * to the subroutine work list.
	 *
	 * @param subroutine the subroutine
	 */
	private void build(Subroutine subroutine) throws CFGBuilderException {
		// Prime the work list
		subroutine.addEdgeAndExplore(subroutine.getEntry(), subroutine.getStartInstruction(), START_EDGE);

		// Keep going until all basic blocks in the subroutine have been added
		while (subroutine.hasMoreWork()) {
			WorkListItem item = subroutine.nextItem();

			InstructionHandle handle = item.getStartInstruction();
			BasicBlock basicBlock = item.getBasicBlock();

			// Add exception handler block (ETB) for exception-throwing instructions
			if (isPEI(handle)) {
				if (DEBUG) System.out.println("ETB block " + basicBlock.getId() + " for " + handle);
				handleExceptions(subroutine, handle, basicBlock);
				BasicBlock body = subroutine.allocateBasicBlock();
				subroutine.addEdge(basicBlock, body, FALL_THROUGH_EDGE);
				basicBlock = body;
			}

			if (DEBUG) System.out.println("BODY block " + basicBlock.getId() + " for " + handle);

			if (!basicBlock.isEmpty())
				throw new IllegalStateException("Block isn't empty!");

			// Add instructions until we get to the end of the block
			boolean endOfBasicBlock = false;
			do {
				Instruction ins = handle.getInstruction();

				// Add the instruction to the block
				if (DEBUG) System.out.println("BB " + basicBlock.getId() + ": adding" + handle);
				basicBlock.addInstruction(handle);
				subroutine.addInstruction(handle);

				short opcode = ins.getOpcode();

				// TODO: should check instruction to ensure that in a JSR subroutine
				// no assignments are made to the local containing the return address.
				// if (ins instanceof ASTORE) ...

				if (opcode == Constants.JSR || opcode == Constants.JSR_W) {
					// Find JSR subroutine, add it to subroutine work list if
					// we haven't built a CFG for it yet
					JsrInstruction jsr = (JsrInstruction) ins;
					InstructionHandle jsrTarget = jsr.getTarget();
					Subroutine jsrSubroutine = jsrSubroutineMap.get(jsrTarget);
					if (jsrSubroutine == null) {
						jsrSubroutine = new Subroutine(jsrTarget);
						jsrSubroutineMap.put(jsrTarget, jsrSubroutine);
						subroutineWorkList.add(jsrSubroutine);
					}

					// This ends the basic block.
					// Add a JSR_EDGE to the successor.
					// It will be replaced later by the inlined JSR subroutine.
					subroutine.addEdgeAndExplore(basicBlock, handle.getNext(), JSR_EDGE);
					endOfBasicBlock = true;
				} else if (opcode == Constants.RET) {
					// End of JSR subroutine
					subroutine.addEdge(basicBlock, subroutine.getExit(), RET_EDGE);
					endOfBasicBlock = true;
				} else {
					TargetEnumeratingVisitor visitor = new TargetEnumeratingVisitor(handle, cpg);
					if (visitor.isEndOfBasicBlock()) {
						endOfBasicBlock = true;

						// Add control edges as appropriate
						if (visitor.instructionIsThrow()) {
							handleExceptions(subroutine, handle, basicBlock);
						} else if (visitor.instructionIsExit()) {
							subroutine.setExitBlock(basicBlock);
						} else if (visitor.instructionIsReturn()) {
							subroutine.setReturnBlock(basicBlock);
						} else {
							Iterator<Target> i = visitor.targetIterator();
							while (i.hasNext()) {
								Target target = i.next();
								subroutine.addEdgeAndExplore(basicBlock, target.getTargetInstruction(),
										target.getEdgeType());
							}
						}
					}
				}

				if (!endOfBasicBlock) {
					InstructionHandle next = handle.getNext();
					if (next == null)
						throw new CFGBuilderException("Control falls off end of method: " + handle);

					// Is the next instruction a control merge or a PEI?
					if (isMerge(next) || isPEI(next)) {
						subroutine.addEdgeAndExplore(basicBlock, next, FALL_THROUGH_EDGE);
						endOfBasicBlock = true;
					} else {
						// Basic block continues
						handle = next;
					}
				}

			} while (!endOfBasicBlock);
		}
	}

	/**
	 * Add exception edges for given instruction.
	 *
	 * @param subroutine the subroutine containing the instruction
	 * @param pei        the instruction which throws an exception
	 * @param etb        the exception thrower block (ETB) for the instruction
	 */
	private void handleExceptions(Subroutine subroutine, InstructionHandle pei, BasicBlock etb) {
		etb.setExceptionThrower(pei);

		// Remember whether or not a universal exception handler
		// is reachable.  If so, then we know that exceptions raised
		// at this instruction cannot propagate out of the method.
		boolean sawUniversalExceptionHandler = false;

		List<CodeExceptionGen> exceptionHandlerList = exceptionHandlerMap.getHandlerList(pei);
		if (exceptionHandlerList != null) {
			for (CodeExceptionGen exceptionHandler : exceptionHandlerList) {
				InstructionHandle handlerStart = exceptionHandler.getHandlerPC();
				subroutine.addEdgeAndExplore(etb, handlerStart, HANDLED_EXCEPTION_EDGE);

				if (Hierarchy.isUniversalExceptionHandler(exceptionHandler.getCatchType()))
					sawUniversalExceptionHandler = true;
			}
		}

		// If required, mark this block as throwing an unhandled exception.
		// For now, we assume that if there is no reachable handler that handles
		// ANY exception type, then the exception can be thrown out of the method.
		if (!sawUniversalExceptionHandler) {
			if (DEBUG) System.out.println("Adding unhandled exception edge from " + pei);
			subroutine.setUnhandledExceptionBlock(etb);
		}
	}

	/**
	 * Return whether or not the given instruction can throw exceptions.
	 *
	 * @param handle the instruction
	 * @return true if the instruction can throw an exception, false otherwise
	 */
	private boolean isPEI(InstructionHandle handle) {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof ExceptionThrower))
			return false;

		if (ins instanceof NEW) return false;
		// if (ins instanceof ATHROW) return false;
		if (ins instanceof GETSTATIC) return false;
		if (ins instanceof PUTSTATIC) return false;
		if (ins instanceof ReturnInstruction) return false;
		if (ins instanceof INSTANCEOF) return false;
		// if (ins instanceof INVOKESTATIC) return false;
		if (ins instanceof MONITORENTER) return false;
		if (ins instanceof MONITOREXIT) return false;
		return true;

	}
	/**
	 * Determine whether or not the given instruction is a control flow merge.
	 *
	 * @param handle the instruction
	 * @return true if the instruction is a control merge, false otherwise
	 */
	private static boolean isMerge(InstructionHandle handle) {
		if (handle.hasTargeters()) {
			// Check all targeters of this handle to see if any
			// of them are branches.  If so, the instruction is a merge.
			InstructionTargeter[] targeterList = handle.getTargeters();
			for (InstructionTargeter targeter : targeterList) {
				if (targeter instanceof BranchInstruction)
					return true;
			}
		}
		return false;
	}

	/**
	 * Inline all JSR subroutines into the top-level subroutine.
	 * This produces a complete CFG for the entire method, in which
	 * all JSR subroutines are inlined.
	 *
	 * @return the CFG for the method
	 */
	private CFG inlineAll() throws CFGBuilderException {
		CFG result = new CFG();

		Context rootContext = new Context(null, topLevelSubroutine, result);
		rootContext.mapBlock(topLevelSubroutine.getEntry(), result.getEntry());
		rootContext.mapBlock(topLevelSubroutine.getExit(), result.getExit());

		BasicBlock resultStartBlock = rootContext.getBlock(topLevelSubroutine.getStartBlock());
		result.createEdge(result.getEntry(), resultStartBlock, START_EDGE);

		inline(rootContext);

		return result;
	}

	/**
	 * Inline a subroutine into a calling context.
	 *
	 * @param context the Context
	 */
	public void inline(Context context) throws CFGBuilderException {

		CFG result = context.getResult();

		// Check to ensure we're not trying to inline something that is recursive
		context.checkForRecursion();

		Subroutine subroutine = context.getSubroutine();
		CFG subCFG = subroutine.getCFG();

		while (context.hasMoreWork()) {
			BasicBlock subBlock = context.nextItem();
			BasicBlock resultBlock = context.getBlock(subBlock);

			// Mark blocks which are in JSR subroutines
			resultBlock.setInJSRSubroutine(context.getCaller() != null);

			// Copy instructions into the result block
			BasicBlock.InstructionIterator insIter = subBlock.instructionIterator();
			while (insIter.hasNext()) {
				InstructionHandle handle = insIter.next();
				resultBlock.addInstruction(handle);
			}

			// Set exception thrower status
			if (subBlock.isExceptionThrower())
				resultBlock.setExceptionThrower(subBlock.getExceptionThrower());

			// Set exception handler status
			if (subBlock.isExceptionHandler())
				resultBlock.setExceptionGen(subBlock.getExceptionGen());

			// Add control edges (including inlining JSR subroutines)
			Iterator<Edge> edgeIter = subCFG.outgoingEdgeIterator(subBlock);
			while (edgeIter.hasNext()) {
				Edge edge = edgeIter.next();
				int edgeType = edge.getType();

				if (edgeType == JSR_EDGE) {
					// Inline a JSR subroutine...

					// Create a new Context
					InstructionHandle jsrHandle = subBlock.getLastInstruction();
					JsrInstruction jsr = (JsrInstruction) jsrHandle.getInstruction();
					Subroutine jsrSub = jsrSubroutineMap.get(jsr.getTarget());
					Context jsrContext = new Context(context, jsrSub, context.getResult());

					// The start block in the JSR subroutine maps to the first
					// inlined block in the result CFG.
					BasicBlock resultJSRStartBlock = jsrContext.getBlock(jsrSub.getStartBlock());
					result.createEdge(resultBlock, resultJSRStartBlock, GOTO_EDGE);

					// The exit block in the JSR subroutine maps to the result block
					// corresponding to the instruction following the JSR.
					// (I.e., that is where control returns after the execution of
					// the JSR subroutine.)
					BasicBlock subJSRSuccessorBlock = subroutine.getBlock(jsrHandle.getNext());
					BasicBlock resultJSRSuccessorBlock = context.getBlock(subJSRSuccessorBlock);
					jsrContext.mapBlock(jsrSub.getExit(), resultJSRSuccessorBlock);

					// Inline the JSR subroutine
					inline(jsrContext);
				} else {
					// Ordinary control edge
					BasicBlock resultTarget = context.getBlock(edge.getTarget());
					result.createEdge(resultBlock, resultTarget, edge.getType());
				}
			}

			// Add control edges for escape targets
			Iterator<EscapeTarget> escapeTargetIter = subroutine.escapeTargetIterator(subBlock);
			while (escapeTargetIter.hasNext()) {
				EscapeTarget escapeTarget = escapeTargetIter.next();
				InstructionHandle targetInstruction = escapeTarget.getTarget();

				// Look for the calling context which has the target instruction
				Context caller = context.getCaller();
				while (caller != null) {
					if (caller.getSubroutine().containsInstruction(targetInstruction))
						break;
					caller = caller.getCaller();
				}

				if (caller == null)
					throw new CFGBuilderException("Unknown caller for escape target " + targetInstruction +
							" referenced by " + context.getSubroutine().getStartInstruction());

				// Find result block in caller
				BasicBlock subCallerTargetBlock = caller.getSubroutine().getBlock(targetInstruction);
				BasicBlock resultCallerTargetBlock = caller.getBlock(subCallerTargetBlock);

				// Add an edge to caller context
				result.createEdge(resultBlock, resultCallerTargetBlock, escapeTarget.getEdgeType());
			}

			// If the block returns from the method, add a return edge
			if (subroutine.isReturnBlock(subBlock)) {
				result.createEdge(resultBlock, result.getExit(), RETURN_EDGE);
			}

			// If the block calls System.exit(), add an exit edge
			if (subroutine.isExitBlock(subBlock)) {
				result.createEdge(resultBlock, result.getExit(), EXIT_EDGE);
			}

			// If the block throws an unhandled exception, add an unhandled
			// exception edge
			if (subroutine.isUnhandledExceptionBlock(subBlock)) {
				result.createEdge(resultBlock, result.getExit(), UNHANDLED_EXCEPTION_EDGE);
			}

		}

/*
		while (blocks are left) {

			get block from subroutine
			get corresponding block from result
			copy instructions into result block

			if (block terminated by JSR) {
				get JSR subroutine
				create new context
				create GOTO edge from current result block to start block of new inlined context
				map subroutine exit block to result JSR successor block
				inline (new context, result)
			} else {
				for each outgoing edge {
					map each target to result blocks (add block to to work list if needed)
					add edges to result
				}

				for each outgoing escape target {
					add edges into blocks in outer contexts (adding those blocks to outer work list if needed)
				}

				if (block returns) {
					add return edge from result block to result CFG exit block
				}

				if (block calls System.exit()) {
					add exit edge from result block to result CFG exit block
				}

				if (block throws unhandled exception) {
					add unhandled exception edge from result block to result CFG exit block
				}
			}

		}
*/
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + BetterCFGBuilder2.class.getName() + " <class file>");
			System.exit(1);
		}

		String methodName = SystemProperties.getProperty("cfgbuilder.method");

		JavaClass jclass = new ClassParser(argv[0]).parse();
		ClassGen classGen = new ClassGen(jclass);

		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (method.isAbstract() || method.isNative())
				continue;

			if (methodName != null && !method.getName().equals(methodName))
				continue;

			MethodGen methodGen = new MethodGen(method, jclass.getClassName(), classGen.getConstantPool());

			CFGBuilder cfgBuilder = new BetterCFGBuilder2(methodGen);
			cfgBuilder.build();

			CFG cfg = cfgBuilder.getCFG();

			CFGPrinter cfgPrinter = new CFGPrinter(cfg);
			System.out.println("---------------------------------------------------------------------");
			System.out.println("Method: " + SignatureConverter.convertMethodSignature(methodGen));
			System.out.println("---------------------------------------------------------------------");
			cfgPrinter.print(System.out);
		}
	}

}

// vim:ts=4
