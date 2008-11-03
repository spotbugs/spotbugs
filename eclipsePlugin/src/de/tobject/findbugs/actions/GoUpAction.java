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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.view.explorer.BugPatternGroup;

public class GoUpAction implements IViewActionDelegate {
	private CommonNavigator navigator;

	public void init(IViewPart view) {
		if(view instanceof CommonNavigator) {
			navigator = (CommonNavigator) view;
		}
	}

	public void run(IAction action) {
		if(action.isEnabled()) {
			CommonViewer viewer = navigator.getCommonViewer();
			Object input = viewer.getInput();
			if(input instanceof IProject){
				IProject project = (IProject) input;
				// if the parent of a project before we've going into was a working set,
				// then we have somehow restore this state
				Object data = null;
				try {
					data = project.getSessionProperty(GoIntoAction.KEY_OLD_PARENT);
					project.setSessionProperty(GoIntoAction.KEY_OLD_PARENT, null);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e,
					"Failed to retrieve working set");
				}
				if(data != null) {
					viewer.setInput(data);
				} else {
					viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
				}
			} else if (input instanceof BugPatternGroup) {
				BugPatternGroup group = (BugPatternGroup) input;
				// if the parent of a project before we've going into was a working set,
				// then we have somehow restore this state
				Object data = group.getParent();
				if(data != null) {
					viewer.setInput(data);
				} else {
					viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
				}
			}
			action.setEnabled(isEnabled());
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(isEnabled());
	}

	private boolean isEnabled() {
		if(navigator == null){
			return false;
		}
		Object input = navigator.getCommonViewer().getInput();
		return !(input instanceof IWorkspaceRoot) && !(input instanceof IWorkingSet);
	}



}
