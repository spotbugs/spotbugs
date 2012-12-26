/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom\u00e1s Pollak
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
package edu.umd.cs.findbugs.architecture;

import java.io.File;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import junit.framework.TestCase;

/**
 * Verifies the package dependencies.
 *
 * @author Tom\u00e1s Pollak
 * @author Andrei Loskutov
 */
public class PackageDependenciesTest extends TestCase {
    private JDepend engine;

    public void testGui2Dependencies() {
        String expectedNotEfferent = "edu.umd.cs.findbugs.gui2";

        assertPackageConstraint("edu.umd.cs.findbugs", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.asm", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.ba", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.bcel", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.classfile", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.cloud", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.detect", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.graph", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.io", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.log", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.model", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.plan", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.util", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.visitclass", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.xml", expectedNotEfferent);

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Get the classes root directory
        String rootDirectory = new File(getClass().getResource("/").toURI()).getCanonicalPath();

        // Setup the JDepend analysis
        engine = new JDepend();
        engine.addDirectory(rootDirectory);
        engine.analyze();
    }

    @Override
    protected void tearDown() throws Exception {
        engine = null;

        super.tearDown();
    }

    private void assertPackageConstraint(String afferent, String expectedNotEfferent) {
        JavaPackage afferentPackage = engine.getPackage(afferent);
        assertNotNull("Afferent package not found: " + afferent, afferentPackage);
        JavaPackage efferentPackage = engine.getPackage(expectedNotEfferent);
        assertNotNull("Efferent package not found: " + efferentPackage, efferentPackage);
        assertFalse(afferentPackage.getName() + " shouldn't depend on " + efferentPackage.getName(), afferentPackage
                .getEfferents().contains(efferentPackage));
    }
}
