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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies the package dependencies.
 *
 * @author Tom\u00e1s Pollak
 * @author Andrei Loskutov
 */
public class PackageDependenciesTest {

    private JDepend engine;

    @Test
    public void testGui2Dependencies() {
        String expectedNotEfferent = "edu.umd.cs.findbugs.gui2";

        assertPackageConstraint("edu.umd.cs.findbugs", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.asm", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.ba", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.bcel", expectedNotEfferent);
        assertPackageConstraint("edu.umd.cs.findbugs.classfile", expectedNotEfferent);
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

    @Before
    public void setUp() throws Exception {
        engine = new JDepend();

        // Get the classes root directory
        String classpath = System.getProperty("java.class.path");
        if (classpath == null) {
            String rootDirectory = new File(getClass().getResource("/").toURI()).getCanonicalPath();
            engine.addDirectory(rootDirectory);
        } else {
            String[] cpParts = classpath.split(File.pathSeparator);
            for (String cpStr : cpParts) {
                File file = new File(cpStr);
                if (file.isDirectory()) {
                    engine.addDirectory(file.getCanonicalPath());
                }
            }
        }

        // Setup the JDepend analysis
        engine.analyze();
    }

    @After
    public void tearDown() {
        engine = null;
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
