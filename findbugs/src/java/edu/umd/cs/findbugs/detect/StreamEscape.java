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

import org.apache.bcel.generic.InstructionHandle;

/**
 * A StreamEscape is an object representing the escape of a Stream.
 * The "source" is the creation point of the stream.  The "target" is
 * the instruction where the stream instance escapes.
 */
public class StreamEscape implements Comparable<StreamEscape> {
	public final InstructionHandle source;
	public final InstructionHandle target;

	public StreamEscape(InstructionHandle source, InstructionHandle target) {
		this.source = source;
		this.target = target;
	}

	public int compareTo(StreamEscape other) {
		int cmp = source.getPosition() - other.source.getPosition();
		if (cmp != 0)
			return cmp;
		return target.getPosition() - other.target.getPosition();
	}

	public String toString() {
		return source.getPosition() + " to " + target.getPosition();
	}
}

// vim:ts=3
