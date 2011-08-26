/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom�s Pollak
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
package de.tobject.findbugs.builder.test;

import static junit.framework.Assert.assertEquals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the public methods for FindBugsWorker.
 *
 * @author Tom�s Pollak
 */
public class FindBugsWorkerTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private static final String CLASS_A_PROJECT_RELATIVE = SRC + "/A.java";

    private static final String CLASS_A_WORKSPACE_RELATIVE = TEST_PROJECT + "/" + CLASS_A_PROJECT_RELATIVE;

    @Test
    public void testBaselineBugs() throws CoreException {
        assertNoBugs();

        setBaselineBugsFile(true);
        work(createFindBugsWorker());
        setBaselineBugsFile(false);

        assertExpectedBugsWithBaseline();
    }

    @Test
    public void testFilter() throws CoreException{
        assertNoBugs();

        setFilterFile(true);
        work(createFindBugsWorker());
        setFilterFile(false);

        assertExpectedBugsWithFilter();
    }

    @Test
    public void testGetFilterPath() throws JavaModelException {
        IPath classALocation = getClassA().getResource().getLocation();
        assertEquals(classALocation, FindBugsWorker.getFilterPath(classALocation.toOSString(), getProject()));
        assertEquals(classALocation, FindBugsWorker.getFilterPath(CLASS_A_PROJECT_RELATIVE, getProject()));
        assertEquals(classALocation, FindBugsWorker.getFilterPath(CLASS_A_WORKSPACE_RELATIVE, getProject()));
        assertEquals(classALocation, FindBugsWorker.getFilterPath(CLASS_A_WORKSPACE_RELATIVE, null));
    }

    @Test
    public void testLoadXML() throws CoreException {
        assertNoBugs();

        loadXml(createFindBugsWorker(), getBugsFileLocation());

        assertExpectedBugs();
    }

    @Test
    public void testRunFindBugs() throws CoreException {
        assertNoBugs();

        work(createFindBugsWorker());

        assertExpectedBugs();
    }

    @Test
    public void testToFilterPath() throws JavaModelException {
        String classALocation = getClassA().getResource().getLocation().toOSString();
        assertEquals(new Path(CLASS_A_PROJECT_RELATIVE), FindBugsWorker.toFilterPath(classALocation, getProject()));
        assertEquals(new Path(CLASS_A_WORKSPACE_RELATIVE), FindBugsWorker.toFilterPath(classALocation, null));
    }

    private void assertExpectedBugsWithBaseline() throws CoreException {
        assertNoBugs();
    }

    private void assertExpectedBugsWithFilter() throws CoreException {
        assertNoBugs();
    }
}
