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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.junit.Assert;

import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;

/**
 * Expected object for a bug group.
 * 
 * @author Tomás Pollak
 */
public class ExpectedViewBugGroup implements ExpectedViewElement {

    private final GroupType groupType;

    private final Object data;

    private final Set<ExpectedViewElement> children;

    private final Set<ExpectedViewElement> markers;

    public ExpectedViewBugGroup(GroupType groupType, Object data, Set<ExpectedViewElement> children,
            Set<ExpectedViewElement> markers) {
        this.groupType = groupType;
        this.data = data;
        this.children = children;
        this.markers = markers;
    }

    public void assertEquals(Object actual, ITreeContentProvider contentProvider) throws CoreException {
        Assert.assertTrue(actual instanceof BugGroup);
        BugGroup bugGroup = (BugGroup) actual;
        Assert.assertEquals(groupType, bugGroup.getType());
        Assert.assertEquals(markers.size(), bugGroup.getMarkersCount());
        if (data != null) {
            Assert.assertEquals(data, bugGroup.getData());
        }

        // Assert on the children
        assertChildren(contentProvider, bugGroup);
    }

    public boolean matches(Object actual) throws CoreException {
        if (actual instanceof BugGroup) {
            BugGroup bugGroup = (BugGroup) actual;
            return doAllExpectedMarkerMatch(bugGroup.getAllMarkers());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Expected View Bug Group: " + groupType.toString();
    }

    private void assertChildren(ITreeContentProvider contentProvider, BugGroup bugGroup) throws CoreException {
        // Get the actual children
        Object[] bugGroupChildren = contentProvider.getChildren(bugGroup);
        Assert.assertEquals(children.size(), bugGroupChildren.length);

        // For each expected child, find a match and assert on it
        for (Iterator<ExpectedViewElement> iChildren = children.iterator(); iChildren.hasNext();) {
            ExpectedViewElement expectedChild = iChildren.next();
            Object actualChild = findActualObjectMatch(expectedChild, bugGroupChildren);
            expectedChild.assertEquals(actualChild, contentProvider);
        }
    }

    private boolean doAllExpectedMarkerMatch(Set<IMarker> actualMarkers) throws CoreException {
        boolean result = true;
        for (Iterator<IMarker> iMarkers = actualMarkers.iterator(); iMarkers.hasNext();) {
            IMarker actualMarker = iMarkers.next();
            result &= doesAnyExpectedMarkerMatch(actualMarker);
        }
        return result;
    }

    private boolean doesAnyExpectedMarkerMatch(IMarker actualMarker) throws CoreException {
        boolean result = false;
        for (Iterator<ExpectedViewElement> iterator = markers.iterator(); iterator.hasNext();) {
            ExpectedViewElement expectedMarker = iterator.next();
            result |= expectedMarker.matches(actualMarker);
        }
        return result;
    }

    private Object findActualObjectMatch(ExpectedViewElement child, Object[] bugGroupChildren) throws CoreException {
        for (int i = 0; i < bugGroupChildren.length; i++) {
            if (child.matches(bugGroupChildren[i])) {
                return bugGroupChildren[i];
            }
        }
        Assert.fail("No match found for: " + child + " in " + Arrays.toString(bugGroupChildren));
        return null;
    }
}
