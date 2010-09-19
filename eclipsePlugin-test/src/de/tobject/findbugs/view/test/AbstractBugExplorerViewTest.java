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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.PartInitException;
import org.junit.After;
import org.junit.Before;

import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

/**
 * This is an abstract class for tests that interact with the BugExplorerView.
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractBugExplorerViewTest extends AbstractFindBugsTest {

    protected static <T> Set<T> setOf(T... a) {
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

    protected Grouping getDefaultGrouping() {
        List<GroupType> types = new ArrayList<GroupType>();
        types.add(GroupType.Project);
        types.add(GroupType.Pattern);
        types.add(GroupType.Marker);
        Grouping grouping = Grouping.createFrom(types);
        return grouping;
    }

    protected ITreeContentProvider getNavigatorContentProvider() throws PartInitException {
        BugExplorerView view = (BugExplorerView) showBugExplorerView();
        assertNotNull(view);

        ITreeContentProvider contentProvider = (ITreeContentProvider) view.getCommonViewer().getContentProvider();
        return contentProvider;
    }

    protected Grouping getProjectPatternPackageMarkerGrouping() {
        List<GroupType> types = new ArrayList<GroupType>();
        types.add(GroupType.Project);
        types.add(GroupType.Pattern);
        types.add(GroupType.Package);
        types.add(GroupType.Marker);
        Grouping grouping = Grouping.createFrom(types);
        return grouping;
    }

    protected Object getSingleElement(ITreeContentProvider contentProvider) {
        Object[] elements = contentProvider.getElements(getWorkspaceRoot());
        assertNotNull(elements);
        assertEquals(1, elements.length);
        return elements[0];
    }

    protected void resetBugContentProviderInput() throws PartInitException {
        BugContentProvider bugContentProvider = getBugContentProvider();
        bugContentProvider.reSetInput();
    }

    protected void setGroupingInBugContentProvider(Grouping grouping) throws PartInitException {
        BugContentProvider bugContentProvider = getBugContentProvider();
        assertNotNull(bugContentProvider);
        bugContentProvider.setGrouping(grouping);
        bugContentProvider.reSetInput();
    }

}
