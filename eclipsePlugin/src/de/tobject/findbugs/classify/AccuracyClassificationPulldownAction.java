/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Feb 7, 2005
 */
package de.tobject.findbugs.classify;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugProperty;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Pulldown toolbar action for classifying a FindBugs warning
 * as "bug" or "not a bug". 
 * 
 * @author David Hovemeyer
 */
public class AccuracyClassificationPulldownAction
		implements IWorkbenchWindowPulldownDelegate2 {
	
	private Menu menu;
	private MenuItem isBugItem;
	private MenuItem notBugItem;
	private IMarker marker;
	private BugInstance bugInstance;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate2#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu = new Menu(parent);
			fillMenu();
		}
		return menu;
	}

	/**
	 * Fill the classification menu.
	 */
	private void fillMenu() {
		isBugItem = new MenuItem(menu, SWT.RADIO);
		isBugItem.setText("Bug");
		notBugItem = new MenuItem(menu, SWT.RADIO);
		notBugItem.setText("Not Bug");
		
		isBugItem.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (bugInstance != null) {
					classifyWarning(bugInstance, true);
				}
			}
		});
		
		notBugItem.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (bugInstance != null) {
					classifyWarning(bugInstance, false);
				}
			}
		});
		
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				// Before showing the menu, sync its contents
				// with the current BugInstance (if any)
				System.out.println("Synchronizing menu!");
				syncMenu();
			}
		});
	}
	
	private void classifyWarning(BugInstance warning, boolean isBug) {
		System.out.println("Classifying warning " + warning.getUniqueId());
		
		BugProperty isBugProp = warning.lookupProperty(BugProperty.IS_BUG);
		if (isBugProp != null) {
			// Warning was previously classified
			if (isBugProp.getValueAsBoolean() == isBug) {
				// No change
				return;
			}
		}
		
		// Warning is being classified for the first time,
		// or the classification is being changed
		warning.setProperty(BugProperty.IS_BUG, isBug ? "true" : "false");
		try {
			FindbugsPlugin.markBugCollectionDirty(marker.getResource().getProject(), true);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not mark bug collection dirty");
		}

		// Currently, we are displaying a marker for this warning.
		// If the user has classified it as a false warning,
		// and false warnings are not being displayed, then we can
		// remove the marker.
		if (!isBug) {
			IProject project = marker.getResource().getProject();
			try {
				UserPreferences userPrefs = FindbugsPlugin.getUserPreferences(project);
				if (!MarkerUtil.displayWarning(warning, userPrefs.getFilterSettings())) {
					System.out.println("Deleting marker for false warning!");
					marker.delete();
				}
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(
						e, "Could not get FindBugs preferences for project");
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		// This just runs once when the action is created
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// TODO: we should create a "Classify Warning" dialog here.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		System.out.println("Selection is " + selection.getClass().getName());
		
		bugInstance = null;
		marker = MarkerUtil.getMarkerFromSelection(selection);
		
		if (marker == null) {
			// No marker selected. 
			return;
		}
		
		System.out.println("Found a marker!");

		bugInstance = MarkerUtil.findBugInstanceForMarker(marker);
		if (bugInstance != null) {
			System.out.println("Found BugInstance for FindBugs warning marker!");
		}
	}

	/**
	 * Update menu to match currently selected BugInstance.
	 */
	private void syncMenu() {
		if (bugInstance != null) {
			isBugItem.setEnabled(true);
			notBugItem.setEnabled(true);
			
			BugProperty isBugProperty = bugInstance.lookupProperty(BugProperty.IS_BUG);
			if (isBugProperty == null) {
				// Unclassified
				isBugItem.setSelection(false);
				notBugItem.setSelection(false);
			} else {
				boolean isBug = isBugProperty.getValueAsBoolean();
				isBugItem.setSelection(isBug);
				notBugItem.setSelection(!isBug);
			}
		} else { 
			// No bug instance, so uncheck and disable the menu items
			//System.out.println("No bug instance found, disabling menu items");
			isBugItem.setEnabled(false);
			notBugItem.setEnabled(false);
			isBugItem.setSelection(false);
			notBugItem.setSelection(false);
		}
	}

}
