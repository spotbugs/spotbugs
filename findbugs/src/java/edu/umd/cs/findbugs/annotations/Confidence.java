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

package edu.umd.cs.findbugs.annotations;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.Priorities;

/**
 * Describes the confidence with which FindBugs reports a bug instance.
 */
public enum Confidence {
    HIGH(Priorities.HIGH_PRIORITY), MEDIUM(Priorities.NORMAL_PRIORITY), LOW(Priorities.LOW_PRIORITY), IGNORE(
            Priorities.IGNORE_PRIORITY);

    private final int confidenceValue;

    /** Given a numeric confidence value, report the corresponding confidence enum value */
    @Nonnull
    static public Confidence getConfidence(int prio) {
        for(Confidence c : values()) {
            if (prio <= c.confidenceValue) {
                return c;
            }
        }
        return Confidence.IGNORE;
    }

    public int getConfidenceValue() {
        return confidenceValue;
    }

    private Confidence(int p) {
        confidenceValue = p;
    }
}
