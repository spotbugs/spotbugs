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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ISetSelectionTarget;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;

public class ShowInPackageExplorerAction implements IObjectActionDelegate {

    private IWorkbenchPartSite site;

    private Object data;

    public ShowInPackageExplorerAction() {
        super();
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        site = targetPart.getSite();
    }

    public void run(IAction action) {
        if (data == null) {
            return;
        }
        IViewPart part = getView(JavaUI.ID_PACKAGES);
        if (part instanceof ISetSelectionTarget) {
            ISetSelectionTarget target = (ISetSelectionTarget) part;
            target.selectReveal(new StructuredSelection(data));
        }
    }

    private IViewPart getView(String id) {
        IViewPart part;
        try {
            part = site.getPage().showView(id);
            return part;
        } catch (PartInitException e) {
            FindbugsPlugin.getDefault().logException(e, "Can't open view: " + id);
            return null;
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            data = null;
            action.setEnabled(false);
            return;
        }
        IStructuredSelection ss = (IStructuredSelection) selection;
        if (ss.size() != 1) {
            data = null;
            action.setEnabled(false);
            return;
        }
        Object firstElement = ss.getFirstElement();
        if (firstElement instanceof IMarker) {
            IMarker marker = (IMarker) firstElement;
            data = marker.getResource();
            action.setEnabled(data != null);
            return;
        }
        if (!(firstElement instanceof BugGroup)) {
            data = null;
            action.setEnabled(false);
            return;
        }
        BugGroup group = (BugGroup) firstElement;
        if (group.getType() == GroupType.Class || group.getType() == GroupType.Package || group.getType() == GroupType.Project) {
            data = group.getData();
            action.setEnabled(data != null);
        } else {
            data = null;
            action.setEnabled(false);
        }
    }

}
