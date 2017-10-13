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

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupSelectionDialog;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

public class OpenGroupDialogAction implements IViewActionDelegate {

    private CommonNavigator navigator;

    @Override
    public void init(IViewPart view) {
        if (view instanceof CommonNavigator) {
            navigator = (CommonNavigator) view;
        }
    }

    @Override
    public void run(IAction action) {
        if (navigator == null) {
            return;
        }
        BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
        List<GroupType> list = provider.getGrouping().asList();
        GroupSelectionDialog dialog = new GroupSelectionDialog(navigator.getSite().getShell(), list);
        dialog.setTitle("Bug Group Configuration");
        int result = dialog.open();
        if (result != Window.OK) {
            return;
        }
        Grouping grouping = Grouping.createFrom(dialog.getGroups());
        provider.setGrouping(grouping);
        CommonViewer viewer = navigator.getCommonViewer();
        Object[] expandedElements = viewer.getExpandedElements();
        provider.reSetInput();
        viewer.setExpandedElements(expandedElements);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (navigator == null) {
            action.setEnabled(false);
            return;
        }
        BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
        action.setEnabled(provider.getGrouping() != null);
    }

}
