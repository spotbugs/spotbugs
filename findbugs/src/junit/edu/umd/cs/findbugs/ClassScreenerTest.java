/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

import junit.framework.Assert;
import junit.framework.TestCase;

public class ClassScreenerTest extends TestCase {
    private IClassScreener emptyScreener;

    private ClassScreener particularClassScreener;

    private ClassScreener particularPackageScreener;

    private ClassScreener particularPackageScreener2;

    private static String makeFileName(String className) {
        return className.replace('.', '/') + ".class";
    }

    private static String makeJarURL(String fileName) {
        return "jar:http://foo.com/bar.jar!/" + fileName;
    }

    private static final String FOOBAR_PACKAGE = "com.foobar";

    private static final String FOOBAR_PACKAGE_WITH_TRAILING_DOT = "com.foobar.";

    private static final String FURRYLEMUR_PACKAGE = "org.furrylemur";

    private static final String SOME_CLASS = FOOBAR_PACKAGE + ".SomeClass";

    private static final String SOME_OTHER_CLASS = FOOBAR_PACKAGE + ".SomeOtherClass";

    private static final String UNRELATED_THING_CLASS = FURRYLEMUR_PACKAGE + ".UnrelatedThing";

    private static final String SOME_CLASS_FILENAME = makeFileName(SOME_CLASS);

    private static final String SOME_OTHER_CLASS_FILENAME = makeFileName(SOME_OTHER_CLASS);

    private static final String UNRELATED_THING_CLASS_FILENAME = makeFileName(UNRELATED_THING_CLASS);

    private static final String SOME_CLASS_JARFILENAME = makeJarURL(SOME_CLASS_FILENAME);

    private static final String SOME_OTHER_CLASS_JARFILENAME = makeJarURL(SOME_OTHER_CLASS_FILENAME);

    private static final String UNRELATED_THING_CLASS_JARFILENAME = makeJarURL(UNRELATED_THING_CLASS_FILENAME);

    @Override
    protected void setUp() {
        emptyScreener = new ClassScreener();

        particularClassScreener = new ClassScreener();
        particularClassScreener.addAllowedClass(SOME_CLASS);

        particularPackageScreener = new ClassScreener();
        particularPackageScreener.addAllowedPackage(FOOBAR_PACKAGE);

        particularPackageScreener2 = new ClassScreener();
        particularPackageScreener2.addAllowedPackage(FOOBAR_PACKAGE_WITH_TRAILING_DOT);
    }

    public void testEmptyClassScreener() {
        Assert.assertTrue(emptyScreener.matches(SOME_CLASS_FILENAME));
        Assert.assertTrue(emptyScreener.matches(SOME_OTHER_CLASS_FILENAME));
        Assert.assertTrue(emptyScreener.matches(UNRELATED_THING_CLASS_FILENAME));

        Assert.assertTrue(emptyScreener.matches(SOME_CLASS_JARFILENAME));
        Assert.assertTrue(emptyScreener.matches(SOME_OTHER_CLASS_JARFILENAME));
        Assert.assertTrue(emptyScreener.matches(UNRELATED_THING_CLASS_JARFILENAME));
    }

    public void testParticularClassScreener() {
        Assert.assertTrue(particularClassScreener.matches(SOME_CLASS_FILENAME));
        Assert.assertFalse(particularClassScreener.matches(SOME_OTHER_CLASS_FILENAME));
        Assert.assertFalse(particularClassScreener.matches(UNRELATED_THING_CLASS_FILENAME));

        Assert.assertTrue(particularClassScreener.matches(SOME_CLASS_JARFILENAME));
        Assert.assertFalse(particularClassScreener.matches(SOME_OTHER_CLASS_JARFILENAME));
        Assert.assertFalse(particularClassScreener.matches(UNRELATED_THING_CLASS_JARFILENAME));
    }

    public void testParticularPackageScreener() {
        testPackageScreener(particularPackageScreener);
        testPackageScreener(particularPackageScreener2);
    }

    private void testPackageScreener(IClassScreener screener) {
        Assert.assertTrue(screener.matches(SOME_CLASS_FILENAME));
        Assert.assertTrue(screener.matches(SOME_OTHER_CLASS_FILENAME));
        Assert.assertFalse(screener.matches(UNRELATED_THING_CLASS_FILENAME));
        Assert.assertTrue(screener.matches(SOME_CLASS_JARFILENAME));
        Assert.assertTrue(screener.matches(SOME_OTHER_CLASS_JARFILENAME));
        Assert.assertFalse(screener.matches(UNRELATED_THING_CLASS_JARFILENAME));
    }
}

