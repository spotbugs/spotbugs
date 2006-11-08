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

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The <code>FindBugsBuilder</code> performs a FindBugs run on a subset of the
 * current project. It will either check all classes in a project or just the
 * ones just having been modified.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 25.9.2003
 * @see IncrementalProjectBuilder
 */
public class FindBugsBuilder extends IncrementalProjectBuilder {

	/** Controls debugging. */
	public static boolean DEBUG;
	
	/**
	 * Run the builder.
	 * 
	 * @see IncrementalProjectBuilder#build
	 */
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Running FindBugs...");
		switch (kind) {
			case IncrementalProjectBuilder.FULL_BUILD :
				{
					if (DEBUG) {
						System.out.println("FULL BUILD");
					}
					doBuild(args, monitor);
					break;
				}
			case IncrementalProjectBuilder.INCREMENTAL_BUILD :
				{
					if (DEBUG) {
						System.out.println("INCREMENTAL BUILD");
					}
					doBuild(args, monitor);
					break;
				}
			case IncrementalProjectBuilder.AUTO_BUILD :
				{
					if (DEBUG) {
						System.out.println("AUTO BUILD");
					}
					doBuild(args, monitor);
					break;
				}
		}
		return null;
	}
	
	/**
	 * Performs the build process. This method gets all files in the current project
	 * and has a <code>FindBugsVisitor</code> run on them.
	 * 
	 * @param args A <code>Map</code> containing additional build parameters.
	 * @param monitor The <code>IProgressMonitor</code> displaying the build progress.
	 * @throws CoreException
	 */
	private void doBuild(final Map args, final IProgressMonitor monitor) throws CoreException {
		AbstractFilesCollector collector = FilesCollectorFactory.getFilesCollector(this);
		Collection files = collector.getFiles();
		FindBugsWorker worker = new FindBugsWorker(this.getProject(), monitor);
		worker.work(files);
	}
	
}
