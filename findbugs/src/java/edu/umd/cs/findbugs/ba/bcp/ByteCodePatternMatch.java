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

public class ByteCodePatternMatch {
	private BindingSet bindingSet;
	private LinkedList<PatternElementMatch> patternElementMatchList;

	public ByteCodePatternMatch(BindingSet bindingSet, PatternElementMatch lastElementMatch) {
		this.bindingSet = bindingSet;
		this.patternElementMatchList = new LinkedList<PatternElementMatch>();

		// The PatternElementMatch objects are stored in reverse order.
		// So, put them in a LinkedList to get them in the right order.
		while (lastElementMatch != null) {
			patternElementMatchList.addLast(lastElementMatch);
			lastElementMatch = lastElementMatch.getPrev();
		}
	}

	public BindingSet getBindingSet() {
		return bindingSet;
	}

	public Iterator<PatternElementMatch> patternElementMatchIterator() {
		return patternElementMatchList.iterator();
	}
}

// vim:ts=4
