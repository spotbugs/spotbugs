/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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
import java.util.List;

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
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;

/**
 * TODO Enter a comment for .
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 26.09.2003
 */
public class FindBugsWorker {
	
	/** Controls debugging. */
	public static boolean DEBUG;

	private IProgressMonitor monitor;
	private List selectedDetectorFactories;
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
			selectedDetectorFactories = FindbugsPlugin.readDetectorFactories(project);
		}
		catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			System.out.println("Looking for detecors in: " + findBugsHome); //$NON-NLS-1$
		}

		// XXX hardcoded findbugs.home property
		System.setProperty("findbugs.home", findBugsHome); //$NON-NLS-1$

		Project findBugsProject = new Project();
		Iterator iter = files.iterator();
		while (iter.hasNext()) {
			// get the resource
			IResource res = (IResource) iter.next();

			if (isJavaArtifact(res)) {
				res.deleteMarkers(
					FindBugsMarker.NAME,
					true,
					IResource.DEPTH_INFINITE);
			}

			if (isClassFile(res)) {
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

		BugReporter bugReporter = new Reporter(this.project, this.monitor, findBugsProject);
		bugReporter.setPriorityThreshold(Detector.LOW_PRIORITY);
		FindBugs findBugs = new FindBugs(bugReporter, findBugsProject);

		String[] classPathEntries = createClassPathEntries();
		// add to findbugs classpath
		for (int i = 0; i < classPathEntries.length; i++) {
			findBugsProject.addAuxClasspathEntry(classPathEntries[i]);
		}

		// configure detectors.
		// XXX currently detector factories are shared between different projects!!!
		// cause detector factories list is a singleton!!!
		// if multiple workers are working (Eclipse 3.0 allows background build),
		// there is a big problem!!!
		if (selectedDetectorFactories != null) {
			Iterator iterator = DetectorFactoryCollection.instance().factoryIterator();
			while (iterator.hasNext()) {
				DetectorFactory factory = (DetectorFactory) iterator.next();
				factory.setEnabled(selectedDetectorFactories.contains(factory));
			}
		}

		try {
			findBugs.execute();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			if (DEBUG) {
				e.printStackTrace();
			}
			// @see IncrementalProjectBuilder.build
			//throw new OperationCanceledException("FindBugs operation cancelled by user");
		}
	}

	private String[] createClassPathEntries() {
		IJavaProject javaProject = JavaCore.create(project);
		try {
			return JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		}
		catch (CoreException e) {
			if (DEBUG) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new String[0];
	}

	/**
	 * Checks whether the given resource is a Java artifact (i.e. either a
	 * Java source file or a Java class file).
	 * 
	 * @param resource The resource to check.
	 * @return 
	 * 	<code>true</code> if the given resource is a Java artifact.
	 * 	<code>false</code> otherwise.
	 */
	private boolean isJavaArtifact(IResource resource) {
		if (resource != null) {
			if ((resource.getName().endsWith(".java")) //$NON-NLS-1$
			|| (resource.getName().endsWith(".class"))) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given resource is a Java class file.
	 * 
	 * @param resource The resource to check.
	 * @return 
	 * 	<code>true</code> if the given resource is a class file,
	 * 	<code>false</code> otherwise.
	 */
	private boolean isClassFile(IResource resource) {
		if (resource != null) {
			if (resource.getName().endsWith(".class")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

}
