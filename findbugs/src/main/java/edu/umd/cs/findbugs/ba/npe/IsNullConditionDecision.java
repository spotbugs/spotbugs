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

package edu.umd.cs.findbugs.ba.npe;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.util.Util;

/**
 * A control decision which resulted in information being gained about whether a
 * particular value is null or non-null on the IFCMP_EDGE and FALL_THROUGH_EDGE
 * branches.
 *
 * @see IsNullValue
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 */
public class IsNullConditionDecision {
    private final @CheckForNull
    ValueNumber value;

    private final @CheckForNull
    IsNullValue ifcmpDecision;

    private final @CheckForNull
    IsNullValue fallThroughDecision;

    /**
     * Constructor.
     *
     * @param value
     *            the ValueNumber for which we have new information; null if no
     *            new information
     * @param ifcmpDecision
     *            the decision for the IFCMP_EDGE; null if that edge is not
     *            feasible
     * @param fallThroughDecision
     *            the decision for the FALL_THROUGH_EDGE; null if that edge is
     *            not feasible
     */
    public IsNullConditionDecision(@CheckForNull ValueNumber value, @CheckForNull IsNullValue ifcmpDecision,
            @CheckForNull IsNullValue fallThroughDecision) {
        this.value = value;

        // At least one of the edges must be feasible
        assert !(ifcmpDecision == null && fallThroughDecision == null);
        this.ifcmpDecision = ifcmpDecision;
        this.fallThroughDecision = fallThroughDecision;
    }

    @Override
    public int hashCode() {
        return Util.nullSafeHashcode(value) + 5 * Util.nullSafeHashcode(ifcmpDecision) + 17
                * Util.nullSafeHashcode(fallThroughDecision);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IsNullConditionDecision)) {
            return false;
        }
        IsNullConditionDecision other = (IsNullConditionDecision) o;
        return Util.nullSafeEquals(value, other.value) && Util.nullSafeEquals(ifcmpDecision, other.ifcmpDecision)
                && Util.nullSafeEquals(fallThroughDecision, other.fallThroughDecision);
    }

    /**
     * Get the value about which the branch yields information.
     */
    public ValueNumber getValue() {
        return value;
    }

    /**
     * Determine whether or not the comparison is redundant.
     */
    public boolean isRedundant() {
        return ifcmpDecision == null || fallThroughDecision == null;
    }

    /**
     * Determine whether or not the given edge is feasible. An edge may be
     * infeasible if the comparison is redundant (i.e., can only be determined
     * one way)
     *
     * @param edgeType
     *            the type of edge; must be IFCMP_EDGE or FALL_THROUGH_EDGE
     * @return true if the edge is feasible, false if infeasible
     */
    public boolean isEdgeFeasible(int edgeType) {
        IsNullValue decision = getDecision(edgeType);
        return decision != null;
    }

    /**
     * Get the decision reached about the value on outgoing edge of given type.
     *
     * @param edgeType
     *            the type of edge; must be IFCMP_EDGE or FALL_THROUGH_EDGE
     * @return the IsNullValue representing the decision, or null if the edge is
     *         infeasible
     */
    public @CheckForNull
    IsNullValue getDecision(int edgeType) {
        switch (edgeType) {
        case EdgeTypes.IFCMP_EDGE:
            return ifcmpDecision;
        case EdgeTypes.FALL_THROUGH_EDGE:
            return fallThroughDecision;
        default:
            throw new IllegalArgumentException("Bad edge type: " + edgeType);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(value != null ? value.toString() : "NoValue,");
        buf.append("ifcmp=");
        buf.append(ifcmpDecision != null ? Strings.trimComma(ifcmpDecision.toString()) : "INFEASIBLE");
        buf.append(",fallthru=");
        buf.append(fallThroughDecision != null ? Strings.trimComma(fallThroughDecision.toString()) : "INFEASIBLE");
        return buf.toString();
    }
}

