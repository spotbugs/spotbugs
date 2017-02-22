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

import java.util.Collection;
import java.util.Locale;


/**
 * A StringMatcher that checks to see if a candidate string (assumed to be a
 * camel-case word), when broken into components, contains a given word.
 *
 * @author David Hovemeyer
 */
public class ContainsCamelCaseWordStringMatcher implements StringMatcher {
    private final String expected;

    /**
     * Constructor. This StringMatcher will match any string which, when broken
     * into camel-case identifier components, has a component which matches the
     * (lower-cased) expected string value.
     *
     * @param expected
     *            the expected string value
     */
    public ContainsCamelCaseWordStringMatcher(String expected) {
        this.expected = expected.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public boolean matches(String s) {
        SplitCamelCaseIdentifier splitter = new SplitCamelCaseIdentifier(s);
        Collection<String> components = splitter.split();
        return components.contains(expected);
    }

    @Override
    public String toString() {
        return "camel-case id contains " + expected;
    }
}
