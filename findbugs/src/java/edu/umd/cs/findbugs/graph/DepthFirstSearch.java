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

// $Revision: 1.10 $

package edu.umd.cs.findbugs.graph;

import java.util.*;

/**
 * Algorithm to perform depth first search on a graph.
 * Based on Cormen, et. al, <cite>Introduction to Algorithms</cite>, p. 478.
 */
public class DepthFirstSearch
        <
        GraphType extends Graph<EdgeType, VertexType>,
        EdgeType extends GraphEdge<EdgeType, VertexType>,
        VertexType extends GraphVertex<VertexType>
        > {

	private static final int WHITE = 0;
	private static final int GRAY = 1;
	private static final int BLACK = 2;

	/**
	 * Colors of the vertices.
	 */
	private int[] m_colorList;

	/**
	 * Start times for the vertices.
	 */
	private int[] m_startTimeList;

	/**
	 * Finish times for the vertices.
	 */
	private int[] m_finishTimeList;

	/**
	 * Predecessors for the vertices.
	 */
	private ArrayList<VertexType> m_predecessorList;

	/**
	 * Depth first search tree forest.
	 */
	private ArrayList<SearchTree<VertexType>> m_searchTreeList;

	/**
	 * Time counter.
	 */
	private int m_time;

	/**
	 * VertexType chooser object; determines which vertices will
	 * be considered by the search.
	 */
	private VertexChooser<VertexType> m_vertexChooser;

	private static class UnconditionalVertexChooser <VertexType extends GraphVertex<VertexType>>
	        implements VertexChooser<VertexType> {
		public UnconditionalVertexChooser() {
		}

		public boolean isChosen(VertexType v) {
			return true;
		}
	}

	/**
	 * Constructor.
	 */
	public DepthFirstSearch() {
		m_vertexChooser = new UnconditionalVertexChooser<VertexType>();
	}

	/**
	 * Specify a VertexChooser object to be used to selected
	 * which vertices are visited by the search.
	 * This is useful if you only want to search a subset
	 * of the vertices in the graph.
	 */
	public void setVertexChooser(VertexChooser<VertexType> vertexChooser) {
		m_vertexChooser = vertexChooser;
	}

	/**
	 * Perform the depth first search.
	 */
	public void search(GraphType g) {
		search(g, g.vertexIterator());
	}

	/**
	 * Perform the depth first search, using given iterator
	 * to specify the order in which the vertices should be searched.
	 */
	public void search(GraphType g, Iterator<VertexType> vertexIter) {

		final int numVertexLabels = g.getNumVertexLabels();

		m_colorList = new int[numVertexLabels]; // colors initially WHITE
		m_startTimeList = new int[numVertexLabels];
		m_finishTimeList = new int[numVertexLabels];
		m_predecessorList = new ArrayList<VertexType>(numVertexLabels);
		for (int i = 0; i < numVertexLabels; ++i) {
			m_predecessorList.add(null);
		}
		m_searchTreeList = new ArrayList<SearchTree<VertexType>>();

		m_time = 0;

		while (vertexIter.hasNext()) {
			VertexType v = (VertexType) vertexIter.next();
			if (m_vertexChooser.isChosen(v) && m_colorList[v.getLabel()] == WHITE) {
				m_searchTreeList.add(visit(g, v));
			}
		}

	}

	/**
	 * Helper function to visit an unvisited vertex.
	 */
	private SearchTree<VertexType> visit(GraphType g, VertexType v) {
		final int vertexNum = v.getLabel();

		SearchTree<VertexType> tree = new SearchTree<VertexType>(v);

		// Record start of visit for this vertex
		m_colorList[vertexNum] = GRAY;
		m_startTimeList[vertexNum] = ++m_time;

		// Visit unvisited adjacent vertices
		Iterator<VertexType> i = g.successorIterator(v);
		while (i.hasNext()) {
			VertexType next = i.next();
			if (m_vertexChooser.isChosen(next) && m_colorList[next.getLabel()] == WHITE) {
				m_predecessorList.set(next.getLabel(), v);
				tree.addChild(visit(g, next));
			}
		}

		// Finish visit for this vertex
		m_colorList[vertexNum] = BLACK;
		m_finishTimeList[vertexNum] = ++m_time;

		return tree;
	}

	/**
	 * Get array of start times for each vertex (indexed by vertex label).
	 */
	public int[] getStartTimeList() {
		return m_startTimeList;
	}

	/**
	 * Get array of finish times for each vertex (indexed by vertex label).
	 */
	public int[] getFinishTimeList() {
		return m_finishTimeList;
	}

//	/** Get array of predecessors for each vertex (indexed by vertex label).  */
//	public VertexType[] getPredecessorList() {
//		return m_predecessorList;
//	}

	/**
	 * Get an iterator over the search trees found by the
	 * depth first search.
	 */
	public Iterator<SearchTree<VertexType>> searchTreeIterator() {
		return m_searchTreeList.iterator();
	}

}

// vim:ts=4
