/*
 * Generic graph library
 * Copyright (C) 2000,2003,2004 University of Maryland
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

// $Revision: 1.6 $

package edu.umd.cs.findbugs.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * SearchTree represents a search tree produced by a graph search algorithm,
 * such as BreadthFirstSearch or DepthFirstSearch.
 */
public class SearchTree<VertexType extends GraphVertex<VertexType>> {

    private final VertexType m_vertex;

    private final ArrayList<SearchTree<VertexType>> m_childList;

    /**
     * Create a new search tree.
     */
    public SearchTree(VertexType v) {
        m_vertex = v;
        m_childList = new ArrayList<SearchTree<VertexType>>();
    }

    /**
     * Get the vertex contained in this node.
     */
    public VertexType getVertex() {
        return m_vertex;
    }

    /**
     * Add a child search tree.
     */
    public void addChild(SearchTree<VertexType> child) {
        m_childList.add(child);
    }

    /**
     * Return collection of children of this search tree. (Elements returned are
     * also SearchTree objects).
     */
    public Iterator<SearchTree<VertexType>> childIterator() {
        return m_childList.iterator();
    }

    /**
     * Add all vertices contained in this search tree to the given set.
     */
    public void addVerticesToSet(Set<VertexType> set) {
        // Add the vertex for this object
        set.add(this.m_vertex);

        // Add vertices for all children
        Iterator<SearchTree<VertexType>> i = childIterator();
        while (i.hasNext()) {
            SearchTree<VertexType> child = i.next();
            child.addVerticesToSet(set);
        }
    }

}

