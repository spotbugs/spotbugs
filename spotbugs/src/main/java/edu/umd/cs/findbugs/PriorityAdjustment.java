/*
 * SpotBugs - Find Bugs in Java programs
 * Copyright (C) 2025, Timo Thomas
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

import java.util.regex.Pattern;

class PriorityAdjustment {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private final int adjustmentValue;
    private final boolean isDelta;

    PriorityAdjustment(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Priority adjustment value cannot be null or empty");
        }
        if (value.startsWith("+")) {
            String num = value.substring(1);
            if (!NUMBER_PATTERN.matcher(num).matches()) {
                throw wrongFormat(value);
            }
            isDelta = true;
            adjustmentValue = Integer.parseInt(num);
        } else if (value.startsWith("-")) {
            String num = value.substring(1);
            if (!NUMBER_PATTERN.matcher(num).matches()) {
                throw wrongFormat(value);
            }
            isDelta = true;
            adjustmentValue = -Integer.parseInt(num);
        } else if (NUMBER_PATTERN.matcher(value).matches()) {
            isDelta = false;
            adjustmentValue = Integer.parseInt(value);
        } else if ("raise".equals(value)) {
            isDelta = true;
            adjustmentValue = -1;
        } else if ("lower".equals(value)) {
            isDelta = true;
            adjustmentValue = +1;
        } else if ("suppress".equals(value)) {
            isDelta = true;
            adjustmentValue = +100;
        } else {
            throw wrongFormat(value);
        }
    }

    private static IllegalArgumentException wrongFormat(String value) {
        return new IllegalArgumentException("Invalid priority adjustment value: " + value
                + ". Must be +[integer], -[integer], [integer] or one of raise|lower|suppress");
    }

    public int adjust(int priority) {
        return isDelta ? priority + adjustmentValue : adjustmentValue;
    }
}
