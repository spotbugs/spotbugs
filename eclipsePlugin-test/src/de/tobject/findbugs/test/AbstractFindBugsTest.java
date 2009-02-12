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

import static de.tobject.findbugs.FindbugsPlugin.*;
import static de.tobject.findbugs.reporter.MarkerUtil.*;
import static org.eclipse.core.runtime.jobs.Job.*;
import static org.eclipse.jdt.testplugin.JavaProjectHelper.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.After;
import org.junit.Before;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.FindbugsTestPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Base class for FindBugs tests.
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractFindBugsTest {

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
		project = createJavaProject("TestProject", "bin");
		addRTJar(getJavaProject());
		addSourceContainer(getJavaProject(), "src");

		// Copy test workspace
		importResources(getProject().getFolder("src"), FindbugsTestPlugin.getDefault()
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
		removeMarkers(getProject());

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
		SortedBugCollection bugs = getBugCollection(project, null);
		assertEquals(expected, bugs.getCollection().size());
	}

	protected void assertExpectedBugs() throws CoreException {
		assertBugsCount(2, getProject());
		assertReportedBugs("EI_EXPOSE_REP", 1, getProject());
		assertReportedBugs("EI_EXPOSE_REP2", 1, getProject());
	}

	protected void assertNoBugs() throws CoreException {
		assertBugsCount(0, getProject());
	}

	/**
	 * Asserts that there are number of detected bugs of the given type match the given
	 * expected count.
	 * 
	 * @param expectedBugType
	 *            The expected bug type.
	 * @param expectedBugCount
	 *            The expected bug type.
	 * @param project
	 *            The IProject that contains the bugs.
	 * @throws CoreException
	 */
	protected void assertReportedBugs(String expectedBugType, int expectedBugCount,
			IProject project) throws CoreException {
		int seenBugCount = 0;
		SortedBugCollection bugs = getBugCollection(project, null);
		for (Iterator<BugInstance> i = bugs.iterator(); i.hasNext();) {
			BugInstance bug = i.next();
			if (expectedBugType.equals(bug.getType())) {
				seenBugCount++;
			}
		}
		assertEquals("Expected " + expectedBugCount + " of bugs " + expectedBugType
				+ " but seen " + seenBugCount, expectedBugCount, seenBugCount);
	}

	protected void clearBugsState() throws CoreException {
		removeMarkers(getProject());
		getBugCollection(getProject(), null).clearBugInstances();
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
		String bugsFileLocation = bugsFile.getLocation().toOSString();
		return bugsFileLocation;
	}

	/**
	 * Returns the filter file path of the test project.
	 * 
	 * @return The absolute path (relative to the workspace root) of the filter file.
	 */
	protected String getFilterFileLocation() {
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

	protected void loadXml(FindBugsWorker worker, String fileName) throws CoreException {
		worker.loadXml(fileName);
	}

	/**
	 * Configures the test project to use the filter file.
	 */
	protected void setFilterFile() throws CoreException, IOException {
		UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject(),
				true);
		preferences.setExcludeFilterFiles(Collections
				.singletonList(getFilterFileLocation()));
		FindbugsPlugin.saveUserPreferences(getProject(), preferences);
	}

	/**
	 * Runs the FindBugs worker on the test project.
	 */
	protected void work(FindBugsWorker worker) throws CoreException {
		worker.work(Collections.singletonList((IResource) getProject()));
	}
}