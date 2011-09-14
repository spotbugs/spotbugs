/*
 * Contributions to FindBugs
 * Copyright (C) 2011, Andrey Loskutov
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Show details on a selected FindBugs marker.
 */
public class ShowBugInfoAction implements IObjectActionDelegate, IEditorActionDelegate {

    /** The current selection. */
    private ISelection selection;

    private IWorkbenchPart targetPart;

    public ShowBugInfoAction() {
        super();
    }

    public ShowBugInfoAction(IWorkbenchPart targetPart) {
        super();
        this.targetPart = targetPart;
    }

    public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

    public final void selectionChanged(final IAction action, final ISelection newSelection) {
        this.selection = newSelection;
    }

    public final void run(final IAction action) {
        if (targetPart == null) {
            return;
        }
        try {
            if (!selection.isEmpty() && (selection instanceof IStructuredSelection)) {
                IStructuredSelection ssel = (IStructuredSelection) selection;
                Object element = ssel.getFirstElement();
                if (element instanceof IMarker) {
                    IMarker marker = (IMarker) element;
                    FindbugsPlugin.showMarker(marker, FindbugsPlugin.DETAILS_VIEW_ID, targetPart);
                }
            }
        } finally {
            targetPart = null;
        }
    }

    public void setActiveEditor(IAction action, IEditorPart editor) {
        targetPart = editor.getSite().getPart();
    }

}
