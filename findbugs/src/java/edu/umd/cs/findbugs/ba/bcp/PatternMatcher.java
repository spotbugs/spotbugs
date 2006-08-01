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

package edu.umd.cs.findbugs.ba.bcp;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DFSEdgeTypes;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Match a ByteCodePattern against the code of a method, represented
 * by a CFG.  Produces some number of ByteCodePatternMatch objects, which
 * indicate how the pattern matched the bytecode instructions in the method.
 * <p/>
 * <p> This code is a hack and should probably be rewritten.
 *
 * @author David Hovemeyer
 * @see ByteCodePattern
 */
public class PatternMatcher implements DFSEdgeTypes {
	private static final boolean DEBUG = SystemProperties.getBoolean("bcp.debug");
	private static final boolean SHOW_WILD = SystemProperties.getBoolean("bcp.showWild");

	private ByteCodePattern pattern;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private DepthFirstSearch dfs;
	private ValueNumberDataflow vnaDataflow;
	private DominatorsAnalysis domAnalysis;
	private LinkedList<BasicBlock> workList;
	private IdentityHashMap<BasicBlock, BasicBlock> visitedBlockMap;
	private LinkedList<ByteCodePatternMatch> resultList;

	/**
	 * Constructor.
	 *
	 * @param pattern      the ByteCodePattern to look for examples of
	 * @param classContext ClassContext for the class to analyze
	 * @param method       the Method to analyze
	 */
	public PatternMatcher(ByteCodePattern pattern, ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		this.pattern = pattern;
		this.cfg = classContext.getCFG(method);
		this.cpg = classContext.getConstantPoolGen();
		this.dfs = classContext.getDepthFirstSearch(method);
		this.vnaDataflow = classContext.getValueNumberDataflow(method);
		this.domAnalysis = classContext.getNonExceptionDominatorsAnalysis(method);
		this.workList = new LinkedList<BasicBlock>();
		this.visitedBlockMap = new IdentityHashMap<BasicBlock, BasicBlock>();
		this.resultList = new LinkedList<ByteCodePatternMatch>();
	}

	/**
	 * Search for examples of the ByteCodePattern.
	 *
	 * @return this object
	 * @throws DataflowAnalysisException if the ValueNumberAnalysis did not produce useful
	 *                                   values for the method
	 */
	public PatternMatcher execute() throws DataflowAnalysisException {
		workList.addLast(cfg.getEntry());

		while (!workList.isEmpty()) {
			BasicBlock basicBlock = workList.removeLast();
			visitedBlockMap.put(basicBlock, basicBlock);

			// Scan instructions of basic block for possible matches
			BasicBlock.InstructionIterator i = basicBlock.instructionIterator();
			while (i.hasNext()) {
				attemptMatch(basicBlock, i.duplicate());
				i.next();
			}

			// Add successors of the basic block (which haven't been visited already)
			Iterator<BasicBlock> succIterator = cfg.successorIterator(basicBlock);
			while (succIterator.hasNext()) {
				BasicBlock succ = succIterator.next();
				if (visitedBlockMap.get(succ) == null)
					workList.addLast(succ);
			}
		}

		return this;
	}

	/**
	 * Return an Iterator over the ByteCodePatternMatch objects representing
	 * successful matches of the ByteCodePattern.
	 */
	public Iterator<ByteCodePatternMatch> byteCodePatternMatchIterator() {
		return resultList.iterator();
	}

	/**
	 * Attempt to begin a match.
	 *
	 * @param basicBlock          the basic block
	 * @param instructionIterator the instruction iterator positioned just before
	 *                            the first instruction to be matched
	 */
	private void attemptMatch(BasicBlock basicBlock, BasicBlock.InstructionIterator instructionIterator)
	        throws DataflowAnalysisException {
		work(new State(basicBlock, instructionIterator, pattern.getFirst()));
	}

	/**
	 * Object representing the current state of the
	 * matching algorithm.  Provides convenient methods to
	 * implement the various steps of the algorithm.
	 */
	private class State {
		private BasicBlock basicBlock;
		private BasicBlock.InstructionIterator instructionIterator;
		private PatternElement patternElement;
		private int matchCount;
		private PatternElementMatch currentMatch;
		private BindingSet bindingSet;
		private boolean canFork;

		/**
		 * Constructor.
		 * Builds the start state.
		 *
		 * @param basicBlock          the initial basic block
		 * @param instructionIterator the instructionIterator indicating where
		 *                            to start matching
		 * @param patternElement      the first PatternElement of the pattern
		 */
		public State(BasicBlock basicBlock, BasicBlock.InstructionIterator instructionIterator,
		             PatternElement patternElement) {
			this(basicBlock, instructionIterator, patternElement, 0, null, null, true);
		}

		/**
		 * Constructor.
		 */
		public State(BasicBlock basicBlock, BasicBlock.InstructionIterator instructionIterator,
		             PatternElement patternElement, int matchCount, @Nullable PatternElementMatch currentMatch,
		             @Nullable BindingSet bindingSet, boolean canFork) {
			this.basicBlock = basicBlock;
			this.instructionIterator = instructionIterator;
			this.patternElement = patternElement;
			this.matchCount = matchCount;
			this.currentMatch = currentMatch;
			this.bindingSet = bindingSet;
			this.canFork = canFork;
		}

		/**
		 * Make an exact copy of this object.
		 */
		public State duplicate() {
			return new State(basicBlock, instructionIterator, patternElement, matchCount, currentMatch, bindingSet, canFork);
		}

		/**
		 * Get basic block.
		 */
		public BasicBlock getBasicBlock() {
			return basicBlock;
		}

		/**
		 * Get current pattern element.
		 */
		public PatternElement getPatternElement() {
			return patternElement;
		}

		/**
		 * Get current pattern element match.
		 */
		public PatternElementMatch getCurrentMatch() {
			return currentMatch;
		}

		/**
		 * Determine if the match is complete.
		 */
		public boolean isComplete() {
			// We're done when we reach the end of the chain
			// of pattern elements.
			return patternElement == null;
		}

		/**
		 * Get a ByteCodePatternMatch representing the complete match.
		 */
		public ByteCodePatternMatch getResult() {
			if (!isComplete()) throw new IllegalStateException("match not complete!");
			return new ByteCodePatternMatch(bindingSet, currentMatch);
		}

		/**
		 * Try to produce a new state that will finish matching
		 * the current element and start matching the next element.
		 * Returns null if the current element is not complete.
		 */
		public State advanceToNextElement() {
			if (!canFork || matchCount < patternElement.minOccur())
			// Current element is not complete, or we already
			// forked at this point
				return null;

			// Create state to advance to matching next pattern element
			// at current basic block and instruction.
			State advance = new State(basicBlock, instructionIterator.duplicate(), patternElement.getNext(),
			        0, currentMatch, bindingSet, true);

			// Now that this state has forked from this element
			// at this instruction, it must not do so again.
			this.canFork = false;

			return advance;
		}

		/**
		 * Determine if the current pattern element can continue
		 * to match instructions.
		 */
		public boolean currentElementCanContinue() {
			return matchCount < patternElement.maxOccur();
		}

		/**
		 * Determine if there are more instructions in the same basic block.
		 */
		public boolean moreInstructionsInBasicBlock() {
			return instructionIterator.hasNext();
		}

		/**
		 * Match current pattern element with next instruction
		 * in basic block.  Returns MatchResult if match succeeds,
		 * null otherwise.
		 */
		public MatchResult matchNextInBasicBlock() throws DataflowAnalysisException {
			if (!moreInstructionsInBasicBlock()) throw new IllegalStateException("At end of BB!");

			// Move to location of next instruction to be matched
			Location location = new Location(instructionIterator.next(), basicBlock);
			return matchLocation(location);
		}

		/**
		 * Determine if it is possible to continue matching
		 * in a successor basic block.
		 */
		public boolean canAdvanceToNextBasicBlock() {
			return currentMatch == null || currentMatch.allowTrailingEdges();
		}

		/**
		 * Get most recently matched instruction.
		 */
		public InstructionHandle getLastMatchedInstruction() {
			if (currentMatch == null) throw new IllegalStateException("no current match!");
			return currentMatch.getMatchedInstructionInstructionHandle();
		}

		/**
		 * Return a new State for continuing the overall pattern match
		 * in a successor basic block.
		 *
		 * @param edge        the Edge leading to the successor basic block
		 * @param matchResult a MatchResult representing the match of the
		 *                    last instruction in the predecessor block; null if none
		 */
		public State advanceToSuccessor(Edge edge, MatchResult matchResult) {
			// If we have just matched an instruction, then we allow the
			// matching PatternElement to choose which edges are acceptable.
			// This allows PatternElements to select particular control edges;
			// for example, only continue the pattern on the true branch
			// of an "if" comparison.
			if (matchResult != null &&
			        !matchResult.getPatternElement().acceptBranch(edge, getLastMatchedInstruction()))
				return null;

			return new State(edge.getTarget(), edge.getTarget().instructionIterator(),
			        patternElement, matchCount, currentMatch, bindingSet, canFork);
		}

		/**
		 * Determine if we need to look for a dominated instruction at
		 * this point in the search.
		 */
		public boolean lookForDominatedInstruction() {
			return patternElement.getDominatedBy() != null && matchCount == 0;
		}

		/**
		 * Return Iterator over states representing dominated instructions
		 * that continue the match.
		 */
		public Iterator<State> dominatedInstructionStateIterator() throws DataflowAnalysisException {
			if (!lookForDominatedInstruction()) throw new IllegalStateException();
			LinkedList<State> stateList = new LinkedList<State>();

			State dup = this.duplicate();

			if (currentMatch != null) {
				// Find the referenced instruction.
				PatternElementMatch dominator = currentMatch.getFirstLabeledMatch(patternElement.getDominatedBy());
				BasicBlock domBlock = dominator.getBasicBlock();

				// Find all basic blocks dominated by the dominator block.
				for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
					BasicBlock block = i.next();
					if (block == domBlock)
						continue;

					BitSet dominators = domAnalysis.getResultFact(block);
					if (dominators.get(domBlock.getId())) {
						// This block is dominated by the dominator block.
						// Each instruction in the block which matches the current pattern
						// element is a new state continuing the match.
						for (Iterator<InstructionHandle> j = block.instructionIterator(); j.hasNext();) {
							MatchResult matchResult = dup.matchLocation(new Location(j.next(), block));
							if (matchResult != null) {
								stateList.add(dup);
								dup = this.duplicate();
							}
						}
					}
				}
			}

			return stateList.iterator();
		}

		private MatchResult matchLocation(Location location) throws DataflowAnalysisException {
			// Get the ValueNumberFrames before and after the instruction
			ValueNumberFrame before = vnaDataflow.getFactAtLocation(location);
			ValueNumberFrame after = vnaDataflow.getFactAfterLocation(location);

			// Try to match the instruction against the pattern element.
			boolean debug = DEBUG && (!(patternElement instanceof Wild) || SHOW_WILD);
			if (debug)
				System.out.println("Match " + patternElement +
				        " against " + location.getHandle() + " " +
				        (bindingSet != null ? bindingSet.toString() : "[]") + "...");
			MatchResult matchResult = patternElement.match(location.getHandle(),
			        cpg, before, after, bindingSet);
			if (debug)
				System.out.println("\t" +
				        ((matchResult != null) ? " ==> MATCH" : " ==> NOT A MATCH"));
			if (matchResult != null) {
				// Successful match!
				// Update state to reflect that the match has occurred.
				++matchCount;
				canFork = true;
				currentMatch = new PatternElementMatch(matchResult.getPatternElement(),
				        location.getHandle(), location.getBasicBlock(), matchCount, currentMatch);
				bindingSet = matchResult.getBindingSet();
			}
			return matchResult;
		}
	}

	/**
	 * Match a sequence of pattern elements, starting at the given one.
	 * The InstructionIterator should generally be positioned just before
	 * the next instruction to be matched.  However, it may be positioned
	 * at the end of a basic block, in which case nothing will happen except
	 * that we will try to continue the match in the non-backedge successors
	 * of the basic block.
	 */
	private void work(State state) throws DataflowAnalysisException {
		// Have we reached the end of the pattern?
		if (state.isComplete()) {
			// This is a complete match.
			if (DEBUG) System.out.println("FINISHED A MATCH!");
			resultList.add(state.getResult());
			return;
		}

		// If we've reached the minimum number of occurrences for this
		// pattern element, we can advance to the next pattern element without trying
		// to match this instruction again.  We make sure that we only advance to
		// the next element once for this matchCount.
		State advance = state.advanceToNextElement();
		if (advance != null) {
			work(advance);
		}

		// If we've reached the maximum number of occurrences for this
		// pattern element, then we can't continue.
		if (!state.currentElementCanContinue())
			return;

		MatchResult matchResult = null;

		// Are we looking for an instruction dominated by an earlier
		// matched instruction?
		if (state.lookForDominatedInstruction()) {
			for (Iterator<State> i = state.dominatedInstructionStateIterator(); i.hasNext();)
				work(i.next());
			return;
		}

		// Is there another instruction in this basic block?
		if (state.moreInstructionsInBasicBlock()) {
			// Try to match it.
			matchResult = state.matchNextInBasicBlock();
			if (matchResult == null)
				return;
		}

		// Continue the match at each successor instruction,
		// using the same PatternElement.
		if (state.moreInstructionsInBasicBlock()) {
			// Easy case; continue matching in the same basic block.
			work(state);
		} else if (state.canAdvanceToNextBasicBlock()) {
			// We've reached the end of the basic block.
			// Try to advance to the successors of this basic block,
			// ignoring loop backedges.
			Iterator<Edge> i = cfg.outgoingEdgeIterator(state.getBasicBlock());
			BitSet visitedSuccessorSet = new BitSet();
			while (i.hasNext()) {
				Edge edge = i.next();
				if (dfs.getDFSEdgeType(edge) == BACK_EDGE)
					continue;

				BasicBlock destBlock = edge.getTarget();
				int destId = destBlock.getId();

				// CFGs can have duplicate edges
				if (visitedSuccessorSet.get(destId))
					continue;
				visitedSuccessorSet.set(destId, true);

				// See if we can continue matching in the successor basic block.
				State succState = state.advanceToSuccessor(edge, matchResult);
				if (succState != null) {
					work(succState);
				}
			}
		}

	}
}

// vim:ts=4
