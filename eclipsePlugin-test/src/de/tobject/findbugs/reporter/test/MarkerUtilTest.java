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

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.test.AbstractFindBugsTest;

/**
 * This class tests the MarkerUtil class.
 * 
 * @author Tomás Pollak
 */
public class MarkerUtilTest extends AbstractFindBugsTest {
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
		Set<IMarker> markers = MarkerUtil.getMarkerFromSelection(new StructuredSelection(
				getProject()));
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
		assertFalse(MarkerUtil.isFiltered(marker, Collections.EMPTY_SET));
	}

	@Test
	public void testIsFiltered_NullFilters() throws CoreException {
		// Load bugs from a file
		loadXml(createFindBugsWorker(), getBugsFileLocation());

		IMarker marker = getAnyMarker();
		assertFalse(MarkerUtil.isFiltered(marker, null));
	}

	@Test
	public void testIsFiltered_NullMarker() throws CoreException {
		assertTrue(MarkerUtil.isFiltered(null, Collections.EMPTY_SET));
	}

	@Test
	public void testRemoveCreateMarkers() throws CoreException {
		// Setup the initial state, load bugs from a file
		loadXml(createFindBugsWorker(), getBugsFileLocation());
		assertExpectedMarkers(MarkerUtil.getAllMarkers(getProject()));

		// Remove the markers
		MarkerUtil.removeMarkers(getProject());
		assertNoMarkers(MarkerUtil.getAllMarkers(getProject()));

		// Create the markers again from the BugCollection
		IProgressMonitor monitor = new NullProgressMonitor();
		MarkerUtil.createMarkers(getJavaProject(), FindbugsPlugin.getBugCollection(
				getProject(), monitor), monitor);
		assertExpectedMarkers(MarkerUtil.getAllMarkers(getProject()));
	}

	private IMarker getAnyMarker() {
		IMarker[] markers = MarkerUtil.getAllMarkers(getProject());
		IMarker marker = markers[0];
		return marker;
	}
}
