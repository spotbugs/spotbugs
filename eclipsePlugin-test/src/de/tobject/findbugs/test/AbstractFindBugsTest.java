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
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Base class for FindBugs UI tests (except the quickfix tests).
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractFindBugsTest extends AbstractPluginTest {

	protected static final String BUG_EXPLORER_VIEW_ID = "de.tobject.findbugs.view.bugtreeview";
	protected static final String BUGS_XML_FILE = "/src/bugs.xml";
	protected static final String FILTER_FILE = "/src/filter.xml";

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
		assertReportedBugs("URF_UNREAD_FIELD", 1, getProject());
		assertReportedBugs("DM_STRING_CTOR", 1, getProject());
	}

	protected void assertExpectedMarkers(IMarker[] markers) throws CoreException {
		assertEquals(getVisibleBugsCount(), markers.length);
		for (int i = 0; i < markers.length; i++) {
			assertTrue(markers[i].isSubtypeOf(FindBugsMarker.NAME));
		}
		assertMarkers("URF_UNREAD_FIELD", 1, markers);
		assertMarkers("DM_STRING_CTOR", 1, markers);
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

	protected BugContentProvider getBugContentProvider() throws PartInitException {
		BugExplorerView navigator = (BugExplorerView) showBugExplorerView();
		BugContentProvider bugContentProvider = BugContentProvider.getProvider(navigator
				.getNavigatorContentService());
		return bugContentProvider;
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

	protected ICompilationUnit getClassA() throws JavaModelException {
		ICompilationUnit compilationUnit = (ICompilationUnit) getJavaProject()
				.findElement(new Path("A.java"));
		return compilationUnit;
	}

	protected ICompilationUnit getClassB() throws JavaModelException {
		ICompilationUnit compilationUnit = (ICompilationUnit) getJavaProject()
				.findElement(new Path("B.java"));
		return compilationUnit;
	}

	protected IPackageFragment getDefaultPackageInSrc() throws JavaModelException {
		IPackageFragment fragment = getJavaProject().findPackageFragment(
				new Path("/" + TEST_PROJECT + "/" + SRC));
		return fragment;
	}

	protected Set<String> getExpectedBugPatterns() {
		return new HashSet<String>(Arrays.asList("URF_UNREAD_FIELD", "DM_STRING_CTOR"));
	}

	protected int getExpectedBugsCount() {
		return getFilteredBugsCount() + getVisibleBugsCount();
	}

	protected int getFilteredBugsCount() {
		return 2;
	}

	/**
	 * Returns the filter file path of the test project.
	 * 
	 * @return The absolute path of the filter file.
	 */
	protected String getFilterFileLocation() {
		IResource filterFile = getProject().findMember(FILTER_FILE);
		return filterFile.getLocation().toOSString();
	}

	@Override
	protected String getTestFilesPath() {
		return "/testFiles";
	}

	protected int getVisibleBugsCount() {
		return 2;
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
		boolean finished = false;
		while (!finished) {
			try {
				getJobManager().join(family, null);
				finished = true;
			} catch (InterruptedException e) {
				// continue waiting
			}
		}
	}

	protected void loadXml(FindBugsWorker worker, String bugsFileLocation)
			throws CoreException {
		worker.loadXml(bugsFileLocation);
	}

	/**
	 * Configures the test project to use the baseline bugs file.
	 */
	protected void setBaselineBugsFile() throws CoreException {
		// per default, workspace settings are used. We enable project settings here
		FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
		UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
		preferences.setExcludeBugsFiles(Collections.singletonList(getBugsFileLocation()));
		FindbugsPlugin.saveUserPreferences(getProject(), preferences);
	}

	/**
	 * Configures the test project to use the filter file.
	 */
	protected void setFilterFile() throws CoreException {
		// per default, workspace settings are used. We enable project settings here
		FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
		UserPreferences preferences = FindbugsPlugin.getUserPreferences(getProject());
		preferences.setExcludeFilterFiles(Collections
				.singletonList(getFilterFileLocation()));
		FindbugsPlugin.saveUserPreferences(getProject(), preferences);
	}

	protected IViewPart showBugExplorerView() throws PartInitException {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.showView(BUG_EXPLORER_VIEW_ID);
	}
}
