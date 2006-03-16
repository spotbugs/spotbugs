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
	 * @param files A collection of {@link IResource}s. 
	 * @throws CoreException
	 */
	public void work(Collection files) throws CoreException {
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
			
			// Merge new results into existing results.
			updateBugCollection(findBugsProject, bugReporter);
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
}
