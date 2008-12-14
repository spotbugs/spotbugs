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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * Show details on a selected FindBugs marker.
 *
 * @author Phil Crosby
 */
public class MarkerShowDetailsAction implements IObjectActionDelegate {

	/** The current selection. */
	private ISelection selection;

	public MarkerShowDetailsAction() {
		super();
	}

	public final void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		// noop
	}

	public final void selectionChanged(final IAction action,
			final ISelection newSelection) {
		this.selection = newSelection;
	}

	public final void run(final IAction action) {
		IMarker marker = MarkerUtil.getMarkerFromSingleSelection(selection);
		if (marker != null) {
			FindbugsPlugin.showMarker(marker);
		}
	}

}
