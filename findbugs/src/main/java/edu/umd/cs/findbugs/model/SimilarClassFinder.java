/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author David Hovemeyer
 */
public class SimilarClassFinder {
    private final List<SimilarClassSet> similarClassSetList;

    public SimilarClassFinder() {
        this.similarClassSetList = new LinkedList<SimilarClassSet>();
    }

    public void add(ClassFeatureSet classFeatureSet) {
        for (SimilarClassSet similarClassSet : similarClassSetList) {
            if (similarClassSet.shouldContain(classFeatureSet)) {
                similarClassSet.addMember(classFeatureSet);
                return;
            }
        }

        SimilarClassSet newSimilarClassSet = new SimilarClassSet();
        newSimilarClassSet.addMember(classFeatureSet);
        similarClassSetList.add(newSimilarClassSet);
    }

    public int size() {
        return similarClassSetList.size();
    }

    public Iterator<SimilarClassSet> similarClassSetIterator() {
        return similarClassSetList.iterator();
    }
}
