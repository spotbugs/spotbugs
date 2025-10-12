/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.plan.ExecutionPlan;

/**
 * Predicate for choosing DetectorFactory objects.
 *
 * @author David Hovemeyer
 */
public interface DetectorFactoryChooser {
    /**
     * Return whether or not given DetectorFactory should be chosen.
     *
     * @param factory
     *            the DetectorFactory
     * @return true if the DetectorFactory should be chosen, false if not
     */
    boolean choose(DetectorFactory factory);

    /**
     * Enable the factory due to ordering constraints with other enabled
     * detectors
     *
     * @param factory
     */
    void enable(DetectorFactory factory);

    /**
     * Check whether the given factory was enabled by {@link ExecutionPlan#build()}
     *
     * @param factory
     *            the DetectorFactory to check, not null
     * @return returns true by default
     * @see ExecutionPlan#build()
     */
    default boolean wasFactoryEnabled(@NonNull DetectorFactory factory) {
        return true;
    }
}
