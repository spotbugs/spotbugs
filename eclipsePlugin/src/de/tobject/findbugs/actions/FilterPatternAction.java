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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.BugGroup;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;

public class FilterPatternAction implements IObjectActionDelegate {

    private CommonNavigator navigator;

    private Object data;

    private boolean useSpecificPattern;

    public FilterPatternAction() {
        super();
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if (targetPart instanceof CommonNavigator) {
            navigator = (CommonNavigator) targetPart;
            useSpecificPattern = action.getId().startsWith("de.tobject.findbugs.filterSpecificPattern");
        }
    }

    public void run(IAction action) {
        Set<String> sortedIds = FindbugsPlugin.getFilteredIds();
        String patternType = getPatternOrPatternType();
        if (patternType != null) {
            if (!sortedIds.contains(patternType)) {
                sortedIds.add(patternType);
            } else {
                sortedIds.remove(patternType);
            }
        }
        String ids = FindBugsConstants.encodeIds(sortedIds);
        final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        store.setValue(FindBugsConstants.LAST_USED_EXPORT_FILTER, ids);
        BugContentProvider provider = BugContentProvider.getProvider(navigator.getNavigatorContentService());
        if (!provider.isBugFilterActive()) {
            MessageDialog.openWarning(null, "Toggle Filter", "Filtering by pattern or type id is currently not enabled!\n"
                    + "To enable it, please select \"Toggle Filters...->Bugs by Id\" filter!");

        }
        provider.refreshFilters();
        CommonViewer viewer = navigator.getCommonViewer();
        Object[] expandedElements = viewer.getExpandedElements();
        viewer.refresh(true);
        viewer.setExpandedElements(expandedElements);
        data = null;
    }

    private String getPatternOrPatternType() {
        if (data instanceof IMarker) {
            BugInstance bug = MarkerUtil.findBugInstanceForMarker((IMarker) data);
            if (bug == null) {
                return null;
            }
            if (useSpecificPattern) {
                // uses specific pattern kind, the naming "Type" is misleading
                return bug.getType();
            }
            // uses pattern type, the naming "Abbrev" is misleading
            return bug.getAbbrev();
        } else if (data instanceof BugPattern) {
            BugPattern pattern = (BugPattern) data;
            if (useSpecificPattern) {
                // uses specific pattern kind, the naming "Type" is misleading
                return pattern.getType();
            }
            // uses pattern type, the naming "Abbrev" is misleading
            return pattern.getAbbrev();
        } else if (data instanceof BugCode) {
            // same as pattern.getAbbrev(): it's pattern type
            return ((BugCode) data).getAbbrev();
        }
        return null;
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
            data = firstElement;
            action.setEnabled(true);
            return;
        }
        if (!(firstElement instanceof BugGroup)) {
            data = null;
            action.setEnabled(false);
            return;
        }
        data = ((BugGroup) firstElement).getData();
        action.setEnabled(data != null);
    }

}
