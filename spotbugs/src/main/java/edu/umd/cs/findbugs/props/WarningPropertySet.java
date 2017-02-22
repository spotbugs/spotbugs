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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.Priorities;

/**
 * A Set of WarningProperty objects, each with an optional attribute Object. A
 * WarningPropertySet is useful for collecting heuristics to use in the
 * determination of whether or not a warning is a false positive, or what the
 * warning's priority should be.
 *
 * @author David Hovemeyer
 */
public class WarningPropertySet<T extends WarningProperty> implements Cloneable {
    private final Map<T, Object> map;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("{ ");
        for (Map.Entry<T, Object> entry : map.entrySet()) {
            WarningProperty prop = entry.getKey();
            Object attribute = entry.getValue();
            buf.append("  ");
            buf.append(prop.getPriorityAdjustment());
            buf.append("\t");
            buf.append(prop.getName());
            buf.append("\t");
            buf.append(attribute);
            buf.append("\n");
        }
        buf.append("}\n");
        return buf.toString();
    }

    /**
     * Constructor Creates empty object.
     */
    public WarningPropertySet() {
        this.map = new HashMap<T, Object>();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Add a warning property to the set. The warning implicitly has the boolean
     * value "true" as its attribute.
     *
     * @param prop
     *            the WarningProperty
     * @return this object
     */
    public WarningPropertySet<T> addProperty(T prop) {
        map.put(prop, Boolean.TRUE);
        return this;
    }

    /**
     * Remove a warning property from the set.
     *
     * @param prop
     *            the WarningProperty
     * @return this object
     */
    public WarningPropertySet<T> removeProperty(T prop) {
        map.remove(prop);
        return this;
    }

    /**
     * Add a warning property and its attribute value.
     *
     * @param prop
     *            the WarningProperty
     * @param value
     *            the attribute value
     * @return this object
     */
    public WarningPropertySet<T> setProperty(T prop, String value) {
        map.put(prop, value);
        return this;
    }

    /**
     * Add a warning property and its attribute value.
     *
     * @param prop
     *            the WarningProperty
     * @param value
     *            the attribute value
     */
    public void setProperty(T prop, Boolean value) {
        map.put(prop, value);
    }

    /**
     * Return whether or not the set contains the given WarningProperty.
     *
     * @param prop
     *            the WarningProperty
     * @return true if the set contains the WarningProperty, false if not
     */
    public @CheckReturnValue
    boolean containsProperty(T prop) {
        return map.keySet().contains(prop);
    }

    /**
     * Check whether or not the given WarningProperty has the given attribute
     * value.
     *
     * @param prop
     *            the WarningProperty
     * @param value
     *            the attribute value
     * @return true if the set contains the WarningProperty and has an attribute
     *         equal to the one given, false otherwise
     */
    public boolean checkProperty(T prop, Object value) {
        Object attribute = getProperty(prop);
        return (attribute != null && attribute.equals(value));
    }

    /**
     * Get the value of the attribute for the given WarningProperty. Returns
     * null if the set does not contain the WarningProperty.
     *
     * @param prop
     *            the WarningProperty
     * @return the WarningProperty's attribute value, or null if the set does
     *         not contain the WarningProperty
     */
    public Object getProperty(T prop) {
        return map.get(prop);
    }

    /**
     * Use the PriorityAdjustments specified by the set's WarningProperty
     * elements to compute a warning priority from the given base priority.
     *
     * @param basePriority
     *            the base priority
     * @return the computed warning priority
     */
    public int computePriority(int basePriority) {
        boolean relaxedReporting = FindBugsAnalysisFeatures.isRelaxedMode();

        boolean atLeastMedium = false;
        boolean falsePositive = false;
        boolean atMostLow = false;
        boolean atMostMedium = false;
        boolean peggedHigh = false;
        int aLittleBitLower = 0;
        int priority = basePriority;
        if (!relaxedReporting) {
            for (T warningProperty : map.keySet()) {
                PriorityAdjustment adj = warningProperty.getPriorityAdjustment();
                if (adj == PriorityAdjustment.PEGGED_HIGH) {
                    peggedHigh = true;
                    priority--;
                } else if (adj == PriorityAdjustment.FALSE_POSITIVE) {
                    falsePositive = true;
                    atMostLow = true;
                } else if (adj == PriorityAdjustment.A_LITTLE_BIT_LOWER_PRIORITY) {
                    aLittleBitLower++;
                } else if (adj == PriorityAdjustment.A_LITTLE_BIT_HIGHER_PRIORITY) {
                    aLittleBitLower--;
                } else if (adj == PriorityAdjustment.RAISE_PRIORITY) {
                    --priority;
                } else if (adj == PriorityAdjustment.RAISE_PRIORITY_TO_AT_LEAST_NORMAL) {
                    --priority;
                    atLeastMedium = true;
                } else if (adj == PriorityAdjustment.LOWER_PRIORITY_TO_AT_MOST_NORMAL) {
                    ++priority;
                    atMostMedium = true;
                } else if (adj == PriorityAdjustment.RAISE_PRIORITY_TO_HIGH) {

                    return Priorities.HIGH_PRIORITY;
                } else if (adj == PriorityAdjustment.LOWER_PRIORITY) {
                    ++priority;
                } else if (adj == PriorityAdjustment.AT_MOST_LOW) {
                    priority++;
                    atMostLow = true;
                } else if (adj == PriorityAdjustment.AT_MOST_MEDIUM) {
                    atMostMedium = true;
                } else if (adj == PriorityAdjustment.NO_ADJUSTMENT) {
                    assert true; // do nothing
                } else {
                    throw new IllegalStateException("Unknown priority " + adj);
                }

            }

            if (peggedHigh && !falsePositive) {
                return Priorities.HIGH_PRIORITY;
            }
            if (aLittleBitLower >= 3 || priority == 1 && aLittleBitLower == 2) {
                priority++;
            } else if (aLittleBitLower <= -2) {
                priority--;
            }
            if (atMostMedium) {
                priority = Math.max(Priorities.NORMAL_PRIORITY, priority);
            }

            if (falsePositive && !atLeastMedium) {
                return Priorities.EXP_PRIORITY + 1;
            } else if (atMostLow) {
                return Math.min(Math.max(Priorities.LOW_PRIORITY, priority), Priorities.EXP_PRIORITY);
            }
            if (atLeastMedium && priority > Priorities.NORMAL_PRIORITY) {
                priority = Priorities.NORMAL_PRIORITY;
            }

            if (priority < Priorities.HIGH_PRIORITY) {
                priority = Priorities.HIGH_PRIORITY;
            } else if (priority > Priorities.EXP_PRIORITY) {
                priority = Priorities.EXP_PRIORITY;
            }
        }

        return priority;
    }

    /**
     * Determine whether or not a warning with given priority is expected to be
     * a false positive.
     *
     * @param priority
     *            the priority
     * @return true if the warning is expected to be a false positive, false if
     *         not
     */
    public boolean isFalsePositive(int priority) {
        return priority > Priorities.EXP_PRIORITY;
    }

    /**
     * Decorate given BugInstance with properties.
     *
     * @param bugInstance
     *            the BugInstance
     */
    public void decorateBugInstance(BugInstance bugInstance) {
        int priority = computePriority(bugInstance.getPriority());
        bugInstance.setPriority(priority);
        for (Map.Entry<T, Object> entry : map.entrySet()) {
            WarningProperty prop = entry.getKey();
            Object attribute = entry.getValue();
            if (attribute == null) {
                attribute = "";
            }
            bugInstance.setProperty(prop.getName(), attribute.toString());
        }
    }
}
