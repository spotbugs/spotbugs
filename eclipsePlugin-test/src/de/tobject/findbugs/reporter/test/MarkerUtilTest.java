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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the MarkerUtil class.
 *
 * @author Tomás Pollak
 */
class MarkerUtilTest extends AbstractFindBugsTest {

    @BeforeAll
    static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterAll
    static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    @Test
    void testGetAllMarkers() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers
        IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
        assertExpectedMarkers(markers);
    }

    @Test
    void testGetAllMarkers_Empty() {
        // Get the markers for an empty project
        IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
        assertNoMarkers(markers);
    }

    @Test
    void testGetMarkersFromSelection() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers from a project selection
        Set<IMarker> markers = MarkerUtil.getMarkerFromSelection(new StructuredSelection(getProject()));
        assertExpectedMarkers(markers);
    }

    @Test
    void testIsFiltered() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        // Get the markers
        IMarker marker = getAnyMarker();
        assertTrue(MarkerUtil.isFiltered(marker, getExpectedBugPatterns()));
    }

    @Test
    void testIsFiltered_EmptyFilters() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        IMarker marker = getAnyMarker();
        assertFalse(MarkerUtil.isFiltered(marker, Collections.<String>emptySet()));
    }

    @Test
    void testIsFiltered_NullFilters() throws CoreException {
        // Load bugs from a file
        loadXml(createFindBugsWorker(), getBugsFileLocation());

        IMarker marker = getAnyMarker();
        assertFalse(MarkerUtil.isFiltered(marker, null));
    }

    @Test
    void testIsFiltered_NullMarker() {
        assertTrue(MarkerUtil.isFiltered(null, Collections.<String>emptySet()));
    }

    @Test
    void testRemoveCreateMarkers() throws CoreException {
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
        return markers[0];
    }
}
