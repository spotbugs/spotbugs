package edu.umd.cs.daveho.ba;

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * A CFGBuilder that really tries to construct accurate control flow graphs.
 * The CFGs is creates have accurate exception edges, and have accurately
 * inlined JSR subroutines.
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class BetterCFGBuilder2 implements CFGBuilder, EdgeTypes {

	// TODO: don't forget to change BasicBlock so ATHROW is considered to have a null check

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

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

	private class Context {
		private final InstructionHandle start;
		private final BitSet instructionMap;
		private final CFG cfg;
		private IdentityHashMap<InstructionHandle, BasicBlock> blockMap;
		private IdentityHashMap<BasicBlock, List<EscapeTarget>> escapeTargetListMap;
		private LinkedList<WorkListItem> workList;

		public Context(InstructionHandle start) {
			this.start = start;
			this.instructionMap = new BitSet();
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

		public BasicBlock getBlock(InstructionHandle start) {
			BasicBlock block = blockMap.get(start);
			if (block == null) {
				block = allocateBasicBlock();
				blockMap.put(start, block);
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
		Context topLevelContext = new Context(methodGen.getInstructionList().getStart());
		contextWorkList.add(topLevelContext);

		while (!contextWorkList.isEmpty()) {
			Context context = contextWorkList.removeFirst();
			build(context);
		}

		// TODO: inline everything into the top level context
	}

	public CFG getCFG() {
		return null;
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private void build(Context context) {
		// Prime the work list
		context.addEdgeAndExplore(context.getEntry(), context.getStartInstruction(), START_EDGE);

		while (context.hasMoreWork()) {
			WorkListItem item = context.nextItem();

			InstructionHandle handle = item.getStartInstruction();
			Instruction ins = handle.getInstruction();
			BasicBlock basicBlock = item.getBasicBlock();

			if (usedInstructionSet.get(handle.getPosition()))
				throw new IllegalStateException("Visiting instruction " + handle + " again");
			usedInstructionSet.set(handle.getPosition());

			// Add exception handler block (ETB) for exception-throwing instructions
			if (isPEI(handle)) {
				handleExceptions(handle, basicBlock);
				BasicBlock body = context.allocateBasicBlock();
				context.addEdge(basicBlock, body, FALL_THROUGH_EDGE);
				basicBlock = body;
			}

			boolean endOfBasicBlock = false;
			do {

				// Add the instruction to the block
				basicBlock.addInstruction(handle);

				short opcode = ins.getOpcode();

				// TODO: should check instruction to ensure that in a JSR subroutine
				// no assignments are made to the local containing the return address.
				// if (ins instanceof ASTORE) ...

				if (opcode == Constants.JSR || opcode == Constants.JSR_W) {
					// TODO: find JSR subroutine, add it to context work list if
					// we haven't built a CFG for it yet

					// This ends the basic block.
					// Add a JSR_EDGE to the successor.  When we eventually
					// inline the JSR subroutine, this edge will be removed.
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
							handleExceptions(handle, basicBlock);
						} else {
							Iterator<Target> i = visitor.targetIterator();
							while (i.hasNext()) {
								Target target = i.next();
								context.addEdgeAndExplore(basicBlock, target.getTargetInstruction(), target.getEdgeType());
							}
						}
					}
				}

				if (!endOfBasicBlock) {

					InstructionHandle next = handle.getNext();
					if (next != null) {
						// Is the next instruction a control merge or a PEI?
						if (isMerge(next) || isPEI(next)) {
							context.addEdgeAndExplore(basicBlock, next, FALL_THROUGH_EDGE);
							endOfBasicBlock = true;
						} else {
							// Basic block continues
							handle = next;
						}
					}
				}
				
			} while (!endOfBasicBlock);
		}
	}

	private void handleExceptions(InstructionHandle pei, BasicBlock etb) {
		// TODO: implement
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

}

// vim:ts=4
