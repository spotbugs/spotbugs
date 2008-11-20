/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.view;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.marker.FindBugsMarker;

final class MarkerSelectionListener implements ISelectionListener {
	private final IMarkerSelectionHandler handler;

	public MarkerSelectionListener(IMarkerSelectionHandler handler) {
		this.handler = handler;
	}

	public void selectionChanged(IWorkbenchPart thePart,
			ISelection theSelection) {
		if (!(theSelection instanceof IStructuredSelection)) {
			return;
		}
		if (thePart == handler || !handler.isVisible()) {
			return;
		}
		IMarker marker = null;
		Object elt = ((IStructuredSelection) theSelection).getFirstElement();
		if (elt instanceof IMarker) {
			marker = (IMarker) elt;
		}

		// bug 2030157: selections in problems view are not reflected in our views
		// we cannot use MarkerItem because this is new Eclipse 3.4 API.
		/* else if (elt instanceof MarkerItem){
			theMarker = ((MarkerItem)elt).getMarker();
		}*/
		// the code below is the workaroound compatible with both 3.3 and 3.4 API
		else if (elt instanceof IAdaptable) {
			marker = (IMarker) ((IAdaptable)elt).getAdapter(IMarker.class);
		}

		if (marker != null) {
			try {
				if(!marker.isSubtypeOf(FindBugsMarker.NAME)){
					// we are not interested in other markers then FB
					return;
				}
			} catch (CoreException e) {
				// ignore
			}
			handler.markerSelected(marker);
		}
	}
}
