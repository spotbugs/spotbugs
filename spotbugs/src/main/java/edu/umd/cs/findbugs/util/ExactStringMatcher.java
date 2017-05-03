/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.util;

/**
 * Exact String-matching predicate.
 *
 * @author David Hovemeyer
 */
public class ExactStringMatcher implements StringMatcher {
    private final String expected;

    /**
     * Constructor.
     *
     * @param expected
     *            the expected string value
     */
    public ExactStringMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(String s) {
        return this.expected.equals(s);
    }

    @Override
    public String toString() {
        return expected;
    }
}
