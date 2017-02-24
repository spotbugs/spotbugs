/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.graph.AbstractVertex;

/**
 * Node in a ConstraintGraph. It represents a single Detector which must be
 * ordered before or after some other Detector(s).
 *
 * @see ConstraintGraph
 * @see ConstraintEdge
 * @see ExecutionPlan
 * @author David Hovemeyer
 */
public class DetectorNode extends AbstractVertex<ConstraintEdge, DetectorNode> {
    private final DetectorFactory factory;

    /**
     * Constructor.
     *
     * @param factory
     *            the DetectorFactory for the Detector this node represents
     */
    public DetectorNode(DetectorFactory factory) {
        this.factory = factory;
    }

    /**
     * Get the DetectorFactory.
     */
    public DetectorFactory getFactory() {
        return factory;
    }

    /**
     * Get the Plugin Name for this DetectorFactory for debugging support
     */
    @Override
    public String toString() {
        return "DetectorNode[" + factory.getReportedBugPatternCodes() + "]";
    }
}

