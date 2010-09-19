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
package de.tobject.findbugs.actions;

import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.Util;

public class CopyMarkerDetailsAction implements IObjectActionDelegate {

    private ISelection selection;

    public CopyMarkerDetailsAction() {
        super();
    }

    public void selectionChanged(IAction action, ISelection newSelection) {
        this.selection = newSelection;
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // noop
    }

    public void run(IAction action) {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return;
        }
        Set<IMarker> markers = getMarkers();
        String content = getContent(markers);
        Util.copyToClipboard(content);
    }

    private String getContent(Set<IMarker> markers) {
        StringBuilder fullText = new StringBuilder();
        for (IMarker marker : markers) {
            try {
                StringBuilder line = new StringBuilder();

                IResource resource = marker.getResource();
                if (resource != null) {
                    IPath location = resource.getLocation();
                    if (location != null) {
                        line.append(location.toPortableString());
                    } else {
                        line.append(resource.getFullPath());
                    }
                }
                Integer lineNumber = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
                line.append(":").append(lineNumber);
                String message = (String) marker.getAttribute(IMarker.MESSAGE);
                line.append(" ").append(message);

                line.append(System.getProperty("line.separator", "\n"));
                fullText.append(line.toString());
            } catch (CoreException e) {
                FindbugsPlugin.getDefault().logException(e, "Exception while parsing content of FindBugs markers.");
            }
        }
        return fullText.toString();
    }

    private Set<IMarker> getMarkers() {
        return MarkerUtil.getMarkerFromSelection(selection);
    }

}
