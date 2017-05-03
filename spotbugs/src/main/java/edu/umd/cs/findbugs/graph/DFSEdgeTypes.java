/*
 * Generic graph library
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

package edu.umd.cs.findbugs.graph;

/**
 * Edge types in a depth first search.
 *
 * @see DepthFirstSearch
 */
public interface DFSEdgeTypes {
    /**
     * Unknown DFS edge type. This is for internal use only.
     */
    public static final int UNKNOWN_EDGE = -1;

    /**
     * Tree edge. Basically, and edge that is part of a depth-first search tree.
     */
    public static final int TREE_EDGE = 0;

    /**
     * Back edge. An edge to an ancestor in the same depth-first search tree.
     */
    public static final int BACK_EDGE = 1;

    /**
     * Forward edge. An edge to a descendant in the same depth-first search
     * tree.
     */
    public static final int FORWARD_EDGE = 2;

    /**
     * Cross edge. Edge between unrelated nodes in the same depth-first search
     * tree, or an edge between nodes in different depth-first search trees.
     */
    public static final int CROSS_EDGE = 3;
}

