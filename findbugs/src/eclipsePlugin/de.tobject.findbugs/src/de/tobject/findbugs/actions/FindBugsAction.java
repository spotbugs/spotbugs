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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.AbstractFilesCollector;
import de.tobject.findbugs.builder.FilesCollectorFactory;
import de.tobject.findbugs.builder.FindBugsWorker;

/**
 * Run FindBugs on the currently selected element(s) in the package explorer.
 *  
 * @author Peter Friese
 * @version 1.0
 * @since 25.09.2003
 */
public class FindBugsAction implements IObjectActionDelegate {

	/** The current selection. */
	private ISelection selection;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public final void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public final void run(final IAction action) {
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				for (Iterator iter = structuredSelection.iterator(); iter.hasNext(); ) {
					Object element = (Object) iter.next();

					/* If the current selection is a Java project:
					 * - get all contained files
					 * - hand them over to a FindBugs worker for processing 
					 */
					IProject project = getProject(element);
					if (project != null) {
						work(project);
					}
				}
			}
		}
	}
	
	private IProject getProject(Object element) {
		if (element instanceof IJavaProject) {
			IJavaProject javaProject = (IJavaProject) element;
			return javaProject.getProject();
		}
		else if (element instanceof IProject) {
			return (IProject) element;
		}
		return null;
	}
	
	private void work(final IProject project) {
		AbstractFilesCollector collector = FilesCollectorFactory.getFilesCollector(project);
		try {
			final Collection files = collector.getFiles();
		
			IRunnableWithProgress r = new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						FindBugsWorker worker = new FindBugsWorker(project, pm);
						worker.work(files);
					}
					catch (CoreException ex) {
						throw new InvocationTargetException(ex);
					}
				}
			};
		
			ProgressMonitorDialog progress = new ProgressMonitorDialog(FindbugsPlugin.getShell());
			progress.run(true, true, r);
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
