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

package edu.umd.cs.findbugs.ba;

import java.util.Iterator;

/**
 * Specify an order for visiting basic blocks.
 */
public interface BlockOrder {
    /**
     * Return an Iterator which visits the basic blocks in order.
     */
    public Iterator<BasicBlock> blockIterator();

    /** Return relative order of blocks.
     * If b1.compareTo(b2) < 0, then b1 should occur before b2 in iteration.
     */
    public int compare(BasicBlock b1, BasicBlock b2);
}

