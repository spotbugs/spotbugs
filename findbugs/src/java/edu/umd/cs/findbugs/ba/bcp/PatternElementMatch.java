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

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;

/**
 * PatternElementMatch represents matching a PatternElement against
 * a single instruction.  The "prev" field points to the previous
 * PatternElementMatch.  By building up sequences of PatternElementMatch objects
 * in this way, we can implement nondeterministic matching without
 * having to copy anything.
 */
public class PatternElementMatch {
	private final PatternElement patternElement;
	private final InstructionHandle matchedInstruction;
	private final BasicBlock basicBlock;
	private final int matchCount;
	private final PatternElementMatch prev;

	/**
	 * Constructor.
	 *
	 * @param patternElement     the PatternElement being matched
	 * @param matchedInstruction the instruction which matched the PatternElement
	 * @param basicBlock         the basic block containing the matched instruction
	 * @param matchCount         the index (starting at zero) of the instructions
	 *                           matching the PatternElement; multiple instructions can match the
	 *                           same PatternElement
	 * @param prev               the previous PatternElementMatch
	 */
	public PatternElementMatch(PatternElement patternElement, InstructionHandle matchedInstruction,
							   BasicBlock basicBlock,
							   int matchCount, PatternElementMatch prev) {
		this.patternElement = patternElement;
		this.matchedInstruction = matchedInstruction;
		this.basicBlock = basicBlock;
		this.matchCount = matchCount;
		this.prev = prev;
	}

	/**
	 * Get the PatternElement.
	 */
	public PatternElement getPatternElement() {
		return patternElement;
	}

	/**
	 * Get the matched instruction.
	 */
	public InstructionHandle getMatchedInstructionInstructionHandle() {
		return matchedInstruction;
	}

	/**
	 * Get the basic block containing the matched instruction.
	 */
	public BasicBlock getBasicBlock() {
		return basicBlock;
	}

	/*
	 * Get the index of this instruction in terms of how many instructions
	 * have matched this PatternElement.  (0 for the first instruction to
	 * match the PatternElement, etc.)
	 */
	public int getMatchCount() {
		return matchCount;
	}

	/**
	 * Get the previous PatternMatchElement.
	 */
	public PatternElementMatch getPrev() {
		return prev;
	}

	/**
	 * Get the <em>first</em> instruction matched by the PatternElement with given label.
	 */
	public InstructionHandle getLabeledInstruction(String label) {
		PatternElementMatch first = getFirstLabeledMatch(label);
		return first != null ? first.getMatchedInstructionInstructionHandle() : null;
	}

	/**
	 * Get <em>first</em> match element with given label,
	 * if any.
	 */
	public PatternElementMatch getFirstLabeledMatch(String label) {
		PatternElementMatch cur = this, result = null;
		while (cur != null) {
			String elementLabel = cur.patternElement.getLabel();
			if (elementLabel != null && elementLabel.equals(label))
				result = cur;
			cur = cur.prev;
		}
		return result;
	}

	/**
	 * Get <em>last</em> match element with given label,
	 * if any.
	 */
	public PatternElementMatch getLastLabeledMatch(String label) {
		PatternElementMatch cur = this;
		while (cur != null) {
			String elementLabel = cur.patternElement.getLabel();
			if (elementLabel != null && elementLabel.equals(label))
				return cur;
			cur = cur.prev;
		}
		return null;
	}

	/**
	 * Return whether or not the most recently matched instruction
	 * allows trailing edges.
	 */
	public boolean allowTrailingEdges() {
		return patternElement.allowTrailingEdges();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		PatternElementMatch cur = this;
		buf.append(cur.patternElement.toString());
		buf.append(", ");
		buf.append(cur.matchedInstruction.toString());
		buf.append(", ");
		buf.append(cur.matchCount);
		return buf.toString();
	}

	@Override
		 public int hashCode() {
		// Do the simplest thing possible that works
		throw new UnsupportedOperationException();
	}

	@Override
		 public boolean equals(Object o) {
		if (!(o instanceof PatternElementMatch))
			return false;
		PatternElementMatch lhs = this;
		PatternElementMatch rhs = (PatternElementMatch) o;

		while (lhs != null && rhs != null) {
			if (lhs.patternElement != rhs.patternElement ||
					lhs.matchedInstruction != rhs.matchedInstruction ||
					lhs.matchCount != rhs.matchCount)
				return false;

			lhs = lhs.prev;
			rhs = rhs.prev;
		}

		return lhs == rhs;
	}
}

// vim:ts=4
