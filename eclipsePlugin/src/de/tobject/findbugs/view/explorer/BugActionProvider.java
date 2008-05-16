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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import de.tobject.findbugs.FindbugsPlugin;

public class BugActionProvider extends CommonActionProvider {

	public static class MyAction extends Action {
		private IMarker marker;

		public MyAction() {
			super();
		}

		@Override
		public void run() {
			try {
				IDE.openEditor(FindbugsPlugin.getActiveWorkbenchWindow().getActivePage(),
						marker, true);
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Cannot open editor for marker: " + marker);
			}
		}

		void setSelection(IMarker sel) {
			marker = sel;
		}
	}

	private MyAction doubleClickAction;

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);

		doubleClickAction = new MyAction();
		// only if doubleClickAction must know tree selection:
//		aSite.getStructuredViewer().addSelectionChangedListener(doubleClickAction);
//		aSite.getStructuredViewer().addDoubleClickListener(doubleClickAction);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);

		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof IMarker) {
			// forward doubleClick to doubleClickAction
			doubleClickAction.setSelection((IMarker) selection.getFirstElement());
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN,
					doubleClickAction);
		}
	}
}
