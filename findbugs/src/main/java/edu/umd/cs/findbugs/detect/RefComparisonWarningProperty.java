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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;

/**
 * Warning properties for FindRefComparison detector.
 *
 * @author David Hovemeyer
 */
public class RefComparisonWarningProperty extends AbstractWarningProperty {
    private RefComparisonWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
        super(name, priorityAdjustment);
    }

    /** There is a call to equals() in the method. */
    public static final RefComparisonWarningProperty SAW_CALL_TO_EQUALS = new RefComparisonWarningProperty("SAW_CALL_TO_EQUALS",
            PriorityAdjustment.AT_MOST_LOW);

    /** Method is private (or package-protected). */
    public static final RefComparisonWarningProperty PRIVATE_METHOD = new RefComparisonWarningProperty("PRIVATE_METHOD",
            PriorityAdjustment.AT_MOST_MEDIUM);

    /** Compare inside test case */
    public static final RefComparisonWarningProperty COMPARE_IN_TEST_CASE = new RefComparisonWarningProperty(
            "COMPARE_IN_TEST_CASE", PriorityAdjustment.AT_MOST_LOW);

    /** Comparing static strings using equals operator. */
    public static final RefComparisonWarningProperty COMPARE_STATIC_STRINGS = new RefComparisonWarningProperty(
            "COMPARE_STATIC_STRINGS", PriorityAdjustment.FALSE_POSITIVE);

    /** Comparing a dynamic string using equals operator. */
    public static final RefComparisonWarningProperty DYNAMIC_AND_UNKNOWN = new RefComparisonWarningProperty(
            "DYNAMIC_AND_UNKNOWN", PriorityAdjustment.RAISE_PRIORITY);

    public static final RefComparisonWarningProperty STRING_PARAMETER_IN_PUBLIC_METHOD = new RefComparisonWarningProperty(
            "STATIC_AND_PARAMETER_IN_PUBLIC_METHOD", PriorityAdjustment.RAISE_PRIORITY);

    public static final RefComparisonWarningProperty STRING_PARAMETER = new RefComparisonWarningProperty("STATIC_AND_PARAMETER",
            PriorityAdjustment.NO_ADJUSTMENT);

    /** Comparing static string and an unknown string. */
    public static final RefComparisonWarningProperty STATIC_AND_UNKNOWN = new RefComparisonWarningProperty("STATIC_AND_UNKNOWN",
            PriorityAdjustment.LOWER_PRIORITY);
    /** Comparing static string and an unknown string. */
    public static final RefComparisonWarningProperty EMPTY_AND_UNKNOWN = new RefComparisonWarningProperty("EMPTY_AND_UNKNOWN",
            PriorityAdjustment.NO_ADJUSTMENT);

    /** Saw a call to String.intern(). */
    public static final RefComparisonWarningProperty SAW_INTERN = new RefComparisonWarningProperty("SAW_INTERN",
            PriorityAdjustment.LOWER_PRIORITY);
}
