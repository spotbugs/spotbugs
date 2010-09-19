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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

public class GroupByAction implements IViewActionDelegate {

    private static final String ACTION_ID_PREFIX = "findBugsEclipsePlugin.toggleGrouping.";

    private CommonNavigator navigator;

    public void init(IViewPart view) {
        if (view instanceof CommonNavigator) {
            navigator = (CommonNavigator) view;
        }
    }

    public void run(IAction action) {
        if (navigator == null) {
            return;
        }
        Grouping grouping = getGrouping(action.getId());
        if (grouping == null) {
            return;
        }
        BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
        provider.setGrouping(grouping);
        CommonViewer viewer = navigator.getCommonViewer();
        Object[] expandedElements = viewer.getExpandedElements();
        provider.reSetInput();
        viewer.setExpandedElements(expandedElements);
    }

    private Grouping getGrouping(String id) {
        if (id == null) {
            return null;
        }
        if (!id.startsWith(ACTION_ID_PREFIX)) {
            return null;
        }
        id = id.substring(ACTION_ID_PREFIX.length());
        String[] typesArr = id.split("\\.");
        List<GroupType> types = new ArrayList<GroupType>();
        for (String string : typesArr) {
            GroupType type = GroupType.valueOf(string);
            types.add(type);
        }
        return Grouping.createFrom(types);
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // noop
    }

}
