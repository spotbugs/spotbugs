/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Set of streams that are in an equivalence class.
 *
 * @author David Hovemeyer
 */
public class StreamEquivalenceClass {
    private final HashSet<Stream> memberSet;

    private boolean isClosed;

    /**
     * Constructor. Creates an empty set.
     */
    public StreamEquivalenceClass() {
        this.memberSet = new HashSet<Stream>();
        this.isClosed = false;
    }

    /**
     * Add a single member to the equivalence class.
     *
     * @param member
     *            the member Stream
     */
    public void addMember(Stream member) {
        memberSet.add(member);
    }

    /**
     * Get Iterator over the members of the class.
     */
    public Iterator<Stream> memberIterator() {
        return memberSet.iterator();
    }

    /**
     * Add all members of other StreamEquivalenceClass to this one.
     *
     * @param other
     *            the other StreamEquivalenceClass
     */
    public void addAll(StreamEquivalenceClass other) {
        memberSet.addAll(other.memberSet);
    }

    /**
     * Mark all members of the class as being closed.
     */
    public void setClosed() {
        if (!isClosed) {
            isClosed = true;
            for (Stream member : memberSet) {
                member.setClosed();
            }
        }
    }
}

