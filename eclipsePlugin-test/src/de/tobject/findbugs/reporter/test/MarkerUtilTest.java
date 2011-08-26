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
package de.tobject.findbugs.reporter.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the MarkerUtil class.
 *
 * @author Tomás Pollak
 */
public class MarkerUtilTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    public void testGetAllMarkers() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers
        IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
        assertExpectedMarkers(markers);
    }

    @Test
    public void testGetAllMarkers_Empty() {
        // Get the markers for an empty project
        IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
        assertNoMarkers(markers);
    }

    @Test
    public void testGetMarkersFromSelection() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers from a project selection
        Set<IMarker> markers = MarkerUtil.getMarkerFromSelection(new StructuredSelection(getProject()));
        assertExpectedMarkers(markers);
    }

    @Test
    public void testIsFiltered() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers
        IMarker marker = getAnyMarker();
        assertTrue(MarkerUtil.isFiltered(marker, getExpectedBugPatterns()));
    }

    @Test
    public void testIsFiltered_EmptyFilters() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        IMarker marker = getAnyMarker();
        assertFalse(MarkerUtil.isFiltered(marker, Collections.<String> emptySet()));
    }

    @Test
    public void testIsFiltered_NullFilters() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        IMarker marker = getAnyMarker();
        assertFalse(MarkerUtil.isFiltered(marker, null));
    }

    @Test
    public void testIsFiltered_NullMarker()  {
        assertTrue(MarkerUtil.isFiltered(null, Collections.<String> emptySet()));
    }

    @Test
    public void testRemoveCreateMarkers() throws CoreException {
        // Setup the initial state, load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());
        assertExpectedMarkers(MarkerUtil.getAllMarkers(getProject()));
        assertBugsCount(getExpectedBugsCount(), getProject());

        // Remove the markers
        MarkerUtil.removeMarkers(getProject());

        // Project should be markers - free now, bug collection is empty
        assertNoMarkers(MarkerUtil.getAllMarkers(getProject()));
        assertNoBugs();
    }

    private IMarker getAnyMarker() {
        IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
        IMarker marker = markers[0];
        return marker;
    }
}
