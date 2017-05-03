/*
 * Generic graph library
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

package edu.umd.cs.findbugs.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A search tree callback implementation that builds a list of SearchTrees
 * recording a graph search.
 *
 * @see SearchTreeCallback
 * @author David Hovemeyer
 */
public class SearchTreeBuilder<VertexType extends GraphVertex<VertexType>> implements SearchTreeCallback<VertexType> {

    private final HashMap<VertexType, SearchTree<VertexType>> searchTreeMap = new HashMap<VertexType, SearchTree<VertexType>>();

    private final LinkedList<SearchTree<VertexType>> searchTreeList = new LinkedList<SearchTree<VertexType>>();

    @Override
    public void startSearchTree(VertexType vertex) {
        searchTreeList.add(createSearchTree(vertex));
    }

    @Override
    public void addToSearchTree(VertexType parent, VertexType child) {
        SearchTree<VertexType> parentTree = searchTreeMap.get(parent);
        if (parentTree == null) {
            throw new IllegalStateException();
        }
        SearchTree<VertexType> childTree = createSearchTree(child);
        parentTree.addChild(childTree);
    }

    /**
     * Get an Iterator over the recorded SearchTrees.
     */
    public Iterator<SearchTree<VertexType>> searchTreeIterator() {
        return searchTreeList.iterator();
    }

    private SearchTree<VertexType> createSearchTree(VertexType vertex) {
        SearchTree<VertexType> searchTree = new SearchTree<VertexType>(vertex);
        searchTreeMap.put(vertex, searchTree);
        return searchTree;
    }
}

