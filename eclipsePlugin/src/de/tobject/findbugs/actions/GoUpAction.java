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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.view.BugExplorerView;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.Grouping;

public class GoUpAction implements IViewActionDelegate {
    private BugExplorerView navigator;

    public void init(IViewPart view) {
        if (view instanceof BugExplorerView) {
            navigator = (BugExplorerView) view;
        }
    }

    public void run(IAction action) {
        if (!action.isEnabled()) {
            return;
        }
        CommonViewer viewer = navigator.getCommonViewer();
        Object[] expandedElements = viewer.getVisibleExpandedElements();
        Object input = viewer.getInput();
        if (input instanceof BugGroup) {
            BugGroup group = (BugGroup) input;
            Object data = group.getParent();
            boolean needRefresh = data == null;
            if (needRefresh) {
                BugContentProvider.getProvider(navigator.getNavigatorContentService()).reSetInput();
            } else {
                viewer.setInput(data);
            }
            // viewer.setSelection(new StructuredSelection(input), true);
            // viewer.expandToLevel(input, 1);
            viewer.setExpandedElements(expandedElements);
        }
        action.setEnabled(isEnabled());
    }

    public void selectionChanged(IAction action, ISelection selection) {
        action.setEnabled(isEnabled());
    }

    private boolean isEnabled() {
        if (navigator == null) {
            return false;
        }
        Object input = navigator.getCommonViewer().getInput();
        if (input instanceof IMarker) {
            return true;
        }
        if (input instanceof BugGroup) {
            BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
            BugGroup bugGroup = (BugGroup) input;
            Grouping grouping = provider.getGrouping();
            if (grouping == null) {
                return false;
            }
            // as long as the current input is on the "visible" children list
            // (workspace is invisible)
            return grouping.contains(bugGroup.getType());
        }
        return false;
    }

}
