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

// $Revision: 1.7 $

package edu.umd.cs.findbugs.graph;

import java.io.Serializable;
import java.util.Comparator;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Comparator to compare GraphVertex objects by their visitation times in a
 * search; for example, it could compare the finishing times produced
 * by DepthFirstSearch.
 */
public class VisitationTimeComparator <VertexType extends GraphVertex<VertexType>> implements Comparator<VertexType>,
Serializable {

	/**
	 * Compare in ascending order.
	 */
	public static final int ASCENDING = 0;

	/**
	 * Compare in descending order.
	 */
	public static final int DESCENDING = 1;

	private int[] m_visitationTimeList;
	private int m_direction;

	/**
	 * Constructor.
	 *
	 * @param visitationTimeList array of visitation times indexed by vertex label
	 * @param direction          either ASCENDING or DESCENDING
	 */
	@SuppressWarnings("EI2")
	public VisitationTimeComparator(int[] visitationTimeList, int direction) {
		m_visitationTimeList = visitationTimeList;
		m_direction = direction;

		if (direction != ASCENDING && direction != DESCENDING)
			throw new IllegalArgumentException();
	}

	public int compare(VertexType v1, VertexType v2) {
		int f1 = m_visitationTimeList[v1.getLabel()];
		int f2 = m_visitationTimeList[v2.getLabel()];

		if (m_direction == ASCENDING)
			return f1 - f2;
		else
			return f2 - f1;
	}

}

// vim:ts=4
