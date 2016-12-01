/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.internalAnnotations;

import java.util.regex.Matcher;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class SlashedClassNameTest extends TestCase {

    public void testFoo() {
        Matcher m = SlashedClassName.Checker.simplePattern.matcher("Foo");
        assertTrue(m.matches());
    }

    public void testFooBar() {
        Matcher m = SlashedClassName.Checker.simplePattern.matcher("foo.Bar");
        assertFalse(m.matches());
        m = SlashedClassName.Checker.pattern.matcher("foo.Bar");
        assertTrue(!m.matches());
    }

    public void testFoo1Bar2() {
        Matcher m = SlashedClassName.Checker.simplePattern.matcher("foo1.Bar2");
        assertFalse(m.matches());
        m = SlashedClassName.Checker.pattern.matcher("foo1.Bar2");
        assertTrue(!m.matches());
    }
}
