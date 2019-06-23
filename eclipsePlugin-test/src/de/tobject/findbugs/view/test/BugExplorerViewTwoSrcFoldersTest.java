/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.view.test;

import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.test.TestScenario;
import de.tobject.findbugs.view.explorer.GroupType;

/**
 * This class tests the BugExplorerView and its related classes. This tests a
 * different scenario with two source folders.
 *
 * @author Tomás Pollak
 */
public class BugExplorerViewTwoSrcFoldersTest extends AbstractBugExplorerViewTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.TWO_SRC_FOLDERS);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testViewContents_runTwice_onceOnSrcFolder() throws CoreException {
        // Run Findbugs on the entire project
        work(createFindBugsWorker());

        // Get the content provider
        ITreeContentProvider contentProvider = getNavigatorContentProvider();
        assertNotNull(contentProvider);
        setGroupingInBugContentProvider(getDefaultGrouping());

        // Assert expected view contents
        Object projectBugGroup = getSingleElement(contentProvider);
        ExpectedViewElement expectedProject = createProjectPatternMarkerExpectedContents();
        expectedProject.assertEquals(projectBugGroup, contentProvider);

        // Run Findbugs on the first source folder
        work(createFindBugsWorker(), getDefaultPackageInSrc());

        // Assert expected view contents
        projectBugGroup = getSingleElement(contentProvider);
        expectedProject = createProjectPatternMarkerExpectedContents();
        expectedProject.assertEquals(projectBugGroup, contentProvider);
    }

    @Override
    protected TestScenario getTestScenario() {
        return TestScenario.TWO_SRC_FOLDERS;
    }

    private ExpectedViewElement createProjectPatternMarkerExpectedContents() {
        // Creates the expected elements tree
        // Project
        // -- Pattern
        // ---- Marker
        // -- Pattern
        // ---- Marker
        // -- Pattern
        // ---- Marker
        ExpectedViewElement expectedMarker1 = new ExpectedViewMarker("DM_STRING_CTOR");
        ExpectedViewElement expectedMarker2 = new ExpectedViewMarker("URF_UNREAD_FIELD");
        ExpectedViewElement expectedMarker3 = new ExpectedViewMarker("DM_NUMBER_CTOR");
        ExpectedViewElement expectedPattern1 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedMarker1),
                setOf(expectedMarker1));
        ExpectedViewElement expectedPattern2 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedMarker2),
                setOf(expectedMarker2));
        ExpectedViewElement expectedPattern3 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedMarker3),
                setOf(expectedMarker3));
        ExpectedViewElement expectedProject = new ExpectedViewBugGroup(GroupType.Project, getProject(), setOf(expectedPattern1,
                expectedPattern2, expectedPattern3), setOf(expectedMarker1, expectedMarker2, expectedMarker3));
        return expectedProject;
    }
}
