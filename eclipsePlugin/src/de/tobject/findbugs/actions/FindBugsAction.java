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
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.AbstractFilesCollector;
import de.tobject.findbugs.builder.FilesCollectorFactory;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.util.Util;



/**
 * Run FindBugs on the currently selected element(s) in the package explorer.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @version 1.1
 * @since 25.09.2003
 */
public class FindBugsAction implements IObjectActionDelegate {

	/** lock to force no more than one findBugs.execute() task at a time. see
	  * http://help.eclipse.org/help30/topic/org.eclipse.platform.doc.isv/guide/runtime_jobs_locks.htm
	  * Alas, this is less pretty than a edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule
	  * from the user's point of view, but it doesn't have the IllegalArgumentException
	  * problem ("does not match outer scope rule") so this the ILock is preferred. */
	protected static final ILock findbugsExecuteLock = Platform.getJobManager().newLock();

	/** The current selection. */
	private ISelection selection;

	/*
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public final void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public final void selectionChanged(final IAction action,
			final ISelection selection) {
		this.selection = selection;
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public final void run(final IAction action) {
		if (!selection.isEmpty()) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				for (Iterator iter = structuredSelection.iterator(); iter
						.hasNext();) {
					Object element = iter.next();
					IResource resource = (IResource) ((IAdaptable) element)
							.getAdapter(IResource.class);
					if (resource == null)
						continue;

					work(resource);
				}
			}
		}
	}

	/**
	 * The files contained within a resource. Searches container resources, such
	 * as folders or packages, or parses single file resources such as java
	 * files.
	 *
	 * @param resource
	 *            the resource to search for files
	 * @return the files contained within the given resource
	 * @throws CoreException
	 */
	private Collection filesInResource(IResource resource) throws CoreException {
		/*
		 * Note: the default package is an IContainer that has all other
		 * packages as subfolders. Eclipse treats the "default package" as the
		 * project itself. Thus, this method will return ALL java files in the
		 * project when invoked on the default package resource.
		 */
		if (resource instanceof IContainer) {
			AbstractFilesCollector collector = FilesCollectorFactory
					.getFilesCollector((IContainer) resource);
			return collector.getFiles();
		} else {
			Collection<IResource> result = new ArrayList<IResource>();
			result.add(resource);
			// For a single file resource, if we have a java file, attempt to
			// add its corresponding class file,
			// and vice versa, so that the analysis can proceed.
			if (resource.getFileExtension().equalsIgnoreCase("java")) {
				result.add(resource.getParent().findMember(
						Util.changeExtension(resource.getName(), "class")));
			} else if (resource.getFileExtension().equalsIgnoreCase("class")) {
				result.add(resource.getParent().findMember(
						Util.changeExtension(resource.getName(), "java")));
			}
			return result;
		}
	}

	/**
	 * Run a FindBugs analysis on the given resource, displaying a progress
	 * monitor.
	 *
	 * @param resource The resource to run the analysis on.
	 */
	private void work(final IResource resource) {
		try {
			final Collection files = filesInResource(resource);
			Job runFindBugs = new Job("Finding bugs in "+resource.getName()+"...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					FindBugsWorker worker =
						new FindBugsWorker(resource.getProject(), monitor);
					try {
						findbugsExecuteLock.acquire();
						worker.work(files, resource);
					} catch (CoreException e) {
						FindbugsPlugin.getDefault().logException(e, "Analysis exception");
						return Status.CANCEL_STATUS;
					}
					finally {
						findbugsExecuteLock.release();
					}
					return Status.OK_STATUS;
				}
			};

			runFindBugs.setUser(true);
			runFindBugs.schedule();
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}
