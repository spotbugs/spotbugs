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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Show details on a selected FindBugs marker.
 */
public class OpenPropertiesAction implements IObjectActionDelegate {

	/** The current selection. */
	private ISelection selection;
	private IWorkbenchPart targetPart;

	public OpenPropertiesAction() {
		super();
	}

	public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	public final void selectionChanged(final IAction action, final ISelection newSelection) {
		this.selection = newSelection;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public final void run(final IAction action) {
		if(targetPart == null){
			return;
		}
		try {
			if (!selection.isEmpty() && (selection instanceof IStructuredSelection)) {
//				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
//					// TODO get id from API
				targetPart.getSite().getPage().showView("org.eclipse.ui.views.PropertySheet");
//				for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
////					BugGroup marker = (BugGroup) iter.next();
//					break;
//				}
			}
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Exception while parsing content of FindBugs markers.");
		} finally {
			targetPart = null;
		}
	}



}
