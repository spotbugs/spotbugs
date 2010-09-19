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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.junit.Assert;

import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * Expected object for a FindBugs marker.
 * 
 * @author Tomás Pollak
 */
public class ExpectedViewMarker implements ExpectedViewElement {

    private final String expectedBugType;

    public ExpectedViewMarker(String expectedBugType) {
        this.expectedBugType = expectedBugType;
    }

    public void assertEquals(Object actual, ITreeContentProvider contentProvider) throws CoreException {
        Assert.assertTrue(actual instanceof IMarker);
        IMarker marker = (IMarker) actual;
        String actualBugType = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
        Assert.assertEquals(expectedBugType, actualBugType);
    }

    public boolean matches(Object actual) throws CoreException {
        if (actual instanceof IMarker) {
            IMarker marker = (IMarker) actual;
            String actualBugType = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
            return expectedBugType.equals(actualBugType);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Expected View Marker: " + expectedBugType;
    }
}
