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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;

/**
 * Warning property for a null argument being passed to a method which might
 * dereference it.
 *
 * @author David Hovemeyer
 */
public class NullArgumentWarningProperty extends AbstractWarningProperty {
    private NullArgumentWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
        super(name, priorityAdjustment);
    }

    public static final NullArgumentWarningProperty ARG_DEFINITELY_NULL = new NullArgumentWarningProperty("ARG_DEFINITELY_NULL",
            PriorityAdjustment.NO_ADJUSTMENT);

    public static final NullArgumentWarningProperty MONOMORPHIC_CALL_SITE = new NullArgumentWarningProperty(
            "MONOMORPHIC_CALL_SITE", PriorityAdjustment.NO_ADJUSTMENT);

    public static final NullArgumentWarningProperty ALL_DANGEROUS_TARGETS = new NullArgumentWarningProperty(
            "ALL_DANGEROUS_TARGETS", PriorityAdjustment.NO_ADJUSTMENT);

    public static final NullArgumentWarningProperty ACTUAL_PARAMETER_GUARANTEED_NULL = new NullArgumentWarningProperty(
            "ACTUAL_PARAMETER_GUARANTEED_NULL", PriorityAdjustment.NO_ADJUSTMENT);
}
