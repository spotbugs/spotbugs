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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
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
		super();
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
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#reportQueuedErrors()
	 */
	@Override
	public void reportQueuedErrors() {
		// Report unique errors in order of their sequence
		List<Error> errorList = new ArrayList<Error>(getQueuedErrors());
		if(errorList.size() > 0) {
			Collections.sort(errorList, new Comparator<Error>() {
				public int compare(Error o1, Error o2) {
					return o1.getSequence() - o2.getSequence();
				}
			});

			MultiStatus status = new MultiStatus(FindbugsPlugin.PLUGIN_ID, IStatus.ERROR,
					"The following errors occurred during FindBugs analysis:", null);

			for (Error error : errorList) {
				status.add(new Status(IStatus.ERROR, FindbugsPlugin.PLUGIN_ID, error
						.getMessage(), error.getCause()));
			}
			FindbugsPlugin.getDefault().getLog().log(status);
		}

		Set<String> missingClasses = getMissingClasses();
		if(missingClasses.size() > 0) {
			MultiStatus status = new MultiStatus(FindbugsPlugin.PLUGIN_ID, IStatus.WARNING,
					"The following classes needed for FindBugs analysis were missing:", null);
			for (String missingClass : missingClasses) {
				status.add(new Status(IStatus.WARNING, FindbugsPlugin.PLUGIN_ID, missingClass));
			}
			FindbugsPlugin.getDefault().getLog().log(status);
		}
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
		// nothing to do, see reportQueuedErrors()
	}

	@Override
	public void reportMissingClass(String missingClass) {
		// nothing to do, see reportQueuedErrors()
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
