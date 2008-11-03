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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.view.explorer.BugPatternGroup;


public class GoIntoAction implements IViewActionDelegate {

	static final QualifiedName KEY_OLD_PARENT = new QualifiedName("old", "parent");
	private CommonNavigator navigator;
	private Object selectedElement;

	public void init(IViewPart view) {
		if(view instanceof CommonNavigator) {
			navigator = (CommonNavigator) view;
		}
	}

	public void run(IAction action) {
		if(action.isEnabled() && navigator != null && selectedElement != null) {
			CommonViewer viewer = navigator.getCommonViewer();
			if(selectedElement instanceof IProject){
				IProject project = (IProject) selectedElement;
				try {
					project.setSessionProperty(KEY_OLD_PARENT, viewer.getInput());
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Failed to remember working set");
				}
			}
			viewer.setInput(selectedElement);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if(!(selection instanceof IStructuredSelection)){
			action.setEnabled(false);
			return;
		}
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if(ssel.size() != 1){
			action.setEnabled(false);
			return;
		}
		Object element = ssel.getFirstElement();
		if(!(element instanceof IProject || element instanceof BugPatternGroup)){
			action.setEnabled(false);
			return;
		}
		action.setEnabled(true);
		selectedElement = element;
	}
}
