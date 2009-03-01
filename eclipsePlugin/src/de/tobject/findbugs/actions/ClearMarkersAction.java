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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * Remove all bug markers for the given selection.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 25.09.2003
 */
public class ClearMarkersAction implements IObjectActionDelegate {

	/** The current selection. */
	private ISelection currentSelection;

	public final void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		// noop
	}

	public final void selectionChanged(final IAction action,
			final ISelection selection) {
		this.currentSelection = selection;
	}

	public final void run(final IAction action) {
		if (!currentSelection.isEmpty()) {
			if (currentSelection instanceof IStructuredSelection) {
				IStructuredSelection sSelection = (IStructuredSelection) currentSelection;
				Map<IProject, List<IResource>> projectMap =
					ResourceUtils.getResourcesPerProject(sSelection);

				for(Map.Entry<IProject, List<IResource>> e : projectMap.entrySet()) {
					work(e.getKey(), e.getValue());
				}
			}
		}
	}

	/**
	 * Clear the FindBugs markers on each project in the given selection, displaying a progress monitor.
	 */
	private void work(IProject project, List<IResource> resources) {
		FindBugsJob job = new ClearMarkersJob(project, resources);
		job.scheduleInteractive();
	}
}

final class ClearMarkersJob extends FindBugsJob {
	private final List<IResource> resources;

	ClearMarkersJob(IProject project, List<IResource> resources) {
		super("Removing FindBugs markers", project);
		this.resources = resources;
	}

	@Override
	protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(getName(), resources.size());
		for (IResource res : resources) {
			monitor.subTask(res.getName());
			MarkerUtil.removeMarkers(res);
			monitor.worked(1);
		}
	}
}
