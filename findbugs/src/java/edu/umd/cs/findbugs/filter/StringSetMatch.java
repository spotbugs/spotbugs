/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Matches a string against a set of predefined values.
 *
 * Value set is defined using a String containing a comma separated value list.
 * Heading an trailing whitespace on the values is ignored in matching.
 *
 * @author rak
 */
public class StringSetMatch {
    private final Set<String> strings = new HashSet<String>();

    @Override
    public int hashCode() {
        return strings.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringSetMatch)) {
            return false;
        }
        return strings.equals(((StringSetMatch) o).strings);
    }

    /**
     * Constructor.
     *
     * @param strings
     *            comma-separated list of Strings
     */
    public StringSetMatch(String strings) {
        if (strings != null) {
            StringTokenizer tok = new StringTokenizer(strings, ",");
            while (tok.hasMoreTokens()) {
                this.strings.add(tok.nextToken().trim());
            }
        }
    }

    public boolean isEmpty() {
        return strings.isEmpty();
    }

    /**
     * Returns true if the given string is contained in the value set.
     *
     * @param string
     * @return true if the given string is contained in the value set
     */
    public boolean match(String string) {
        return strings.contains(string.trim());
    }

    @Override
    public String toString() {
        if (strings.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String s : strings) {
            result.append(s).append(",");
        }
        return result.substring(0, result.length() - 1);
    }
}
