/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;

/**
 * Boolean analysis properties for use in the AnalysisContext. These can be used
 * to enable or disable various analysis features in the bytecode analysis
 * framework.
 *
 * @author David Hovemeyer
 */
public interface AnalysisFeatures {

    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnalysisFeature {
    }


    public static class Builder {
        static int next = NUM_BOOLEAN_ANALYSIS_PROPERTIES;
        private static @AnalysisFeature int asFeatureNum(int num) { return num; }
        static @AnalysisFeature
        public int build(String name) {
            int num = next++;
            return asFeatureNum(num);
        }

    }
    /**
     * Determine (1) what exceptions can be thrown on exception edges, (2) which
     * catch blocks are reachable, and (3) which exception edges carry only
     * "implicit" runtime exceptions.
     */
    public static final @AnalysisFeature
    int ACCURATE_EXCEPTIONS = 0;

    /**
     * A boolean flag which if set means that analyses should try to conserve
     * space at the expense of precision.
     */
    public static final @AnalysisFeature
    int CONSERVE_SPACE = 1;

    /**
     * If true, model the effect of instanceof checks in type analysis.
     */
    public static final @AnalysisFeature
    int MODEL_INSTANCEOF = 2;

    /**
     * Skip generating CFG's and methodGen's for huge methods
     */
    public static final @AnalysisFeature
    int SKIP_HUGE_METHODS = 3;

    /**
     * Perform interative opcode stack analysis: always enabled.
     */
    public static final @Deprecated @AnalysisFeature
    int INTERATIVE_OPCODE_STACK_ANALYSIS = 4;

    /**
     * In the null pointer analysis, track null values that are guaranteed to be
     * dereferenced on some (non-implicit-exception) path.
     */
    public static final @AnalysisFeature
    int TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS = 5;

    /**
     * In the null pointer analysis, track value numbers that are known to be
     * null. This allows us to not lose track of null values that are not
     * currently in the stack frame but might be in a heap location where the
     * value is recoverable by redundant load elimination or forward
     * substitution.
     */
    public static final @AnalysisFeature
    int TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS = 6;

    /**
     * Merge similar warnings. If we are tracking warnings across versions, it
     * is useful to merge all similar issues together. Otherwise, when we
     * compare the warnings in two different versions, we will not be able to
     * match them up correctly.
     */
    public static final @AnalysisFeature
    int MERGE_SIMILAR_WARNINGS = 7;

    /**
     * Number of boolean analysis properties reserved for the bytecode analysis
     * framework. Clients of the framework may use property values &gt;= this
     * value.
     * @deprecated - use Builder instead
     */
    @Deprecated
    public static final @AnalysisFeature
    int NUM_BOOLEAN_ANALYSIS_PROPERTIES = 128;

}

