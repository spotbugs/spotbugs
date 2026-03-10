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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.plan.ExecutionPlan;

public class PriorityAdjuster {
    Map<String, PriorityAdjustment> adjustments;
    private DetectorFactoryChooser factoryChooser;

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
            return factory.getFullName();
        }
        factory = factoryCollection.getFactory(adjustmentTarget);
        if (factory != null) {
            // return the factory's FQCN
            return factory.getFullName();
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
        int priority = bugInstance.getPriority();
        priority += adjustForDetector(bugInstance);

        if (!adjustments.isEmpty()) {
            DetectorFactory detectorFactory = bugInstance.getDetectorFactory();
            if (detectorFactory != null) {
                String detectorFactoryName = detectorFactory.getFullName();
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
        }
        priority = BugInstance.boundedPriority(priority);
        if (priority == bugInstance.getPriority()) {
            return bugInstance;
        }
        BugInstance clone = ((BugInstance) bugInstance.clone());
        clone.setPriority(priority);
        return clone;
    }

    /**
     * Adjust priority if the factory was forcibly enabled by {@link ExecutionPlan} although the user did not select it.
     *
     * @param bugInstance
     * @return adjusted bug priority
     */
    public int adjustForDetector(BugInstance bugInstance) {
        DetectorFactory factory = bugInstance.getDetectorFactory();
        if (factory != null && factoryChooser != null && factoryChooser.wasForciblyEnabled(factory)) {
            BugPattern bugPattern = bugInstance.getBugPattern();
            if (SystemProperties.ASSERTIONS_ENABLED && !"EXPERIMENTAL".equals(bugPattern.getCategory())
                    && !factory.getReportedBugPatterns().contains(bugPattern)) {
                AnalysisContext.logError(factory.getShortName() + " doesn't note that it reports " + bugPattern
                        + " in category " + bugPattern.getCategory());
            }
            return 100;
        }
        return 0;
    }

    /**
     * Set the factory chooser used by {@link ExecutionPlan}
     * @param factoryChooser the factory chooser, never null
     */
    public void setFactoryChooser(@NonNull DetectorFactoryChooser factoryChooser) {
        this.factoryChooser = factoryChooser;
    }
}
