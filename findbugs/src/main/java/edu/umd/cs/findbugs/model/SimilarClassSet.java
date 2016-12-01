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

import java.util.LinkedList;
import java.util.List;

/**
 * A set of classes considered similar because of their features.
 *
 * @author David Hovemeyer
 */
public class SimilarClassSet {
    private final List<ClassFeatureSet> memberList;

    public SimilarClassSet() {
        this.memberList = new LinkedList<ClassFeatureSet>();
    }

    public boolean shouldContain(ClassFeatureSet candidate) {
        for (ClassFeatureSet member : memberList) {
            if (candidate.similarTo(member)) {
                return true;
            }
        }
        return false;
    }

    public void addMember(ClassFeatureSet member) {
        memberList.add(member);
    }

    public String getRepresentativeClassName() {
        if (memberList.isEmpty()) {
            throw new IllegalStateException();
        }
        return memberList.get(0).getClassName();
    }

    public int size() {
        return memberList.size();
    }
}
