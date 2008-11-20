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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import de.tobject.findbugs.FindbugsPlugin;

public class BugActionProvider extends CommonActionProvider {

	static class MyAction extends Action implements ISelectionChangedListener {
		private IMarker marker;
		private IFile file;

		@Override
		public void run() {
			if(marker == null && file == null){
				return;
			}
			try {
				if(marker != null) {
					IDE.openEditor(FindbugsPlugin.getActiveWorkbenchWindow().getActivePage(),
							marker, true);
				} else {
					IDE.openEditor(FindbugsPlugin.getActiveWorkbenchWindow().getActivePage(),
							file, true);
				}
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Cannot open editor for marker: " + marker);
			}
		}

		void setSelection(IMarker sel) {
			marker = sel;
		}

		public void selectionChanged(SelectionChangedEvent event) {
			resetSelection();
			ISelection selection = event.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (ss.size() == 1) {
					Object firstElement = ss.getFirstElement();
					if(firstElement instanceof IMarker) {
						// forward doubleClick to doubleClickAction
						setSelection((IMarker) firstElement);
					} else if (firstElement instanceof BugGroup){
						BugGroup group = (BugGroup) firstElement;
						Object data = group.getData();
						if(data instanceof IAdaptable){
							IAdaptable adaptable = (IAdaptable) data;
							Object adapter = adaptable.getAdapter(IResource.class);
							if(adapter instanceof IFile){
								file = (IFile) adapter;
							}
						}
					}
				}
			}
		}

		private void resetSelection() {
			marker = null;
			file = null;
		}

	}

	boolean hasContributedToViewMenu;
	private MyAction doubleClickAction;
	private ICommonActionExtensionSite site;

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		site = aSite;
		super.init(aSite);

		doubleClickAction = new MyAction();
		// only if doubleClickAction must know tree selection:
		aSite.getStructuredViewer().addSelectionChangedListener(doubleClickAction);
		// aSite.getStructuredViewer().addDoubleClickListener(doubleClickAction);
	}

	@Override
	public void dispose() {
		site.getStructuredViewer().removeSelectionChangedListener(doubleClickAction);
		super.dispose();
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);

		if (!hasContributedToViewMenu) {
			IMenuManager menuManager = actionBars.getMenuManager();

			// XXX dirty hack to get rid of "Top Level Elements" menu which is meaningless
			// for us
			IContributionItem[] items = menuManager.getItems();
			for (IContributionItem item : items) {
				if (item instanceof MenuManager) {
					MenuManager mm = (MenuManager) item;
					if ("Top Level Elements".equals(mm.getMenuText())
							|| "&Top Level Elements".equals(mm.getMenuText())) {
						menuManager.remove(item);
						break;
					}
				}
			}
			IContributionItem item = menuManager.find("findBugsEclipsePlugin.toggleGrouping.groupDialog");
			if(item != null){
				menuManager.remove(item);
				menuManager.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, item);
			}
			IMenuManager mm = menuManager.findMenuUsingPath("bugExplorer.menu.group");
			if (mm != null) {
				menuManager.remove(mm);
				menuManager.insertBefore(IWorkbenchActionConstants.MB_ADDITIONS, mm);
			}

			hasContributedToViewMenu = true;
		}
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, doubleClickAction);
	}

//	@Override
//	public void updateActionBars() {
//		// IStructuredSelection selection = (IStructuredSelection) getContext()
//		// .getSelection();
//		// if (selection.size() == 1 && selection.getFirstElement() instanceof IMarker) {
//		// // forward doubleClick to doubleClickAction
//		// doubleClickAction.setSelection((IMarker) selection.getFirstElement());
//		// }
//	}
}
