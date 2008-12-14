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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.reporter.MarkerUtil;

final class MarkerSelectionListener implements ISelectionListener {
	private final IMarkerSelectionHandler handler;

	public MarkerSelectionListener(IMarkerSelectionHandler handler) {
		this.handler = handler;
	}

	public void selectionChanged(IWorkbenchPart thePart,
			ISelection theSelection) {
		if (thePart == handler || !handler.isVisible()) {
			return;
		}
		IMarker marker = MarkerUtil.getMarkerFromSingleSelection(theSelection);
		if (marker != null) {
			handler.markerSelected(marker);
		}
	}
}
