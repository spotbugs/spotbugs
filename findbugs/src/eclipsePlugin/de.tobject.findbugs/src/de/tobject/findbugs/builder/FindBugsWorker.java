/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003, Peter Friese
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.Reporter;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;

/**
 * TODO Enter a comment for .
 * @author U402101
 * @version 1.0
 * @since 26.09.2003
 */
public class FindBugsWorker {
	
	private IProgressMonitor monitor;

	private IProject project;

	public FindBugsWorker(IProject project, IProgressMonitor monitor) {
		super();
		this.project = project;
		this.monitor = monitor;
	}

	/**
	 * Controls debugging.
	 */
	public static boolean DEBUG;
	
	public void work(Collection files) throws CoreException {
		int count = 0;

		if (files != null) {
			if (this.monitor != null) {
				this.monitor.beginTask("FindBugs", files.size());
			}

			String findBugsHome = FindbugsPlugin.getFindBugsEnginePluginLocation();
			if (DEBUG) {
				System.out.println("Looking for detecors in: " + findBugsHome);
			}
			System.setProperty("findbugs.home", findBugsHome);

			BugReporter bugReporter = new Reporter(this.project, this.monitor);
			bugReporter.setPriorityThreshold(Detector.LOW_PRIORITY);
			edu.umd.cs.findbugs.Project findBugsProject =
				new edu.umd.cs.findbugs.Project();
			FindBugs findBugs = new FindBugs(bugReporter, findBugsProject);

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
					System.out.println("Resource: " + fileName  + ": in synch: " + res.isSynchronized(IResource.DEPTH_INFINITE));
					findBugsProject.addJar(fileName);
				}
			}

			try {
				findBugs.execute();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			//			Iterator iter = files.iterator();
			//			while (iter.hasNext()) {
			//				// get the resource
			//				IResource res = (IResource) iter.next();
			//				
			//				// advance progress monitor
			//				if (monitor != null && count % MONITOR_INTERVAL == 0) {
			//					monitor.worked(MONITOR_INTERVAL);
			//					monitor.subTask("Performing bug check on: " + res.getName());
			//					if (monitor.isCanceled())
			//						break;
			//				}
			//				
			//				// visit resource
			//				res.accept(new FindBugsVisitor());
			//				count++;
			//			}
		}
		else {
			if (DEBUG) {
				System.out.println("No files to build");
			}
		}
	}
	
	private boolean isJavaArtifact(IResource resource) {
		if (resource != null) {
			if ( (resource.getName().endsWith(".java")) || (resource.getName().endsWith(".class")) ) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isClassFile(IResource resource) {
		if (resource != null) {
			if (resource.getName().endsWith(".class")) {
				return true;
			}
		}
		return false;
	}

}
