/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2005, University of Maryland
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

package de.tobject.findbugs.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Execute FindBugs on a collection of Java resources in a project.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 26.09.2003
 */
public class FindBugsWorker {
	private static final boolean INCREMENTAL_UPDATE = false;
	
	/** Controls debugging. */
	public static boolean DEBUG;

	private IProgressMonitor monitor;
	private UserPreferences userPrefs;
	private IProject project;

	/**
	 * Creates a new worker.
	 * 
	 * @param project The project to work on.
	 * @param monitor A progress monitor.
	 */
	public FindBugsWorker(IProject project, IProgressMonitor monitor) {
		super();
		this.project = project;
		this.monitor = monitor;
		try {
			this.userPrefs = FindbugsPlugin.getUserPreferences(project);
		}
		catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get selected detectors for project");
		}
		
	}

	/**
	 * Run FindBugs on the given collection of files.
	 * 
	 * This has been broken into three steps, so that
	 * the middle step can be executed with a mutex lock.
	 * @see #workPrepare(Collection) the 1st step
	 * @see #workExecute(Project) the 2nd step
	 * @see UpdateJob#update() the 3rd step
	 * 
	 * @param files A collection of {@link IResource}s. 
	 * @throws CoreException
	 */
	public void work(Collection files) throws CoreException {
		Project proj = workPrepare(files);
		UpdateJob uj = workExecute(proj);
		if (uj != null) uj.update();
	}

	/**
	 * Prepare to run FindBugs on the given collection of files.
	 *  
	 * @param files A collection of {@link IResource}s.
	 * @return a findbugs Project that may be passed to workExecute
	 * @throws CoreException
	 */
	public static Project workPrepare(Collection files) throws CoreException {
		if (files == null) {
			if (DEBUG) {
				System.out.println("No files to build"); //$NON-NLS-1$
			}
		}

		String findBugsHome = FindbugsPlugin.getFindBugsEnginePluginLocation();
		if (DEBUG) {
			System.out.println("Looking for detectors in: " + findBugsHome); //$NON-NLS-1$
		}

		// FIXME hardcoded findbugs.home property
		System.setProperty("findbugs.home", findBugsHome); //$NON-NLS-1$

		Project findBugsProject = new Project();
		Iterator iter = files.iterator();
		while (iter.hasNext()) {
			// get the resource
			IResource res = (IResource) iter.next();

			if (Util.isJavaArtifact(res)) {
				res.deleteMarkers(
					FindBugsMarker.NAME,
					true,
					IResource.DEPTH_INFINITE);
			}

			if (Util.isClassFile(res)) {
				// add this file to the work list:
				String fileName = res.getLocation().toOSString();

				res.refreshLocal(IResource.DEPTH_INFINITE, null);
				if (DEBUG) {
					System.out.println(
						"Resource: " + fileName //$NON-NLS-1$
						+ ": in sync: " + res.isSynchronized(IResource.DEPTH_INFINITE)); //$NON-NLS-1$
				}
				findBugsProject.addFile(fileName);
			}
		}
		return findBugsProject;
	}

	/**
	 * Run FindBugs on the given findBugsProject's files up
	 * through findBugs.execute(), and return a Job object
	 * which can be used to update the found bugs.
	 * 
	 * The returned Job object may be null if something
	 * unexpected occurs. Call its update() method to
	 * update the found bugs directly, or schedule it
	 * as an Eclipse task.
	 * 
	 * The reason for splitting the work is that a mutex
	 * should be held to run findBugs.execute(), but this
	 * causes trouble when updateBugCollection() attempts
	 * to write to the .fbwarnings file. So the invoker can
	 * release the lock before executing the returned Job.
	 *  
	 * @param findBugsProject to which files have already been added
	 * @return an UpdateJob to update found bugs, or null on error
	 */
	public UpdateJob workExecute(Project findBugsProject) {

		Reporter bugReporter = new Reporter(this.project, this.monitor, findBugsProject);
		bugReporter.setPriorityThreshold(Detector.LOW_PRIORITY);
    
		String[] classPathEntries = createClassPathEntries();
		// add to findbugs classpath
		for (int i = 0; i < classPathEntries.length; i++) {
			findBugsProject.addAuxClasspathEntry(classPathEntries[i]);
		}
        
		FindBugs findBugs = new FindBugs(bugReporter, findBugsProject);

		// configure detectors.
		findBugs.setUserPreferences(this.userPrefs);

		try {
			// Perform the analysis!
			findBugs.execute();
			
			// return a job that will Merge new results into existing results.
			return new UpdateJob(findBugsProject, bugReporter);
			// was: updateBugCollection(findBugsProject, bugReporter);
		}
		catch (InterruptedException e) {
			if (DEBUG) {
				e.printStackTrace();
			}
			// @see IncrementalProjectBuilder.build
			//throw new OperationCanceledException("FindBugs operation cancelled by user");
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs analysis");
		}
		return null;
	}

	/**
	 * Update the BugCollection for the project.
	 * 
	 * @param findBugsProject FindBugs project representing analyzed classes
	 * @param bugReporter     Reporter used to collect the new warnings
	 * @throws CoreException
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void updateBugCollection(Project findBugsProject, Reporter bugReporter)
			throws CoreException, IOException, DocumentException {
		SortedBugCollection oldBugCollection = FindbugsPlugin.getBugCollection(project, monitor);
		SortedBugCollection newBugCollection = bugReporter.getBugCollection();

		if (INCREMENTAL_UPDATE) {
			updateBugCollectionIncrementally(bugReporter, oldBugCollection, newBugCollection);
		} else {
			updateBugCollectionDestructively(bugReporter, oldBugCollection, newBugCollection);
		}

		// Store updated BugCollection
		FindbugsPlugin.storeBugCollection(project, oldBugCollection, findBugsProject, monitor);
	}

	/**
	 * Update the original bug collection to include the information in
	 * the new bug collection, preserving the history and classification
	 * of each warning.
	 * 
	 * @param bugReporter      Reporter used to collect the new warnings
	 * @param oldBugCollection original warnings
	 * @param newBugCollection new warnings
	 */
	private void updateBugCollectionIncrementally(
			Reporter bugReporter,
			SortedBugCollection oldBugCollection,
			SortedBugCollection newBugCollection) {
		throw new UnsupportedOperationException();
//		UpdateBugCollection updater = new UpdateBugCollection(oldBugCollection, newBugCollection);
//		updater.setUpdatedClassNameSet(bugReporter.getAnalyzedClassNames());
//		updater.execute();
	}

	/**
	 * Update the original bug collection destructively.
	 * Each warning in the set of analyzed classes is replaced with
	 * warnings from the new bug collection.  Past history is discarded.
	 * 
	 * @param bugReporter      Reporter used to collect the new warnings
	 * @param oldBugCollection original warnings
	 * @param newBugCollection new warnings
	 */
	private void updateBugCollectionDestructively(
			Reporter bugReporter,
			SortedBugCollection oldBugCollection,
			SortedBugCollection newBugCollection) {
		// FIXME we do this destructively for now: should do incrementally

		// Algorithm:
		// Remove all old warnings for classes which were just analyzed.
		// Then add all new warnings.
		if (oldBugCollection != null) {
			Set analyzedClassNameSet = bugReporter.getAnalyzedClassNames();
			for (Iterator i = oldBugCollection.iterator(); i.hasNext(); ) {
				BugInstance oldWarning = (BugInstance) i.next();
				ClassAnnotation warningClass = oldWarning.getPrimaryClass();
				if (warningClass != null && analyzedClassNameSet.contains(warningClass.getClassName())) {
					i.remove();
				}
			}
		} else {
			oldBugCollection = new SortedBugCollection();
		}
		for (Iterator i = newBugCollection.iterator(); i.hasNext(); ) {
			BugInstance newWarning = (BugInstance) i.next();
			oldBugCollection.add(newWarning);
		}
	}

	private String[] createClassPathEntries() {
		IJavaProject javaProject = JavaCore.create(project);
		try {
			return JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		}
		catch (CoreException e) {
			if (DEBUG) {
				FindbugsPlugin.getDefault().logException(e, "Could not compute classpath for project");
			}
		}
		return new String[0];
	}

	/** A nested class that is prepared to call updateBugCollection().
	 *  You may ignore the fact that it extends <tt>Job</tt> and call
	 *  update() directly, or you may use the eclipse scheduler.
	 *  @see #workExecute(Collection)
	 */
	public class UpdateJob extends Job {
		private Project findBugsProject;
		private Reporter bugReporter;

		public UpdateJob(Project findBugsProject, Reporter bugReporter) {
			super("Updating found bugs...");
			//setUser(true); // don't want this, since not _directly_ initiated by end user
			this.findBugsProject = findBugsProject;
			this.bugReporter = bugReporter;
		}

		public IStatus update() {
			try {
				// Merge new results into existing results.
				updateBugCollection(findBugsProject, bugReporter);
			}
			catch (RuntimeException re) {
				throw re;
			} catch (Exception e) {
				FindbugsPlugin.getDefault().logException(e, "Error updating FindBugs analysis");
				return Status.CANCEL_STATUS; // is this what we want?
			}
			return Status.OK_STATUS;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			return update();
		}
	}

}
