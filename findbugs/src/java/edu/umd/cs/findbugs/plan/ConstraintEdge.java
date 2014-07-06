/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.graph.AbstractEdge;

/**
 * Edge in a ConstraintGraph. Edges flow from earlier detectors to later
 * detectors.
 *
 * @see ConstraintGraph
 * @see DetectorNode
 * @see ExecutionPlan
 * @author David Hovemeyer
 */
public class ConstraintEdge extends AbstractEdge<ConstraintEdge, DetectorNode> {
    private DetectorOrderingConstraint constraint;

    /**
     * Constructor.
     *
     * @param source
     *            the source vertex (earlier Detector)
     * @param target
     *            the target vertex (later Detector)
     */
    public ConstraintEdge(DetectorNode source, DetectorNode target) {
        super(source, target);
    }

    /**
     * Set the DetectorOrderingConstraint that created this edge.
     *
     * @param constraint
     *            the DetectorOrderingConstraint that created this edge
     */
    public void setConstraint(DetectorOrderingConstraint constraint) {
        this.constraint = constraint;
    }

    /**
     * Determine whether or not this ConstraintEdge resulted from an ordering
     * constraint having a single detector as its source (ealier detector). Such
     * constraints automatically enable the source (earlier) detector if the
     * target (later) detector is enabled.
     *
     * @return true if this edge has a single detector as its source (earlier
     *         detector)
     */
    public boolean isSingleSource() {
        return constraint.isSingleSource();
    }
}

