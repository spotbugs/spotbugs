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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import de.tobject.findbugs.FindbugsPlugin;

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
	@SuppressWarnings("unchecked")
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Running FindBugs...");
		switch (kind) {
		case IncrementalProjectBuilder.FULL_BUILD: {
			if (FindbugsPlugin.getUserPreferences(getProject()).isRunAtFullBuild()){
				if (DEBUG) {
					System.out.println("FULL BUILD");
				}
				doBuild(args, monitor, kind);
			} else {
				// TODO probably worth to cleanup? MarkerUtil.removeMarkers(getProject());
			}
			break;
		}
		case IncrementalProjectBuilder.INCREMENTAL_BUILD: {
			if (DEBUG) {
				System.out.println("INCREMENTAL BUILD");
			}
			doBuild(args, monitor, kind);
			break;
		}
		case IncrementalProjectBuilder.AUTO_BUILD: {
			if (DEBUG) {
				System.out.println("AUTO BUILD");
			}
			doBuild(args, monitor, kind);
			break;
		}
		}
		return null;
	}

	/**
	 * Performs the build process. This method gets all files in the current project and
	 * has a <code>FindBugsVisitor</code> run on them.
	 *
	 * @param args
	 *            A <code>Map</code> containing additional build parameters.
	 * @param monitor
	 *            The <code>IProgressMonitor</code> displaying the build progress.
	 * @param kind
	 *            kind the kind of build being requested, see IncrementalProjectBuilder
	 * @throws CoreException
	 */
	private void doBuild(final Map<?,?> args, final IProgressMonitor monitor, int kind) throws CoreException {
		boolean incremental = (kind != IncrementalProjectBuilder.FULL_BUILD);
		IProject project = getProject();
		FindBugsWorker worker = new FindBugsWorker(project, monitor);
		List<WorkItem> files;
		if(incremental) {
			IResourceDelta resourceDelta = getDelta(project);
			boolean configChanged = !isConfigUnchanged(resourceDelta);
			boolean fullBuildEnabled = FindbugsPlugin.getUserPreferences(getProject(),
					configChanged).isRunAtFullBuild();
			if (configChanged && fullBuildEnabled) {
				files = new ArrayList<WorkItem>();
				files.add(new WorkItem(project));
			} else {
				files = ResourceUtils.collectIncremental(resourceDelta);
				/*
				 * Here we expect to have only ONE file as a result of a post-save
				 * trigger. In this case incremental builder should run and analyse 1 file
				 * again. For some reason, JDT uses "AUTO" kind for such incremental
				 * compile. Unfortunately, Eclipse also triggers "AUTO" build on startup,
				 * if it detects that some project files are changed (I think also on team
				 * update operations). This causes sometimes a real startup slowdown for
				 * workspaces with many projects. Because we cannot distinguish between
				 * first and second build, and if "fullBuildEnabled" id OFF, we will
				 * analyse incrementally ONLY ONE SINGLE FILE. This is not nice, but there
				 * is no other ways today... May be we can use preferences to define
				 * "how many" is "incremental"...
				 */
				if(files.size() > 1){
					if(DEBUG){
						FindbugsPlugin.getDefault().logInfo(
								"Incremental builder: too many resources to analyse for project "
										+ project + ", files: " + files);
					}
					return;
				}
			}
		} else {
			files = new ArrayList<WorkItem>();
			files.add(new WorkItem(project));
		}
		worker.work(files);
	}

	private boolean isConfigUnchanged(IResourceDelta resourceDelta) {
		return resourceDelta != null
				&& resourceDelta.findMember(new Path(".project")) == null
				&& resourceDelta.findMember(new Path(".classpath")) == null
				&& resourceDelta.findMember(new Path(".fbprefs")) == null;
	}
}
