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
package de.tobject.findbugs.test;

import static org.eclipse.core.runtime.jobs.Job.*;
import static org.eclipse.jdt.testplugin.JavaProjectHelper.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.After;
import org.junit.Before;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.FindbugsTestPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Base class for FindBugs tests.
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractFindBugsTest {

	private static final String SRC = "src";
	private static final String TEST_PROJECT = "TestProject";
	protected static final String BUGS_XML_FILE = "/src/bugs.xml";
	protected static final String FILTER_FILE = "/src/filter.xml";
	private IJavaProject project;

	public AbstractFindBugsTest() {
		super();
	}

	/**
	 * Create a new Java project with a source folder and copy the test files of the
	 * plugin to the source folder. Compile the project.
	 * 
	 * @throws CoreException
	 * @throws IOException
	 */
	@Before
	public void setUp() throws CoreException, IOException {
		// Create the test project
		project = createJavaProject(TEST_PROJECT, "bin");
		addRTJar(getJavaProject());
		addSourceContainer(getJavaProject(), SRC);

		// Copy test workspace
		importResources(getProject().getFolder(SRC), FindbugsTestPlugin.getDefault()
				.getBundle(), "/testFiles");

		// Compile project
		getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		performDummySearch();

		// Start with a clean FindBugs state
		clearBugsState();
	}

	/**
	 * Delete the Java project used for this test.
	 * 
	 * @throws CoreException
	 */
	@After
	public void tearDown() throws CoreException {
		// Clean the FindBugs state
		clearBugsState();

		// Delete the test project
		delete(project);
	}

	/**
	 * Assert the total number of bugs in the given resource.
	 * 
	 * @param expected
	 *            The expected number of bugs.
	 * @param project
	 *            The IProject that contains the bugs.
	 * @throws CoreException
	 */
	protected void assertBugsCount(int expected, IProject project) throws CoreException {
		SortedBugCollection bugs = FindbugsPlugin.getBugCollection(project, null);
		assertEquals(expected, bugs.getCollection().size());
	}

	protected void assertExpectedBugs() throws CoreException {
		assertBugsCount(getExpectedBugsCount(), getProject());
		assertReportedBugs("EI_EXPOSE_REP", 1, getProject());
		assertReportedBugs("EI_EXPOSE_REP2", 1, getProject());
	}

	protected void assertExpectedMarkers(IMarker[] markers) throws CoreException {
		assertEquals(getExpectedBugsCount(), markers.length);
		for (int i = 0; i < markers.length; i++) {
			assertTrue(markers[i].isSubtypeOf(FindBugsMarker.NAME));
		}
		assertMarkers("EI_EXPOSE_REP", 1, markers);
		assertMarkers("EI_EXPOSE_REP2", 1, markers);
	}

	protected void assertExpectedMarkers(Set<IMarker> markers) throws CoreException {
		assertExpectedMarkers(markers.toArray(new IMarker[0]));
	}

	/**
	 * Asserts that the number of present markers of the given type match the given
	 * expected count.
	 * 
	 * @param expectedBugType
	 *            The expected bug type.
	 * @param expectedBugCount
	 *            The expected bug type count.
	 * @param markers
	 *            The array of markers to assert on.
	 * @throws CoreException
	 */
	protected void assertMarkers(String expectedBugType, int expectedBugTypeCount,
			IMarker[] markers) throws CoreException {
		int seenBugTypeCount = 0;
		for (int i = 0; i < markers.length; i++) {
			IMarker marker = markers[i];
			if (expectedBugType.equals(marker.getAttribute(FindBugsMarker.BUG_TYPE))) {
				seenBugTypeCount++;
			}
		}
		assertEquals("Expected " + expectedBugTypeCount + " of markers "
				+ expectedBugType + " but seen " + seenBugTypeCount,
				expectedBugTypeCount, seenBugTypeCount);
	}

	protected void assertNoBugs() throws CoreException {
		assertBugsCount(0, getProject());
	}

	protected void assertNoMarkers(IMarker[] markers) {
		assertEquals(0, markers.length);
	}

	/**
	 * Asserts that the number of detected bugs of the given type match the given expected
	 * count.
	 * 
	 * @param expectedBugType
	 *            The expected bug type.
	 * @param expectedBugCount
	 *            The expected bug type count.
	 * @param project
	 *            The IProject that contains the bugs.
	 * @throws CoreException
	 */
	protected void assertReportedBugs(String expectedBugType, int expectedBugCount,
			IProject project) throws CoreException {
		int seenBugCount = 0;
		SortedBugCollection bugs = FindbugsPlugin.getBugCollection(project, null);
		for (BugInstance bug : bugs) {
			if (expectedBugType.equals(bug.getType())) {
				seenBugCount++;
			}
		}
		assertEquals("Expected " + expectedBugCount + " of bugs " + expectedBugType
				+ " but seen " + seenBugCount, expectedBugCount, seenBugCount);
	}

	protected void clearBugsState() throws CoreException {
		MarkerUtil.removeMarkers(getProject());
		FindbugsPlugin.getBugCollection(getProject(), null).clearBugInstances();
	}

	protected FindBugsWorker createFindBugsWorker() throws CoreException {
		FindBugsWorker worker = new FindBugsWorker(getProject(),
				new NullProgressMonitor());
		return worker;
	}

	/**
	 * Returns the bug file path of the test project.
	 * 
	 * @return The absolute filesystem path of the bugs file.
	 */
	protected String getBugsFileLocation() {
		IResource bugsFile = getProject().findMember(BUGS_XML_FILE);
		return bugsFile.getLocation().toOSString();
	}

	/**
	 * Returns the bug file path of the test project.
	 * 
	 * @return The absolute path (relative to the workspace root) of the bugs file.
	 */
	protected String getBugsFilePath() {
		IResource bugsFile = getProject().findMember(BUGS_XML_FILE);
		return bugsFile.getFullPath().toPortableString();
	}

	protected IPackageFragment getDefaultPackageInSrc() throws JavaModelException {
		IPackageFragment fragment = getJavaProject().findPackageFragment(
				new Path("/" + TEST_PROJECT + "/" + SRC));
		return fragment;
	}

	protected Set<String> getExpectedBugPatterns() {
		return new HashSet<String>(Arrays.asList("EI_EXPOSE_REP", "EI_EXPOSE_REP2"));
	}

	protected int getExpectedBugsCount() {
		return 2;
	}

	/**
	 * Returns the filter file path of the test project.
	 * 
	 * @return The absolute path (relative to the workspace root) of the filter file.
	 */
	protected String getFilterFilePath() {
		IResource filterFile = getProject().findMember(FILTER_FILE);
		return filterFile.getFullPath().toPortableString();
	}

	/**
	 * Returns the Java project for this test.
	 * 
	 * @return An IJavaProject.
	 */
	protected IJavaProject getJavaProject() {
		return project;
	}

	/**
	 * Returns the project for this test.
	 * 
	 * @return An IProject.
	 */
	protected IProject getProject() {
		return getJavaProject().getProject();
	}

	/**
	 * Suspend the calling thread until all the background jobs belonging to the given
	 * family are done.
	 * 
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(Object)
	 * 
	 * @param family
	 *            The family object that groups the jobs.
	 */
	protected void joinJobFamily(Object family) {
		try {
			getJobManager().join(family, null);
		} catch (OperationCanceledException e) {
			// continue
		} catch (InterruptedException e) {
			// continue
		}
	}

	protected void loadXml(FindBugsWorker worker, String bugsFileLocation)
			throws CoreException {
		worker.loadXml(bugsFileLocation);
	}

	/**
	 * Configures the test project to use the baseline bugs file.
	 */
	protected void setBaselineBugsFile() throws CoreException, IOException {
		UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
		preferences.setExcludeBugsFiles(Collections.singletonList(getBugsFilePath()));
		FindbugsPlugin.saveUserPreferences(getProject(), preferences);
	}

	/**
	 * Configures the test project to use the filter file.
	 */
	protected void setFilterFile() throws CoreException, IOException {
		UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
		preferences.setExcludeFilterFiles(Collections.singletonList(getFilterFilePath()));
		FindbugsPlugin.saveUserPreferences(getProject(), preferences);
	}

	/**
	 * Runs the FindBugs worker on the test project.
	 */
	protected void work(FindBugsWorker worker) throws CoreException {
		worker.work(Collections.singletonList((IResource) getProject()));
	}
}