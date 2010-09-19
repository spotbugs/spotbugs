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

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.test.TestScenario;
import de.tobject.findbugs.view.explorer.GroupType;

/**
 * This class tests the BugExplorerView and its related classes.
 * 
 * @author Tomás Pollak
 */
public class BugExplorerViewTest extends AbstractBugExplorerViewTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testShowView() throws PartInitException {
        // Show the view
        IViewPart view = showBugExplorerView();
        assertNotNull(view);
    }

    @Test
    public void testViewContents_Empty() throws PartInitException {
        // Get the content provider
        ITreeContentProvider contentProvider = getNavigatorContentProvider();
        assertNotNull(contentProvider);

        // Get the top level elements from the content provider
        Object[] projects = contentProvider.getElements(getWorkspaceRoot());
        assertNotNull(projects);
        assertEquals(0, projects.length);
    }

    @Test
    public void testViewContents_Project_Pattern_Marker() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the content provider
        ITreeContentProvider contentProvider = getNavigatorContentProvider();
        assertNotNull(contentProvider);
        setGroupingInBugContentProvider(getDefaultGrouping());

        // Assert expected view contents
        Object projectBugGroup = getSingleElement(contentProvider);
        ExpectedViewElement expectedProject = createProjectPatternMarkerExpectedContents();
        expectedProject.assertEquals(projectBugGroup, contentProvider);
    }

    @Test
    public void testViewContents_Project_Pattern_Package_Marker() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the content provider
        ITreeContentProvider contentProvider = getNavigatorContentProvider();
        assertNotNull(contentProvider);
        setGroupingInBugContentProvider(getProjectPatternPackageMarkerGrouping());

        // Assert expected view contents
        Object projectBugGroup = getSingleElement(contentProvider);
        ExpectedViewElement expectedProject = createProjectPatternPackageMarkerExpectedContents();
        expectedProject.assertEquals(projectBugGroup, contentProvider);
    }

    private ExpectedViewElement createProjectPatternMarkerExpectedContents() {
        // Creates the expected elements tree
        // Project
        // -- Pattern
        // ---- Marker
        // -- Pattern
        // ---- Marker
        ExpectedViewElement expectedMarker1 = new ExpectedViewMarker("DM_STRING_CTOR");
        ExpectedViewElement expectedMarker2 = new ExpectedViewMarker("URF_UNREAD_FIELD");
        ExpectedViewElement expectedPattern1 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedMarker1),
                setOf(expectedMarker1));
        ExpectedViewElement expectedPattern2 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedMarker2),
                setOf(expectedMarker2));
        ExpectedViewElement expectedProject = new ExpectedViewBugGroup(GroupType.Project, getProject(), setOf(expectedPattern1,
                expectedPattern2), setOf(expectedMarker1, expectedMarker2));
        return expectedProject;
    }

    private ExpectedViewElement createProjectPatternPackageMarkerExpectedContents() throws JavaModelException {
        // Creates the expected elements tree
        // Project
        // -- Pattern
        // ---- Package
        // ------ Marker
        // -- Pattern
        // ---- Package
        // ------ Marker
        ExpectedViewElement expectedMarker1 = new ExpectedViewMarker("DM_STRING_CTOR");
        ExpectedViewElement expectedMarker2 = new ExpectedViewMarker("URF_UNREAD_FIELD");
        ExpectedViewElement expectedPackage1 = new ExpectedViewBugGroup(GroupType.Package, getDefaultPackageInSrc(),
                setOf(expectedMarker1), setOf(expectedMarker1));
        ExpectedViewElement expectedPackage2 = new ExpectedViewBugGroup(GroupType.Package, getDefaultPackageInSrc(),
                setOf(expectedMarker2), setOf(expectedMarker2));
        ExpectedViewElement expectedPattern1 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedPackage1),
                setOf(expectedMarker1));
        ExpectedViewElement expectedPattern2 = new ExpectedViewBugGroup(GroupType.Pattern, null, setOf(expectedPackage2),
                setOf(expectedMarker2));
        ExpectedViewElement expectedProject = new ExpectedViewBugGroup(GroupType.Project, getProject(), setOf(expectedPattern1,
                expectedPattern2), setOf(expectedMarker1, expectedMarker2));
        return expectedProject;
    }
}
