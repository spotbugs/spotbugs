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
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A CFGBuilder that really tries to construct accurate control flow graphs.
 * The CFGs it creates have accurate exception edges, and have accurately
 * inlined JSR subroutines.  This is the fourth version of CFGBuilder I've
 * written!
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class BetterCFGBuilder2 implements CFGBuilder, EdgeTypes {

	// TODO: don't forget to change BasicBlock so ATHROW is considered to have a null check

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * A work list item for creating the CFG for a context.
	 */
	private static class WorkListItem {
		private final InstructionHandle start;
		private final BasicBlock basicBlock;

		public WorkListItem(InstructionHandle start, BasicBlock basicBlock) {
			this.start = start;
			this.basicBlock = basicBlock;
		}

		public InstructionHandle	getStartInstruction()	{ return start; }
		public BasicBlock			getBasicBlock()			{ return basicBlock; }
	}

	/**
	 * A placeholder for a control edge that escapes its context to return
	 * control back to an outer (calling) context.  It will turn into a
	 * real edge during inlining.
	 */
	private static class EscapeTarget {
		private final InstructionHandle target;
		private final int edgeType;

		public EscapeTarget(InstructionHandle target, int edgeType) {
			this.target = target;
			this.edgeType = edgeType;
		}

		public InstructionHandle	getTarget()				{ return target; }
		public int					getEdgeType()			{ return edgeType; }
	}

	/**
	 * A Context for inlining JSR subroutines.  The top level
	 * context is where execution starts.  Each JSR subroutine is
	 * its own context.  Each context has its own CFG.  Eventually,
	 * all JSR subroutines will be inlined into the top level context,
	 * resulting in an accurate CFG for the overall method which is
	 * free from JSR subroutines.
	 */
	private class Context {
		private final InstructionHandle start;
		private final BitSet instructionSet;
		private final CFG cfg;
		private IdentityHashMap<InstructionHandle, BasicBlock> blockMap;
		private IdentityHashMap<BasicBlock, List<EscapeTarget>> escapeTargetListMap;
		private LinkedList<WorkListItem> workList;

		public Context(InstructionHandle start) {
			this.start = start;
			this.instructionSet = new BitSet();
			this.cfg = new CFG();
			this.blockMap = new IdentityHashMap<InstructionHandle, BasicBlock>();
			this.escapeTargetListMap = new IdentityHashMap<BasicBlock, List<EscapeTarget>>();
			this.workList = new LinkedList<WorkListItem>();
		}

		public InstructionHandle	getStartInstruction()	{ return start; }
		public BasicBlock			allocateBasicBlock()	{ return cfg.allocate(); }
		public void					addItem(WorkListItem item){ workList.add(item); }
		public boolean				hasMoreWork()			{ return !workList.isEmpty(); }
		public WorkListItem			nextItem()				{ return workList.removeFirst(); }
		public BasicBlock			getEntry()				{ return cfg.getEntry(); }
		public BasicBlock			getExit()				{ return cfg.getExit(); }
		public CFG					getCFG()				{ return cfg; }

		public void addInstruction(InstructionHandle handle) throws CFGBuilderException {
			int position = handle.getPosition();
			if (usedInstructionSet.get(position))
				throw new CFGBuilderException("Instruction " + handle + " visited in multiple contexts");
			instructionSet.set(position);
			usedInstructionSet.set(position);
		}

		public BasicBlock getBlock(InstructionHandle start) {
			BasicBlock block = blockMap.get(start);
			if (block == null) {
				block = allocateBasicBlock();
				blockMap.put(start, block);
				addItem(new WorkListItem(start, block));
			}
			return block;
		}

		public void addEdgeAndExplore(BasicBlock sourceBlock, InstructionHandle target, int edgeType) {
			if (usedInstructionSet.get(target.getPosition())) {
				// Control escapes this context
				List<EscapeTarget> escapeTargetList = escapeTargetListMap.get(sourceBlock);
				if (escapeTargetList == null) {
					escapeTargetList = new LinkedList<EscapeTarget>();
					escapeTargetListMap.put(sourceBlock, escapeTargetList);
				}
				escapeTargetList.add(new EscapeTarget(target, edgeType));
			} else {
				// Edge within the current context
				BasicBlock targetBlock = getBlock(target);

				// Add only if no edge with same source and target already exists.
				// (Switches can create multiple edges from source to target.)
				if (cfg.lookupEdge(sourceBlock, targetBlock) == null)
					addEdge(sourceBlock, targetBlock, edgeType);
			}
		}

		public void addEdge(BasicBlock sourceBlock, BasicBlock destBlock, int edgeType) {
			cfg.addEdge(sourceBlock, destBlock, edgeType);
		}
	}

	/* ----------------------------------------------------------------------
	 * Instance data
	 * ---------------------------------------------------------------------- */

	private MethodGen methodGen;
	private ConstantPoolGen cpg;
	private ExceptionHandlerMap exceptionHandlerMap;
	private BitSet usedInstructionSet;
	private LinkedList<Context> contextWorkList;
	private Context topLevelContext;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	public BetterCFGBuilder2(MethodGen methodGen) {
		this.methodGen = methodGen;
		this.cpg = methodGen.getConstantPool();
		this.exceptionHandlerMap = new ExceptionHandlerMap(methodGen);
		this.usedInstructionSet = new BitSet();
		this.contextWorkList = new LinkedList<Context>();
	}

	public void build() throws CFGBuilderException {
		topLevelContext = new Context(methodGen.getInstructionList().getStart());
		contextWorkList.add(topLevelContext);

		while (!contextWorkList.isEmpty()) {
			Context context = contextWorkList.removeFirst();
			build(context);
		}

		// TODO: inline everything into the top level context
	}

	public CFG getCFG() {
		// FIXME
		return topLevelContext.getCFG();
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private void build(Context context) throws CFGBuilderException {
		// Prime the work list
		context.addEdgeAndExplore(context.getEntry(), context.getStartInstruction(), START_EDGE);

		// Keep going until all basic blocks in the context have been added
		while (context.hasMoreWork()) {
			WorkListItem item = context.nextItem();

			InstructionHandle handle = item.getStartInstruction();
			Instruction ins = handle.getInstruction();
			BasicBlock basicBlock = item.getBasicBlock();

			// Add exception handler block (ETB) for exception-throwing instructions
			if (isPEI(handle)) {
				handleExceptions(context, handle, basicBlock);
				basicBlock.setExceptionThrower(handle);
				BasicBlock body = context.allocateBasicBlock();
				context.addEdge(basicBlock, body, FALL_THROUGH_EDGE);
				basicBlock = body;
			}

			// Add instructions until we get to the end of the block
			boolean endOfBasicBlock = false;
			do {
				// Add the instruction to the block
				basicBlock.addInstruction(handle);
				context.addInstruction(handle);

				short opcode = ins.getOpcode();

				// TODO: should check instruction to ensure that in a JSR subroutine
				// no assignments are made to the local containing the return address.
				// if (ins instanceof ASTORE) ...

				if (opcode == Constants.JSR || opcode == Constants.JSR_W) {
					// TODO: find JSR subroutine, add it to context work list if
					// we haven't built a CFG for it yet

					// This ends the basic block.
					// Add a JSR_EDGE to the successor.
					// It will be replaced later by the inlined JSR subroutine.
					context.addEdgeAndExplore(basicBlock, handle.getNext(), JSR_EDGE);
					endOfBasicBlock = true;
				} else if (opcode == Constants.RET) {
					// End of JSR subroutine
					context.addEdge(basicBlock, context.getExit(), RET_EDGE);
					endOfBasicBlock = true;
				} else {
					TargetEnumeratingVisitor visitor = new TargetEnumeratingVisitor(handle, cpg);
					if (visitor.isEndOfBasicBlock()) {
						endOfBasicBlock = true;

						// Add control edges as appropriate
						if (visitor.instructionIsThrow()) {
							handleExceptions(context, handle, basicBlock);
						} else {
							Iterator<Target> i = visitor.targetIterator();
							while (i.hasNext()) {
								Target target = i.next();
								context.addEdgeAndExplore(basicBlock, target.getTargetInstruction(),
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
						context.addEdgeAndExplore(basicBlock, next, FALL_THROUGH_EDGE);
						endOfBasicBlock = true;
					} else {
						// Basic block continues
						handle = next;
					}
				}
				
			} while (!endOfBasicBlock);
		}
	}

	private void handleExceptions(Context context, InstructionHandle pei, BasicBlock etb) {
		List<CodeExceptionGen> exceptionHandlerList = exceptionHandlerMap.getHandlerList(pei);
		if (exceptionHandlerList == null)
			return;

		// TODO: should try to prune some obviously infeasible exception edges
		Iterator<CodeExceptionGen> i = exceptionHandlerList.iterator();
		while (i.hasNext()) {
			CodeExceptionGen exceptionHandler = i.next();
			InstructionHandle handlerStart = exceptionHandler.getHandlerPC();
			context.addEdgeAndExplore(etb, handlerStart, HANDLED_EXCEPTION_EDGE);
		}
	}

	private boolean isPEI(InstructionHandle handle) {
		Instruction ins = handle.getInstruction();

		if (!(ins instanceof ExceptionThrower))
			return false;

		// Return instructions can throw exceptions only if the method is synchronized
		if (ins instanceof ReturnInstruction && !methodGen.isSynchronized())
			return false;

		return true;
	}

	private static boolean isMerge(InstructionHandle handle) {
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

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + BetterCFGBuilder2.class.getName() + " <class file>");
			System.exit(1);
		}

		String methodName = System.getProperty("cfgbuilder.method");

		JavaClass jclass = new ClassParser(argv[0]).parse();
		ClassGen classGen = new ClassGen(jclass);

		Method[] methodList = jclass.getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];

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
