/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;

import org.apache.bcel.Constants;

/**
 * Class representing the set of opcodes used in a method.
 *
 * @author David Hovemeyer
 */
public class MethodBytecodeSet extends BitSet {
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("[");
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            buf.append(Constants.OPCODE_NAMES[i]).append(", ");
        }
        buf.setLength(buf.length() - 2);
        buf.append("]");
        return buf.toString();
    }

}
