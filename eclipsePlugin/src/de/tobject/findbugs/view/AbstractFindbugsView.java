/*
 * Contributions to FindBugs
 * Copyright (C) 2007, Andrei Loskutov
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
package de.tobject.findbugs.view;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Andrei Loskutov
 */
public abstract class AbstractFindbugsView extends ViewPart {
	static final String DETAILS_VIEW_IMG = "detailsView.png";
	static final String USER_ANNOTATIONS_VIEW_IMG = "annotationsView.png";
	static final String TREE_VIEW_IMG = "treeView.png";
	static final String PERSPECTIVE_IMG = "buggy-tiny.png";

	private Composite root;
	private Action actionShowDetailsView;
	private Action actionShowBugTreeView;
	private Action actionShowAnnotationsView;
	private Action actionShowPerspective;

	public AbstractFindbugsView() {
		super();
	}

	/**
	 * activates view if it is not visible
	 */
	final protected void activate() {
		if (!isVisible()) {
			getSite().getPage().activate(this);
		}
	}

	final protected boolean isVisible() {
		return getSite().getPage().isPartVisible(this);
	}

	@Override
	public void setFocus() {
		getRootControl().setFocus();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		getRootControl().dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public final void createPartControl(Composite parent) {
		root = createRootControl(parent);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	final protected Composite getRootControl() {
		return root;
	}

	/**
	 * @param parent
	 * @return
	 */
	abstract protected Composite createRootControl(Composite parent);

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(getRootControl());
		getRootControl().setMenu(menu);
		// TODO
		// getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	protected void makeActions() {
		actionShowAnnotationsView = new Action() {
			@Override
			public void run() {
				showUserAnnotationView();
			}
		};
		configureAction(actionShowAnnotationsView, "Show Bug Annotations View",
				"Show Annotations View", USER_ANNOTATIONS_VIEW_IMG);

		actionShowBugTreeView = new Action() {
			@Override
			public void run() {
				showBugTreeView();
			}
		};
		configureAction(actionShowBugTreeView, "Show Bug Tree View",
				"Show BugTree View", TREE_VIEW_IMG);

		actionShowDetailsView = new Action() {
			@Override
			public void run() {
				showDetailsView();
			}
		};
		configureAction(actionShowDetailsView, "Show Bug Details View",
				"Show Bug Details View", DETAILS_VIEW_IMG);

		actionShowPerspective = new Action() {
			@Override
			public void run() {
				showPerspective();
			}
		};
		configureAction(actionShowPerspective, "Switch to FindBugs Perspective",
				"Switch to FindBugs Perspective", PERSPECTIVE_IMG);
	}


	protected void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionShowPerspective);
		manager.add(new Separator());

		if(!(this instanceof UserAnnotationsView)) {
			manager.add(actionShowAnnotationsView);
		}
		if(!(this instanceof BugTreeView)) {
			manager.add(actionShowBugTreeView);
		}
		if(!(this instanceof DetailsView)) {
			manager.add(actionShowDetailsView);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute there actions here
		manager.add(new Separator("additions")); //$NON-NLS-1$
	}

	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionShowPerspective);
		manager.add(new Separator());

		if(!(this instanceof UserAnnotationsView)) {
			manager.add(actionShowAnnotationsView);
		}
		if(!(this instanceof BugTreeView)) {
			manager.add(actionShowBugTreeView);
		}
		if(!(this instanceof DetailsView)) {
			manager.add(actionShowDetailsView);
		}
	}

	protected void hookDoubleClickAction() {
		// TODO should refactor dirty code in views to common
	}

	protected final void configureAction(Action action, String textKey,
			String tooltipKey, String imageKey) {
		action.setText(FindbugsPlugin.getResourceString(textKey));
		action.setToolTipText(FindbugsPlugin.getResourceString(tooltipKey));
		action.setImageDescriptor(FindbugsPlugin.getDefault()
				.getImageDescriptor(imageKey));
	}

	/**
	 * Get the IWorkbenchSiteProgressService for the receiver.
	 *
	 * @return IWorkbenchSiteProgressService or <code>null</code>.
	 */
	protected IWorkbenchSiteProgressService getProgressService() {
		IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
				.getAdapter(IWorkbenchSiteProgressService.class);
		return service;
	}

	/**
	 * @return instance of annotations view or null if view couldn't be opened
	 */
	static IViewPart showUserAnnotationView() {
		IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage();
		try {
			return page.showView(FindbugsPlugin.USER_ANNOTATIONS_VIEW_ID);
		} catch (PartInitException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not show bug annotations view");
		}
		return null;
	}

	/**
	 * @return instance of annotations view or null if view couldn't be opened
	 */
	static IViewPart showDetailsView() {
		IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage();
		try {
			return page.showView(FindbugsPlugin.DETAILS_VIEW_ID);
		} catch (PartInitException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not show bug details view");
		}
		return null;
	}

	/**
	 * @return instance of annotations view or null if view couldn't be opened
	 */
	static IViewPart showBugTreeView() {
		IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage();
		try {
			return page.showView(FindbugsPlugin.TREE_VIEW_ID);
		} catch (PartInitException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not show bug tree view");
		}
		return null;
	}


	/**
	 *
	 */
	final void showPerspective() {
		IWorkbenchPage page = getSite().getPage();
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		IAdaptable input;
		if (page != null) {
			input = page.getInput();
		} else {
			input= ResourcesPlugin.getWorkspace().getRoot();
		}
		try {
			 PlatformUI.getWorkbench().showPerspective(FindBugsPerspectiveFactory.ID, window, input);
		} catch (WorkbenchException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Failed to open FindBugs Perspective");
		}
	}
}
