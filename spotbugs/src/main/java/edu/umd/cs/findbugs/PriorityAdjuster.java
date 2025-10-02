/*
 * SpotBugs - Find Bugs in Java programs
 * Copyright (C) 2025, Timo Thomas
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

import java.util.HashMap;
import java.util.Map;

public class PriorityAdjuster {
    Map<String, PriorityAdjustment> adjustments;

    public PriorityAdjuster(Map<String, String> adjustments) {
        this.adjustments = new HashMap<>();
        for (Map.Entry<String, String> entry : adjustments.entrySet()) {
            String target = checkTarget(entry.getKey());
            PriorityAdjustment priorityAdjustment = new PriorityAdjustment(entry.getValue());
            this.adjustments.put(target, priorityAdjustment);
        }
    }

    private static String checkTarget(String adjustmentTarget) {
        DetectorFactoryCollection factoryCollection = DetectorFactoryCollection.instance();
        DetectorFactory factory = factoryCollection.getFactoryByClassName(adjustmentTarget);
        if (factory != null) {
            // return the factory's FQCN
            return factory.getClass().getName();
        }
        factory = factoryCollection.getFactory(adjustmentTarget);
        if (factory != null) {
            // return the factory's FQCN
            return factory.getClass().getName();
        }
        BugPattern pattern = factoryCollection.lookupBugPattern(adjustmentTarget);
        if (pattern == null) {
            throw new IllegalArgumentException("Unknown detector or bug pattern: " + adjustmentTarget);
        }
        // return the bug pattern name
        return adjustmentTarget;
    }

    /**
     * Returns an instance with adjusted priority. If no adjustment is necessary, the original instance is returned.
     * Otherwise, a cloned instance is returned with an adjusted priority value.
     *
     * @param bugInstance the bug instance to adjust
     * @return the cloned and changed bug instance, or the original one in case that no change is necessary
     */
    BugInstance adjustPriority(BugInstance bugInstance) {
        if (adjustments.isEmpty()) {
            return bugInstance;
        }
        int priority = bugInstance.getPriority();
        DetectorFactory detectorFactory = bugInstance.getDetectorFactory();
        if (detectorFactory != null) {
            String detectorFactoryName = detectorFactory.getClass().getName();
            PriorityAdjustment factoryAdjustment = adjustments.get(detectorFactoryName);
            if (factoryAdjustment != null) {
                priority = factoryAdjustment.adjust(priority);
            }
        }
        String bugPattern = bugInstance.getBugPattern().getType();
        PriorityAdjustment patternAdjustment = adjustments.get(bugPattern);
        if (patternAdjustment != null) {
            priority = patternAdjustment.adjust(priority);
        }
        if (priority == bugInstance.getPriority()) {
            return bugInstance;
        }
        BugInstance clone = ((BugInstance) bugInstance.clone());
        clone.setPriority(priority);
        return clone;
    }
}
