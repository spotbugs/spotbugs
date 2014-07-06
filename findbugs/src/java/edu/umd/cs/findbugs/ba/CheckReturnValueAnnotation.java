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

package edu.umd.cs.findbugs.ba;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.Detector;

/**
 * @author pugh
 */
public class CheckReturnValueAnnotation extends AnnotationEnumeration<CheckReturnValueAnnotation> {

    final int priority;

    final String pattern;

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_UNKNOWN = new CheckReturnValueAnnotation(
            "UnknownCheckReturnValue", 0, Detector.EXP_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_HIGH = new CheckReturnValueAnnotation(
            "CheckReturnValueHigh", 1, Detector.HIGH_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_MEDIUM = new CheckReturnValueAnnotation("CheckReturnValue",
            2, Detector.NORMAL_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_LOW = new CheckReturnValueAnnotation("CheckReturnValueLow",
            3, Detector.LOW_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_IGNORE = new CheckReturnValueAnnotation(
            "OkToIgnoreReturnValue", 4, Detector.IGNORE_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_VERY_HIGH = new CheckReturnValueAnnotation(
            "CheckReturnValueVeryHigh", 5, Detector.HIGH_PRIORITY - 1);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_LOW_BAD_PRACTICE = new CheckReturnValueAnnotation(
            "CheckReturnValueLowBadPractice", 6, "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", Detector.LOW_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE = new CheckReturnValueAnnotation(
            "CheckReturnValueMediumBadPractice", 7, "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", Detector.NORMAL_PRIORITY);

    public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE_INFERRED = new CheckReturnValueAnnotation(
            "CheckReturnValueInferred", 8, "RV_RETURN_VALUE_IGNORED_INFERRED", Detector.NORMAL_PRIORITY);

    private final static CheckReturnValueAnnotation[] myValues = { CHECK_RETURN_VALUE_UNKNOWN, CHECK_RETURN_VALUE_HIGH,
        CHECK_RETURN_VALUE_MEDIUM, CHECK_RETURN_VALUE_LOW, CHECK_RETURN_VALUE_IGNORE, CHECK_RETURN_VALUE_VERY_HIGH,
        CHECK_RETURN_VALUE_LOW_BAD_PRACTICE, CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE, CHECK_RETURN_VALUE_INFERRED};

    @CheckForNull
    public static CheckReturnValueAnnotation parse(String priority) {
        if (priority == null) {
            return CHECK_RETURN_VALUE_MEDIUM;
        }
        if (priority.endsWith("HIGH")) {
            return CHECK_RETURN_VALUE_HIGH;
        }
        if (priority.endsWith("MEDIUM")) {
            return CHECK_RETURN_VALUE_MEDIUM;
        }
        if (priority.endsWith("LOW")) {
            return CHECK_RETURN_VALUE_LOW;
        }
        throw new IllegalArgumentException("Bad priority: " + priority);

    }

    public static CheckReturnValueAnnotation[] values() {
        return myValues.clone();
    }

    public int getPriority() {
        return priority;
    }

    public String getPattern() {
        return pattern;
    }

    private CheckReturnValueAnnotation(String s, int i, int p) {
        super(s, i);
        pattern = "RV_RETURN_VALUE_IGNORED";
        priority = p;
    }

    private CheckReturnValueAnnotation(String s, int i, String pattern, int p) {
        super(s, i);
        this.pattern = pattern;
        priority = p;
    }

}
