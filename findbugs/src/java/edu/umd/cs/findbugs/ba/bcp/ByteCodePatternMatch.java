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
	private List<PatternElementMatch> patternElementMatchList;
	private BindingSet bindingSet;
	private Iterator<PatternElement> elementIter;
	private PatternElementMatch currentPatternElementMatch;

	public ByteCodePatternMatch(ByteCodePattern byteCodePattern) {
		this.byteCodePattern = byteCodePattern;
		this.patternElementMatchList = new LinkedList<PatternElementMatch>();
		this.bindingSet = null;
		this.elementIter = byteCodePattern.patternElementIterator();
	}

	public boolean hasMoreElements() {
		return elementIter.hasNext();
	}

	public PatternElement nextElement() {
		PatternElement patternElement = elementIter.next();
		currentPatternElementMatch = new PatternElementMatch(patternElement);
		return patternElement;
	}

	public void addMatchedInstruction(InstructionHandle handle) {
		currentPatternElementMatch.addMatchedInstruction(handle);
	}
}

// vim:ts=4
