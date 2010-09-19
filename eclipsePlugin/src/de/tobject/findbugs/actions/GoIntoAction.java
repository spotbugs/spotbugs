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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugGroup;

public class GoIntoAction implements IViewActionDelegate {

    private BugExplorerView navigator;

    private Object selectedElement;

    public void init(IViewPart view) {
        if (view instanceof BugExplorerView) {
            navigator = (BugExplorerView) view;
        }
    }

    public void run(IAction action) {
        if (action.isEnabled() && navigator != null && selectedElement != null) {
            CommonViewer viewer = navigator.getCommonViewer();
            Object[] expandedElements = viewer.getVisibleExpandedElements();
            viewer.setInput(selectedElement);
            viewer.setExpandedElements(expandedElements);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }
        IStructuredSelection ssel = (IStructuredSelection) selection;
        if (ssel.size() != 1) {
            action.setEnabled(false);
            return;
        }
        Object element = ssel.getFirstElement();
        if (!(element instanceof BugGroup)) {
            action.setEnabled(false);
            return;
        }
        action.setEnabled(true);
        selectedElement = element;
    }
}
