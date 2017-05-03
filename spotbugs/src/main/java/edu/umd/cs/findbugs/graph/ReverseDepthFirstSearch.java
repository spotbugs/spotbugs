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

package edu.umd.cs.findbugs.graph;

import java.util.Iterator;

/**
 * Perform a reverse depth first search of a graph. (I.e., depth first search of
 * reversed graph.)
 *
 * @author David Hovemeyer
 * @see Graph
 * @see AbstractDepthFirstSearch
 */
public class ReverseDepthFirstSearch<GraphType extends Graph<EdgeType, VertexType>, EdgeType extends GraphEdge<EdgeType, VertexType>, VertexType extends GraphVertex<VertexType>>
extends AbstractDepthFirstSearch<GraphType, EdgeType, VertexType> {

    /**
     * Constructor.
     *
     * @param graph
     *            the graph to perform a reverse depth first search of
     */
    public ReverseDepthFirstSearch(GraphType graph) {
        super(graph);
    }

    @Override
    protected Iterator<EdgeType> outgoingEdgeIterator(GraphType graph, VertexType vertex) {
        return graph.incomingEdgeIterator(vertex);
    }

    @Override
    protected VertexType getTarget(EdgeType edge) {
        return edge.getSource();
    }

    @Override
    protected VertexType getSource(EdgeType edge) {
        return edge.getTarget();
    }

}

