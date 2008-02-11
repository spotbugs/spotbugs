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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.ResourceUtils;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

public class LoadXmlAction extends FindBugsAction implements IEditorActionDelegate {

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(final IAction action) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;

			List<IProject> projects = getProjects(structuredSelection);

			for (IProject project : projects) {
				// Get the file name from a file dialog
				FileDialog theDialog = new FileDialog(new Shell(),
						SWT.APPLICATION_MODAL | SWT.OPEN);
				theDialog.setText("Select bug result xml for project: "
						+ project.getName());
				String theFileName = theDialog.open();
				if (theFileName == null) {
					continue;
				}
				work(project, theFileName);
			}
		}
	}


	/**
	 * @param structuredSelection
	 * @return
	 */
	private List<IProject> getProjects(IStructuredSelection structuredSelection) {
		List<IProject> projects = new ArrayList<IProject>();
		for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
			Object element = iter.next();
			IResource resource = ResourceUtils.getResource(element);
			if (resource == null) {
				continue;
			}
			IProject project = resource.getProject();
			// do not need to check for duplicates, cause user cannot select
			// the same element twice
			if(!projects.contains(project)) {
				projects.add(project);
			}
		}
		return projects;
	}


	/**
	 * Run a FindBugs analysis on the given resource, displaying a progress monitor.
	 *
	 * @param resource
	 *            The resource to run the analysis on.
	 */
	protected final void work(final IProject project, final String fileName) {

		Job runFindBugs = new Job("Loading XML data from..." + fileName + "...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					FindBugsWorker worker = new FindBugsWorker(project, monitor);
					worker.loadXml(fileName);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e, "Analysis exception");
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		runFindBugs.setUser(true);
		runFindBugs.setRule(new MutexSchedulingRule(project));
		runFindBugs.schedule();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		usedInEditor = true;
		if (targetEditor instanceof AbstractDecoratedTextEditor) {
			AbstractDecoratedTextEditor editor = (AbstractDecoratedTextEditor) targetEditor;
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFileEditorInput editorInput = (IFileEditorInput) input;
				selection = new StructuredSelection(editorInput.getFile());
			}
		} else {
			selection = null;
		}
	}
}
