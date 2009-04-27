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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

/**
 * This class tests the BugExplorerView and its related classes.
 *
 * @author Tomás Pollak
 */
public class BugExplorerViewTest extends AbstractFindBugsTest {

    private static <T> Set<T> setOf(T... a) {
        return new HashSet<T>(Arrays.asList(a));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        resetBugContentProviderInput();
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws CoreException {
        super.tearDown();
        resetBugContentProviderInput();
    }

    @Test
    public void testShowView() throws PartInitException {
        // Show the view
        IViewPart view = showBugExplorerView();
        assertNotNull(view);
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
        ExpectedViewElement expectedPattern1 = new ExpectedViewBugGroup(
                GroupType.Pattern, null, setOf(expectedMarker1), setOf(expectedMarker1));
        ExpectedViewElement expectedPattern2 = new ExpectedViewBugGroup(
                GroupType.Pattern, null, setOf(expectedMarker2), setOf(expectedMarker2));
        ExpectedViewElement expectedProject = new ExpectedViewBugGroup(GroupType.Project,
                getProject(), setOf(expectedPattern1, expectedPattern2), setOf(
                        expectedMarker1, expectedMarker2));
        return expectedProject;
    }

    private ExpectedViewElement createProjectPatternPackageMarkerExpectedContents()
            throws JavaModelException {
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
        ExpectedViewElement expectedPackage1 = new ExpectedViewBugGroup(
                GroupType.Package, getDefaultPackageInSrc(), setOf(expectedMarker1),
                setOf(expectedMarker1));
        ExpectedViewElement expectedPackage2 = new ExpectedViewBugGroup(
                GroupType.Package, getDefaultPackageInSrc(), setOf(expectedMarker2),
                setOf(expectedMarker2));
        ExpectedViewElement expectedPattern1 = new ExpectedViewBugGroup(
                GroupType.Pattern, null, setOf(expectedPackage1), setOf(expectedMarker1));
        ExpectedViewElement expectedPattern2 = new ExpectedViewBugGroup(
                GroupType.Pattern, null, setOf(expectedPackage2), setOf(expectedMarker2));
        ExpectedViewElement expectedProject = new ExpectedViewBugGroup(GroupType.Project,
                getProject(), setOf(expectedPattern1, expectedPattern2), setOf(
                        expectedMarker1, expectedMarker2));
        return expectedProject;
    }

    private ITreeContentProvider getNavigatorContentProvider() throws PartInitException {
        BugExplorerView view = (BugExplorerView) showBugExplorerView();
        assertNotNull(view);

        ITreeContentProvider contentProvider = (ITreeContentProvider) view
                .getCommonViewer().getContentProvider();
        return contentProvider;
    }

    private Object getSingleElement(ITreeContentProvider contentProvider) {
        Object[] elements = contentProvider.getElements(getWorkspaceRoot());
        assertNotNull(elements);
        assertEquals(1, elements.length);
        return elements[0];
    }

    private IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    private void resetBugContentProviderInput() throws PartInitException {
        BugContentProvider bugContentProvider = getBugContentProvider();
        bugContentProvider.reSetInput();
    }

	private void setGroupingInBugContentProvider(Grouping grouping)
			throws PartInitException {
        BugContentProvider bugContentProvider = getBugContentProvider();
        assertNotNull(bugContentProvider);
		bugContentProvider.setGrouping(grouping);
		bugContentProvider.reSetInput();
	}

	private Grouping getProjectPatternPackageMarkerGrouping() {
        List<GroupType> types = new ArrayList<GroupType>();
        types.add(GroupType.Project);
        types.add(GroupType.Pattern);
        types.add(GroupType.Package);
        types.add(GroupType.Marker);
		Grouping grouping = Grouping.createFrom(types);
		return grouping;
    }

	private Grouping getDefaultGrouping() {
		List<GroupType> types = new ArrayList<GroupType>();
		types.add(GroupType.Project);
		types.add(GroupType.Pattern);
		types.add(GroupType.Marker);
		Grouping grouping = Grouping.createFrom(types);
		return grouping;
    }
}
