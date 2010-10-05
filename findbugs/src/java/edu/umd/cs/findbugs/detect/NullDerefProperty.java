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
 * Warning properties for null pointer dereference and redundant null comparison
 * warnings.
 *
 * @author David Hovemeyer
 */
public class NullDerefProperty extends AbstractWarningProperty {

    private NullDerefProperty(String name, PriorityAdjustment priorityAdjustment) {
        super(name, priorityAdjustment);
    }

    /** Redundant null comparison is of a checked null value. */
    public static final NullDerefProperty CHECKED_VALUE = new NullDerefProperty("CHECKED_VALUE",
            PriorityAdjustment.RAISE_PRIORITY);

    /** Redundant null comparison is of a checked null value. */
    public static final NullDerefProperty LONG_RANGE_NULL_SOURCE = new NullDerefProperty("LONG_RANGE_NULL_SOURCE",
            PriorityAdjustment.AT_MOST_MEDIUM);

    /** dereference always on exception path */
    public static final NullDerefProperty ALWAYS_ON_EXCEPTION_PATH = new NullDerefProperty("ALWAYS_ON_EXCEPTION_PATH",
            PriorityAdjustment.AT_MOST_MEDIUM);

    /** Redundant nullcheck of previously dereferenced value. */
    public static final NullDerefProperty WOULD_HAVE_BEEN_A_KABOOM = new NullDerefProperty("WOULD_HAVE_BEEN_A_KABOOM",
            PriorityAdjustment.RAISE_PRIORITY);

    /** Redundant nullcheck created dead code. */
    public static final NullDerefProperty CREATED_DEAD_CODE = new NullDerefProperty("CREATED_DEAD_CODE",
            PriorityAdjustment.RAISE_PRIORITY);

    public static final NullDerefProperty DEREFS_ARE_CLONED = new NullDerefProperty("DEREFS_ARE_CLONED",
            PriorityAdjustment.AT_MOST_MEDIUM);

    public static final NullDerefProperty CLOSING_NULL = new NullDerefProperty("CLOSING_NULL", PriorityAdjustment.PEGGED_HIGH);

    public static final NullDerefProperty DEREFS_ARE_INLINED_FINALLY_BLOCKS = new NullDerefProperty(
            "DEREFS_ARE_INLINED_FINALLY_BLOCKS", PriorityAdjustment.AT_MOST_MEDIUM);

    public static final NullDerefProperty DEREFS_IN_CATCH_BLOCKS = new NullDerefProperty("DEREFS_IN_CATCH_BLOCKS",
            PriorityAdjustment.AT_MOST_MEDIUM);
}
