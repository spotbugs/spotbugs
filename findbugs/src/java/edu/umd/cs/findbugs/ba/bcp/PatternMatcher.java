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

package edu.umd.cs.daveho.ba.bcp;

import java.util.*;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ConstantPoolGen;
import edu.umd.cs.daveho.ba.*;

/**
 * Match a ByteCodePattern against the code of a method, represented
 * by a CFG.  Produces some number of ByteCodePatternMatch objects, which
 * indicate how the pattern matched the bytecode instructions in the method.
 *
 * @see ByteCodePattern
 * @author David Hovemeyer
 */
public class PatternMatcher implements DFSEdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("bcp.debug");
	private static final boolean SHOW_WILD = Boolean.getBoolean("bcp.showWild");

	private ByteCodePattern pattern;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private DepthFirstSearch dfs;
	private ValueNumberDataflow vnaDataflow;
	private LinkedList<BasicBlock> workList;
	private IdentityHashMap<BasicBlock, BasicBlock> visitedBlockMap;
	private LinkedList<ByteCodePatternMatch> resultList;

	/**
	 * Constructor.
	 * @param pattern the ByteCodePattern to look for examples of
	 * @param cfg the CFG for the method to search
	 * @param cpg the ConstantPoolGen for the method
	 * @param dfs an initialized DepthFirstSearch object; used to detect loop backedges
	 *   in the CFG
	 * @param vnaDataflow an initialized ValueNumberDataflow object which tracks the
	 *   flow of values in the method
	 */
	public PatternMatcher(ByteCodePattern pattern, CFG cfg, ConstantPoolGen cpg, DepthFirstSearch dfs, ValueNumberDataflow vnaDataflow) {
		this.pattern = pattern;
		this.cfg = cfg;
		this.cpg = cpg;
		this.dfs = dfs;
		this.vnaDataflow = vnaDataflow;
		this.workList = new LinkedList<BasicBlock>();
		this.visitedBlockMap = new IdentityHashMap<BasicBlock, BasicBlock>();
		this.resultList = new LinkedList<ByteCodePatternMatch>();
	}

	/**
	 * Search for examples of the ByteCodePattern.
	 * @return this object
	 * @throws DataflowAnalysisException if the ValueNumberAnalysis did not produce useful
	 *   values for the method
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
	 * @param basicBlock the basic block
	 * @param instructionIterator the instruction iterator positioned just before
	 *  the first instruction to be matched
	 */
	private void attemptMatch(BasicBlock basicBlock, BasicBlock.InstructionIterator instructionIterator)
		throws DataflowAnalysisException {
		work(basicBlock, instructionIterator, pattern.getFirst(), 0, null, null, true);
	}

	/**
	 * Match a sequence of pattern elements, starting at the given one.
	 * The InstructionIterator should generally be positioned just before
	 * the next instruction to be matched.  However, it may be positioned
	 * at the end of a basic block, in which case nothing will happen except
	 * that we will try to continue the match in the non-backedge successors
	 * of the basic block.
	 *
	 * @param basicBlock the basic block
	 * @param instructionIterator an InstructionIterator positioned just before
	 *   the instruction to be matched
	 * @param patternElement the PatternElement to be applied to the next instruction;
	 *   if canFork==true and matchCount &gt;= the element's min count, then we may
	 *   advance to the next element in the pattern
	 * @param matchCount number of instructions the PatternElement has matched so far
	 * @param currentMatch the tail of the chain of PatternElementMatch objects
	 *   representing the successful PatternElement/instruction matches so far
	 * @param bindingSet the BindingSet representing the binding of variable names
	 *   in the pattern to Variables (representing runtime values and fields)
	 * @param canFork true if it is permissible to advance to the next PatternElement
	 *   if we've reached the min count for the current PatternElement, false otherwise
	 */
	private void work(BasicBlock basicBlock, BasicBlock.InstructionIterator instructionIterator,
		PatternElement patternElement, int matchCount,
		PatternElementMatch currentMatch, BindingSet bindingSet,
		boolean canFork)
		throws DataflowAnalysisException {

		// Have we reached the end of the pattern?
		if (patternElement == null) {
			// This is a complete match.
			if (DEBUG) System.out.println("FINISHED A MATCH!");
			resultList.add(new ByteCodePatternMatch(bindingSet, currentMatch));
			return;
		}

		// If we've reached the minimum number of occurrences for this
		// pattern element, we can advance to the next pattern element without trying
		// to match this instruction again.  We make sure that we only advance to
		// the next element once for this matchCount.
		if (canFork && matchCount >= patternElement.minOccur()) {
			work(basicBlock, instructionIterator.duplicate(), patternElement.getNext(), 0, currentMatch, bindingSet, true);
			canFork = false;
		}

		// If we've reached the maximum number of occurrences for this
		// pattern element, then we can't continue.
		if (matchCount >= patternElement.maxOccur())
			return;

		InstructionHandle matchedInstruction = null;
		PatternElement matchedPatternElement = null;

		// Is there another instruction in this basic block?
		if (instructionIterator.hasNext()) {
			InstructionHandle handle = instructionIterator.next();

			// Get the ValueNumberFrames before and after the instruction
			ValueNumberFrame before = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
			BasicBlock.InstructionIterator dupIter = instructionIterator.duplicate();
			ValueNumberFrame after = dupIter.hasNext()
				? vnaDataflow.getFactAtLocation(new Location(dupIter.next(), basicBlock))
				: vnaDataflow.getResultFact(basicBlock);

			// Try to match the instruction against the pattern element.
			boolean isWild = patternElement instanceof Wild;
			if (DEBUG && (!isWild || SHOW_WILD)) System.out.println("Match " + patternElement + " against " + handle + " " +
				(bindingSet != null ? bindingSet.toString() : "[]") + "...");
			MatchResult matchResult = patternElement.match(handle, cpg, before, after, bindingSet);
			if (DEBUG && (!isWild || SHOW_WILD)) System.out.println("\t" + ((matchResult != null) ? " ==> MATCH" : " ==> NOT A MATCH"));
			if (matchResult == null)
				// Not a match.
				return;

			// Successful match!
			matchedInstruction = handle;
			matchedPatternElement = matchResult.getPatternElement();
			++matchCount;
			canFork = true;
			currentMatch = new PatternElementMatch(matchedPatternElement, handle, matchCount, currentMatch);
			bindingSet = matchResult.getBindingSet();
		}

		// Continue the match at each successor instruction,
		// using the same PatternElement.
		if (instructionIterator.hasNext()) {
			// Easy case; continue matching in the same basic block.
			work(basicBlock, instructionIterator, patternElement, matchCount, currentMatch, bindingSet, canFork);
		} else if (matchedPatternElement.allowTrailingEdges()) {
			// We've reached the end of the basic block.
			// Try to advance to the successors of this basic block,
			// ignoring loop backedges.
			Iterator<Edge> i = cfg.outgoingEdgeIterator(basicBlock);
			BitSet visitedSuccessorSet = new BitSet();
			while (i.hasNext()) {
				Edge edge = i.next();
				if (dfs.getDFSEdgeType(edge) == BACK_EDGE)
					continue;


				// If we have just matched an instruction, then we allow the
				// matching PatternElement to choose which edges are acceptable.
				// This allows PatternElements to select particular control edges;
				// for example, only continue the pattern on the true branch
				// of an "if" comparison.
				if (matchedInstruction == null || matchedPatternElement.acceptBranch(edge, matchedInstruction)) {
					BasicBlock destBlock = edge.getDest();
					int destId = destBlock.getId();

					// CFGs can have duplicate edges
					if (visitedSuccessorSet.get(destId))
						continue;
					visitedSuccessorSet.set(destId, true);

					work(destBlock, destBlock.instructionIterator(), patternElement, matchCount, currentMatch, bindingSet, canFork);
				}
			}
		}

	}
}

// vim:ts=4
