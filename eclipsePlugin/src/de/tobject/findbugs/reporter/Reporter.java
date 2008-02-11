/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004-2006, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.reporter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * The <code>Reporter</code> is a class that is called by the FindBugs engine
 * in order to record and report bugs that have been found. This implementation
 * displays the bugs found as tasks in the task view.
 *
 * @author Peter Friese
 * @author David Hovemeyer
 * @version 1.0
 * @since 28.07.2003
 */
public class Reporter extends AbstractBugReporter  implements FindBugsProgress {

	/** Controls debugging for the reporter */
	public static boolean DEBUG;

	private final IJavaProject project;

	private final IProgressMonitor monitor;

	/** Persistent store of reported warnings. */
	private final SortedBugCollection bugCollection;

	/** Current user preferences for the project. */
	private UserPreferences userPrefs;

	private int pass = -1;

	private int filteredBugCount;

	/**
	 * Constructor.
	 *
	 * @param project         the project whose classes are being analyzed for bugs
	 * @param monitor         progress monitor
	 */
	public Reporter(IJavaProject project, IProgressMonitor monitor) {
		this.monitor = monitor;
		this.project = project;
		this.bugCollection = new SortedBugCollection();
		try {
			this.userPrefs = FindbugsPlugin.getUserPreferences(project.getProject());
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Error getting FindBugs preferences for project");
			this.userPrefs = UserPreferences.createDefaultUserPreferences();
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#doReportBug(edu.umd.cs.findbugs.BugInstance)
	 */
	@Override
	protected void doReportBug(BugInstance bug) {
		getBugCollection().add(bug);
		if (MarkerUtil.displayWarning(bug, userPrefs.getFilterSettings())) {
			MarkerUtil.createMarker(bug, project, getBugCollection());
			filteredBugCount++;
		}
	}

	@Override
	public ProjectStats getProjectStats() {
		return bugCollection.getProjectStats();
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugReporter#finish()
	 */
	public void finish() {
		if (DEBUG) {
			System.out.println("Finish: Found " + filteredBugCount + " bugs."); //$NON-NLS-1$//$NON-NLS-2$
		}
		monitor.done();
	}

	/**
	 * Returns the list of bugs found in this project. If the list has not been
	 * initialized yet, this will be done before returning.
	 *
	 * @return The collection that hold the bugs found in this project.
	 */
	public SortedBugCollection getBugCollection() {
		return bugCollection;
	}

	public void observeClass(ClassDescriptor classDescriptor) {
		if (monitor.isCanceled()) {
			// causes break in FindBugs main loop
			Thread.currentThread().interrupt();
		}

		int work = pass == 0 ? 1 : 2;
		String className = classDescriptor.getDottedClassName();

		if (DEBUG) {
			System.out.println("Observing class: " + className); //$NON-NLS-1$
		}

		// Update progress monitor
		if (pass <= 0) {
			monitor.setTaskName("Prescanning... (found " + filteredBugCount
					+ ", checking " + className + ")");
		} else {
			monitor.setTaskName("Checking... (pass #" + pass + ") (found "
					+ filteredBugCount + ", checking " + className + ")");
		}
		monitor.worked(work);
	}

	@Override
	public void reportAnalysisError(AnalysisError error) {
		FindbugsPlugin.getDefault().logException(error.getException(),
				"FindBugs analysis error: " + error.getMessage());
	}

	@Override
	public void reportMissingClass(String missingClass) {
		FindbugsPlugin.getDefault().logWarning(
				"FindBugs could not find a class that would be useful in analyzing your code: "
						+ missingClass);
	}

	public BugReporter getRealBugReporter() {
		return this;
	}

	public void finishArchive() {
		// noop
	}

	public void finishClass() {
		// noop
	}

	public void finishPerClassAnalysis() {
		// noop
	}

	public void reportNumberOfArchives(int numArchives) {
		// noop
	}

	public void startAnalysis(int numClasses) {
		pass++;
	}

	public void predictPassCount(int[] classesPerPass) {
		int expectedWork = 0;
		for(int count : classesPerPass) {
			expectedWork += 2*count;
		}
		expectedWork -= classesPerPass[0];
		if (!(monitor instanceof SubProgressMonitor)) {
			monitor.beginTask("Performing bug checking...", expectedWork);
		}
	}
}
