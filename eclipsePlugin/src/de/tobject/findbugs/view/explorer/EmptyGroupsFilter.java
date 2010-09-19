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
package de.tobject.findbugs.view.explorer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * @author Andrei
 */
public class EmptyGroupsFilter extends ViewerFilter {

    public EmptyGroupsFilter() {
        super();
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        CommonViewer v2 = (CommonViewer) viewer;
        BugContentProvider provider = BugContentProvider.getProvider(v2.getNavigatorContentService());
        if (element instanceof BugGroup) {
            BugGroup group = (BugGroup) element;
            if (provider.getFilteredMarkersCount(group) == group.getMarkersCount()) {
                return false;
            }
        }
        return true;
    }

}
