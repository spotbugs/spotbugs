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

package de.tobject.findbugs.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.ResourceUtils;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;



/**
 * Run FindBugs on the currently selected element(s) in the package explorer.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 1.1
 * @since 25.09.2003
 */
public class FindBugsAction implements IObjectActionDelegate {

	/** The current selection. */
	protected ISelection selection;

	/** true if this action is used from editor */
	protected boolean usedInEditor;

	/*
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public final void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		// noop
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public final void selectionChanged(final IAction action,
			final ISelection newSelection) {
		if (!usedInEditor) {
			this.selection = newSelection;
		}
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(final IAction action) {
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection sSelection = (IStructuredSelection) selection;

				if (selection.isEmpty()) {
					return;
				}

				Map<IProject, List<IResource>> projectMap =
					ResourceUtils.getResourcesPerProject(sSelection);

				for(Map.Entry<IProject, List<IResource>> e : projectMap.entrySet()) {
					work(e.getKey(), e.getValue());
				}
			}
		}
	}

	/**
	 * Run a FindBugs analysis on the given resource, displaying a progress
	 * monitor.
	 *
	 * @param resources The resource to run the analysis on.
	 */
	protected final void work(final IProject project, final List<IResource> resources) {

		Job runFindBugs = new Job("Finding bugs in " + project.getName() + "...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					FindBugsWorker worker =	new FindBugsWorker(project, monitor);
					worker.work(resources);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e, "Analysis exception");
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}

			/**
			 * Overriden to be able to control the max number of running FB jobs
			 */
			@Override
			public boolean belongsTo(Object family) {
				return MutexSchedulingRule.class == family;
			}
		};

		runFindBugs.setUser(true);
		runFindBugs.setPriority(Job.BUILD);
		runFindBugs.setRule(new MutexSchedulingRule(project));
		runFindBugs.schedule();
	}
}
