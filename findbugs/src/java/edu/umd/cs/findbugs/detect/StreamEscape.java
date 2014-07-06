/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.Location;

/**
 * A StreamEscape is an object representing the escape of a Stream to a called
 * method. The "source" is the Stream which is escaping. The "target" is the
 * Location where the stream instance escapes.
 */
public class StreamEscape implements Comparable<StreamEscape> {
    public final Stream source;

    public final Location target;

    /**
     * Constructor.
     *
     * @param source
     *            Location where stream is opened
     * @param target
     *            Location where stream escapes by being passed to a method
     */
    public StreamEscape(Stream source, Location target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public int compareTo(StreamEscape other) {
        int cmp = source.compareTo(other.source);
        if (cmp != 0) {
            return cmp;
        }
        return target.compareTo(other.target);
    }

    @Override
    public int hashCode() {
        return source.hashCode() + 7 * target.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StreamEscape)) {
            return false;
        }
        StreamEscape other = (StreamEscape) o;
        return source.equals(other.source) && target.equals(other.target);
    }

    @Override
    public String toString() {
        return source + " to " + target;
    }
}

