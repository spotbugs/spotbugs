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
package edu.umd.cs.findbugs.props;

/**
 * Enum representing how a particular warning property is expected to affect its
 * likelihood of being serious, benign, or a false positive.
 *
 * @author David Hovemeyer
 */
public class PriorityAdjustment {
    private final String value;

    private PriorityAdjustment(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /** No adjustment to the priority. */
    public static final PriorityAdjustment NO_ADJUSTMENT = new PriorityAdjustment("NO_ADJUSTMENT");

    /** Raise the priority. */
    public static final PriorityAdjustment RAISE_PRIORITY = new PriorityAdjustment("RAISE_PRIORITY");

    /** Raise the priority. */
    public static final PriorityAdjustment RAISE_PRIORITY_TO_AT_LEAST_NORMAL = new PriorityAdjustment(
            "RAISE_PRIORITY_TO_AT_LEAST_NORMAL");

    /** lower the priority. */
    public static final PriorityAdjustment LOWER_PRIORITY_TO_AT_MOST_NORMAL = new PriorityAdjustment(
            "LOWER_PRIORITY_TO_AT_MOST_NORMAL");

    /** Raise the priority. */
    public static final PriorityAdjustment RAISE_PRIORITY_TO_HIGH = new PriorityAdjustment("RAISE_PRIORITY_TO_HIGH");

    /** Priority is at most low. */
    public static final PriorityAdjustment AT_MOST_LOW = new PriorityAdjustment("AT_MOST_LOW");

    /** Priority is at most medium. */
    public static final PriorityAdjustment AT_MOST_MEDIUM = new PriorityAdjustment("AT_MOST_MEDIUM");

    /** Pegged high */
    public static final PriorityAdjustment PEGGED_HIGH = new PriorityAdjustment("PEGGED_HIGH");

    /** Lower the priority. */
    public static final PriorityAdjustment LOWER_PRIORITY = new PriorityAdjustment("LOWER_PRIORITY");

    /** Lower the priority a little */
    public static final PriorityAdjustment A_LITTLE_BIT_LOWER_PRIORITY = new PriorityAdjustment("A_LITTLE_BIT_LOWER_PRIORITY");

    /** Raise the priority a little */
    public static final PriorityAdjustment A_LITTLE_BIT_HIGHER_PRIORITY = new PriorityAdjustment("A_LITTLE_BIT_HIGHER_PRIORITY");

    /** Warning is likely to be a false positive. */
    public static final PriorityAdjustment FALSE_POSITIVE = new PriorityAdjustment("FALSE_POSITIVE");
}
