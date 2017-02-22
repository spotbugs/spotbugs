/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006,2008 University of Maryland
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

package edu.umd.cs.findbugs.util;

import junit.framework.TestCase;

public class StringsTest extends TestCase {

    public static String[] escapedStrings = {
        // mixed entities/unicode escape sequences
        "a b c 1 2 3 &amp; &lt; &gt; &quot; &apos; \\u0005 \\u0013 &#955; \\\\u0007",
        // *even* series of prefixed slashes + \\u -> do escape
        "a b c \\\\\\u0005",
        // *odd* series of prefixed slashes + \\u -> don't escape
        "a b c \\\\\\\\u0005",
        // mixed even/odd prefixed slashes
        "a b c \\\\\\u0005 \\\\\\\\u0013",
        // make sure slashes work on their own (no double un/escaping)
        "\\\\\\",
        // make sure that normal characters are handled correctly if they
        // appear after escapes
        "a b c 1 2 3 &amp; &lt; &gt; &quot; &apos; \\u0005 \\u0013 &#955; \\\\u0007 a b c 1 2 3",
        // escaping a null string should be safe
        null,
        // an empty string should be safe too
        "", };

    public static String[] unescapedStrings = { "a b c 1 2 3 & < > \" ' \u0005 \u0013 \u03BB \\\\u0007", "a b c \\\\\u0005",
        "a b c \\\\\\\\u0005", "a b c \\\\\u0005 \\\\\\\\u0013", "\\\\\\",
        "a b c 1 2 3 & < > \" ' \u0005 \u0013 \u03BB \\\\u0007 a b c 1 2 3", null, "", };

    public void testEscapeXml() {
        assert (escapedStrings.length == unescapedStrings.length);
        for (int i = 0; i < unescapedStrings.length; i++) {
            if (unescapedStrings[i] == null) {
                assert (Strings.escapeXml(unescapedStrings[i]) == null);
            } else {
                assert (Strings.escapeXml(unescapedStrings[i]).compareTo(escapedStrings[i]) == 0);
            }
        }
    }

    public void testUnescapeXml() {
        assert (escapedStrings.length == unescapedStrings.length);
        for (int i = 0; i < escapedStrings.length; i++) {
            if (escapedStrings[i] == null) {
                assert (Strings.unescapeXml(escapedStrings[i]) == null);
            } else {
                assert (Strings.unescapeXml(escapedStrings[i]).compareTo(unescapedStrings[i]) == 0);
            }
        }
    }

    public void checkEscapeLFCRBackSlash(String expected, String argument) {
        assertEquals(argument, expected, Strings.escapeLFCRBackSlash(argument));

    }

    public void testEscapeLFCRBackSlash() {

        checkEscapeLFCRBackSlash("abc", "abc");
        checkEscapeLFCRBackSlash("\\n", "\n");
        checkEscapeLFCRBackSlash("\\r", "\r");
        checkEscapeLFCRBackSlash("\\\\a\\r", "\\a\r");
    }

}
