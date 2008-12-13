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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;
import edu.umd.cs.findbugs.BugPattern;

public class FilterPatternAction implements IObjectActionDelegate {

	private BugGroup group;
	private CommonNavigator navigator;

	public FilterPatternAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		if (targetPart instanceof CommonNavigator) {
			navigator = (CommonNavigator) targetPart;
		}
	}

	public void run(IAction action) {
		final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
		String lastUsedFilter = store.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
		Set<String> sortedIds = new TreeSet<String>();
		sortedIds.addAll(Arrays.asList(lastUsedFilter.split(",")));
		String patternType = getPatternType();
		if(patternType != null) {
			if(!sortedIds.contains(patternType)) {
				sortedIds.add(patternType);
			} else {
				sortedIds.remove(patternType);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String string : sortedIds) {
			sb.append(string).append(",");
		}
		if(sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		store.setValue(FindBugsConstants.LAST_USED_EXPORT_FILTER, sb.toString());
		BugContentProvider provider = BugContentProvider.getProvider(navigator
				.getNavigatorContentService());
		provider.refreshFilters();
		CommonViewer viewer = navigator.getCommonViewer();
		Object[] expandedElements = viewer.getExpandedElements();
		viewer.refresh(true);
		viewer.setExpandedElements(expandedElements);
		group = null;
	}

	private String getPatternType() {
		if(group.getType() == GroupType.Pattern){
			BugPattern pattern = (BugPattern) group.getData();
			return pattern.getAbbrev();
		} else if(group.getType() == GroupType.PatternType){
			return (String) group.getData();
		}
		return null;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			group = null;
			action.setEnabled(false);
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		if (ss.size() != 1) {
			group = null;
			action.setEnabled(false);
			return;
		}
		Object firstElement = ss.getFirstElement();
		if (!(firstElement instanceof BugGroup)) {
			group = null;
			action.setEnabled(false);
			return;
		}
		group = (BugGroup) firstElement;
		action.setEnabled(true);
	}

}
