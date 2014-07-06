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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.generic.InstructionHandle;

public class ByteCodePatternMatch {
    private final BindingSet bindingSet;

    private final PatternElementMatch lastElementMatch;

    private final LinkedList<PatternElementMatch> patternElementMatchList;

    @Override
    public String toString() {
        ArrayList<Integer> lst = new ArrayList<Integer>();
        for (PatternElementMatch m : patternElementMatchList) {
            lst.add(m.getMatchedInstructionInstructionHandle().getPosition());
        }
        return lst.toString();
    }

    public ByteCodePatternMatch(BindingSet bindingSet, PatternElementMatch lastElementMatch) {
        this.bindingSet = bindingSet;
        this.lastElementMatch = lastElementMatch;
        this.patternElementMatchList = new LinkedList<PatternElementMatch>();

        // The PatternElementMatch objects are stored in reverse order.
        // So, put them in a LinkedList to get them in the right order.
        while (lastElementMatch != null) {
            patternElementMatchList.addFirst(lastElementMatch);
            lastElementMatch = lastElementMatch.getPrev();
        }
    }

    public BindingSet getBindingSet() {
        return bindingSet;
    }

    public Iterator<PatternElementMatch> patternElementMatchIterator() {
        return patternElementMatchList.iterator();
    }

    public InstructionHandle getLabeledInstruction(String label) {
        return lastElementMatch != null ? lastElementMatch.getLabeledInstruction(label) : null;
    }

    public PatternElementMatch getFirstLabeledMatch(String label) {
        return lastElementMatch != null ? lastElementMatch.getFirstLabeledMatch(label) : null;
    }

    public PatternElementMatch getLastLabeledMatch(String label) {
        return lastElementMatch != null ? lastElementMatch.getLastLabeledMatch(label) : null;
    }
}

