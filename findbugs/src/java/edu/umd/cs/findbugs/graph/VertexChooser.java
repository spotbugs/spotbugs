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

// $Revision: 1.5 $

package edu.umd.cs.findbugs.graph;

/**
 * VertexChooser specifies an interface for objects that determine whether or
 * not a vertex is chosen for some purpose. For example, in the DepthFirstSearch
 * algorithm a VertexChooser may be specified to select which vertices should be
 * considered by the search.
 */
public interface VertexChooser<VertexType extends GraphVertex<VertexType>> {

    /**
     * Determine whether the given GraphVertex should be chosen.
     */
    public boolean isChosen(VertexType v);

}

