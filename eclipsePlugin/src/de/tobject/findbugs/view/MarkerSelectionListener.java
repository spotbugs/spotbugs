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

class MarkerSelectionListener implements ISelectionListener {
    private final IMarkerSelectionHandler handler;

    @Override
    public String toString() {
        return "MarkerSelectionListener for " + handler.getClass().getSimpleName();
    }

    public MarkerSelectionListener(IMarkerSelectionHandler handler) {
        this.handler = handler;
    }

    public void selectionChanged(IWorkbenchPart thePart, ISelection theSelection) {
        if (thePart == handler || !handler.isVisible()) {
            return;
        }
        IMarker marker = MarkerUtil.getMarkerFromSingleSelection(theSelection);
        // only handle a null if it's from the bug explorer. this way if the
        // user
        // selects a bug pattern or other "folder" in the tree, the annotation
        // view
        // and the property view are cleared. BUT if the user clicks into
        // the editor, or some other view, it stays up. not quite sure what the
        // best behavior is here but for now this works. -Keiths
        if (marker == null && !(thePart instanceof BugExplorerView)) {
            return;
        }
        handler.markerSelected(thePart, marker);
    }
}
