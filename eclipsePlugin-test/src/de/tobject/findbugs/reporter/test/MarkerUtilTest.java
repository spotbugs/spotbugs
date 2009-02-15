package de.tobject.findbugs.reporter.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
		assertTrue(MarkerUtil.isFiltered(marker, new HashSet(Arrays.asList(
				"EI_EXPOSE_REP", "EI_EXPOSE_REP2"))));
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
