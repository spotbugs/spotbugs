/*
 * FindBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.AnalysisFeatures.AnalysisFeature;

/**
 * Boolean-valued analysis properties for FindBugs.
 *
 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setBoolProperty(int, boolean)
 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getBoolProperty(int)
 * @author David Hovemeyer
 */
public abstract class FindBugsAnalysisFeatures {


    /**
     * "Relaxed" warning reporting mode. Rather than using hard-coded heuristics
     * to decide when to suppress a warning, report warnings freely and encode
     * the heuristics as BugProperties (for consumption by a
     * machine-learning-based ranking algorithm).
     */
    public static final @AnalysisFeature
    int RELAXED_REPORTING_MODE = AnalysisFeatures.Builder.build("RELAXED_REPORTING_MODE");

    /**
     * Enable interprocedural analysis.
     */
    public static final @AnalysisFeature
    int INTERPROCEDURAL_ANALYSIS = AnalysisFeatures.Builder.build("INTERPROCEDURAL_ANALYSIS");

    public static final @AnalysisFeature
    int INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES = AnalysisFeatures.Builder.build("INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES");

    static void setProperty(@AnalysisFeature int property, boolean value) {
        AnalysisContext.currentAnalysisContext().setBoolProperty(property, value);
    }

    static boolean getProperty(@AnalysisFeature int property) {
        return AnalysisContext.currentAnalysisContext().getBoolProperty(property);
    }

    /**
     * Set relaxed reporting mode.
     *
     * @param relaxedMode
     *            true if relaxed reporting mode should be enabled, false if not
     */
    public static void setRelaxedMode(boolean relaxedMode) {
        setProperty(RELAXED_REPORTING_MODE, relaxedMode);
    }

    /**
     * Get relaxed reporting mode.
     *
     * @return true if relaxed reporting mode should be enabled, false if not
     */
    public static boolean isRelaxedMode() {
        return getProperty(RELAXED_REPORTING_MODE);
    }
}
