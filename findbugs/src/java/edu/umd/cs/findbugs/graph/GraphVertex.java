/*
 * Generic graph library
 * Copyright (C) 2000,2003 University of Maryland
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

// $Revision: 1.2 $

package edu.umd.cs.daveho.graph;

import java.io.Serializable;
import java.lang.Comparable;

/**
 * GraphVertex interface; represents a vertex in a graph.
 */
public interface GraphVertex<VertexKey>
	extends Serializable, Comparable<GraphVertex<VertexKey>> {

	/** Get the numeric label for this vertex. */
	public int getLabel();

	/** Set the numeric label for this vertex. */
	public void setLabel(int label);

	/** Get the VertexKey object uniquely identifying this vertex. */
	public VertexKey getVertexKey();

}

// vim:ts=4
