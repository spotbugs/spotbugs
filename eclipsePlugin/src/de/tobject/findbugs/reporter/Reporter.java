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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.ba.AnalysisContext;
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
	public static boolean DEBUG = false;

	private static final int MAX_CLASS_NAME_LENGTH = 35;
	
	private IProject project;
	private Project findBugsProject;
	
	/** determines how often the progress monitor gets updated */
	private static int MONITOR_INTERVAL = 1;
	private IProgressMonitor monitor;

	/** Persistent store of reported warnings. */
	private SortedBugCollection bugCollection;
	
	/** Set of names of analyzed classes. */
	private Set<String> analyzedClassNameSet;
	
	/** Current user preferences for the project. */
	private UserPreferences userPrefs;
	
	private boolean workStarted;
	int filesNumber;
	int observed = 0;
	int [] classesPerPass;
	int expectedWork;
	int pass = -1;
	int filteredBugCount = 0;
	int workCount = 0;
	/**
	 * Constructor.
	 * 
	 * @param project         the project whose classes are being analyzed for bugs
	 * @param monitor         progress monitor
	 * @param findBugsProject the FindBugs Project object
	 */
	public Reporter(IProject project, IProgressMonitor monitor, Project findBugsProject) {
		super();
		this.monitor = monitor;
		this.project = project;
		this.findBugsProject = findBugsProject;
		this.project = project;
		this.bugCollection = new SortedBugCollection();
		this.analyzedClassNameSet = new HashSet<String>();
		try {
			this.userPrefs = FindbugsPlugin.getUserPreferences(project);
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
			MarkerUtil.createMarker(bug, project);
			filteredBugCount++;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugReporter#finish()
	 */
	public void finish() {
		if (DEBUG) {
			System.out.println("Finish: Found " + filteredBugCount + " bugs."); //$NON-NLS-1$//$NON-NLS-2$
		}
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
	
	/**
	 * Returns the current project.
	 * 
	 * @return The current project.
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * Get set containing full names of analyzed classes.
	 * 
	 * @return Set containing names of all analyzed classes
	 */
	public Set<String> getAnalyzedClassNames() {
		return analyzedClassNameSet;
	}
	
	/**
	 * Returns the current project cast into a Java project.
	 * 
	 * @return The current project as a Java project.
	 */
	public static IJavaProject getJavaProject(IProject project) {
		return JavaCore.create(project);
	}

	public void observeClass(ClassDescriptor classDescriptor) {
		JavaClass clazz;
		try {
			clazz = AnalysisContext.currentAnalysisContext().lookupClass(classDescriptor);
		} catch (ClassNotFoundException e) {
			// This should not happen
			return;
		}
		
		if (DEBUG) {
			System.out.println("Observing class: " + clazz.getClassName()); //$NON-NLS-1$
		}
			// Keep track of classes analyzed
		analyzedClassNameSet.add(clazz.getClassName());
		if (filesNumber < analyzedClassNameSet.size())
			filesNumber = analyzedClassNameSet.size();
		
		// Update progress monitor
		if (monitor == null) {
			return;
		}
		if (!workStarted) {
			workStarted = true;
			filesNumber = findBugsProject.getFileCount();
			if (!(monitor instanceof SubProgressMonitor)) {
				monitor.beginTask("Performing bug checking...", expectedWork);
			}
		}
		if (monitor.isCanceled()) {
			// causes break in FindBugs main loop
			Thread.currentThread().interrupt();
		}
		
		if (pass <= 0)
			monitor.setTaskName(
					"Prescanning... (found "
					+ filteredBugCount
					+ ", checking "
					+ getAbbreviatedClassName(clazz)
					+ ")");
		else 	if (pass == 1)
			monitor.setTaskName(
					"Checking... (found "
					+ filteredBugCount
					+ ", checking "
					+ getAbbreviatedClassName(clazz)
					+ ")");
		else 	if (pass == 2)
			monitor.setTaskName(
					"Checking... (3rd pass) (found "
					+ filteredBugCount
					+ ", checking "
					+ getAbbreviatedClassName(clazz)
					+ ")");
	    int work = pass == 0 ? 1 : 2;
		monitor.worked( work);
		workCount+= work;
	}
	
	/**
	 * Returns an abreviated version of the class name.
	 * 
	 * @param clazz A Java class.
	 * @return
	 */
	private String getAbbreviatedClassName(JavaClass clazz) {
		String name = clazz.getClassName();
		if (name.length() > MAX_CLASS_NAME_LENGTH) {
			int startCutIdx = name.length() - MAX_CLASS_NAME_LENGTH;
			int pointIdx = name.indexOf(".", startCutIdx); //$NON-NLS-1$
			if (pointIdx > startCutIdx) {
				startCutIdx = pointIdx;
			}
			name = ".." + name.substring(startCutIdx); //$NON-NLS-1$
		}
		return name;
	}

	@Override
	public void reportAnalysisError(AnalysisError error) {
		FindbugsPlugin.getDefault().logWarning("FindBugs analysis error: " + error.getMessage());
	}

	@Override
	public void reportMissingClass(String missingClass) {
		FindbugsPlugin.getDefault().logWarning("FindBugs could not find a class that would be useful in analyzing your code: " + missingClass);
	}

	public BugReporter getRealBugReporter() {
		return this;
	}

	public void finishArchive() {
		// TODO Auto-generated method stub
		
	}

	public void finishClass() {
		// TODO Auto-generated method stub
		
	}

	public void finishPerClassAnalysis() {
		// TODO Auto-generated method stub
		
	}

	public void reportNumberOfArchives(int numArchives) {
		// TODO Auto-generated method stub
		
	}

	public void startAnalysis(int numClasses) {
		pass++;
		// TODO Auto-generated method stub
		
	}

	public void predictPassCount(int[] classesPerPass) {
		this.classesPerPass = classesPerPass;
		for(int count : classesPerPass) expectedWork += 2*count;
		expectedWork -= classesPerPass[0];
		
	}
}
