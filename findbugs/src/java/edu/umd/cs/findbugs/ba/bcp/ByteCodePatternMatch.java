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

public class ByteCodePatternMatch {
	private ByteCodePattern byteCodePattern;
	private LinkedList<PatternElementMatch> patternElementMatchList;
	private BindingSet bindingSet;
	private PatternElement nextElement;
	private PatternElementMatch curMatch;

	private ByteCodePatternMatch() {
	}

	public ByteCodePatternMatch(ByteCodePattern byteCodePattern) {
		this.byteCodePattern = byteCodePattern;
		this.patternElementMatchList = new LinkedList<PatternElementMatch>();
		this.bindingSet = null;
		this.nextElement = byteCodePattern.getFirst();
		this.curMatch = null;
	}

	public boolean hasMoreElements() {
		return nextElement != null;
	}

	public PatternElement nextElement() {
		PatternElement patternElement = nextElement;
		nextElement = patternElement.getNext();
		curMatch = new PatternElementMatch(patternElement);
		patternElementMatchList.add(curMatch);
		return patternElement;
	}

	public void addMatchedInstruction(InstructionHandle handle) {
		curMatch.addMatchedInstruction(handle);
	}

	public ByteCodePatternMatch duplicate() {
		ByteCodePatternMatch dup = new ByteCodePatternMatch();
		dup.byteCodePattern = this.byteCodePattern;

		// Copy only the first n-1 PatternElementMatches, since the last
		// PatternElementMatch is the current one, and we may want to
		// modify it in both this object and the duplicate.
		dup.patternElementMatchList = new LinkedList<PatternElementMatch>();
		int count = patternElementMatchList.size() - 1;
		Iterator<PatternElementMatch> i = patternElementMatchList.iterator();
		while (count-- > 0 && i.hasNext()) {
			dup.patternElementMatchList.add(i.next());
		}

		// TODO: make a duplicate of the last PatternElementMatch,
		// and set it to curMatch

		dup.bindingSet = this.bindingSet;

		return dup;
	}
}

// vim:ts=4
