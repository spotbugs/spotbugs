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

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.view.explorer.BugContentProvider;

public class RefreshAction implements IViewActionDelegate {

    private CommonNavigator navigator;

    public void init(IViewPart view) {
        if (view instanceof CommonNavigator) {
            navigator = (CommonNavigator) view;
        }
    }

    public void run(IAction action) {
        if (navigator != null) {
            CommonViewer viewer = navigator.getCommonViewer();
            BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
            // provider.setRefreshRequested(true);

            Object[] expandedElements = viewer.getExpandedElements();
            Object input = viewer.getInput();
            if (provider.getChildren(input).length == 0) {
                viewer.setInput(null);
                if (input instanceof IWorkingSet || input instanceof IWorkspaceRoot) {
                    viewer.setInput(input);
                } else {
                    viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
                }
                viewer.refresh(true);
            } else {
                provider.reSetInput();
            }
            viewer.setExpandedElements(expandedElements);
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // noop
    }
}
