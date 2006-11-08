/*
 * Created on Feb 8, 2005
 */
package de.tobject.findbugs.classify;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugProperty;

/**
 * Pulldown menu action for classifying warning severity.
 * 
 * @author David Hovemeyer
 */
public class SeverityClassificationPulldownAction implements
		IWorkbenchWindowPulldownDelegate2 {

	private Menu menu;
	private MenuItem[] severityItemList;
	private BugInstance bugInstance;
	
	private static final String[] SEVERITY_LABEL_LIST =
			{"1 (Least Severe)", "2", "3", "4", "5 (Most Severe)"};
	
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
	 * Fill the drop-down menu.
	 * We allow the user to choose a severity from 1 (least severe)
	 * to 5 (most severe).  Default is 3.
	 */
	private void fillMenu() {
		// Create a selection listener to handle when the
		// user selects a warning severity.
		SelectionListener menuItemSelectionListener = new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				Widget w = e.widget;
				int index;
				for (index = 0; index < severityItemList.length; ++index) {
					if (w == severityItemList[index])
						break;
				}
				
				if (index < severityItemList.length) {
					if (bugInstance != null) {
						bugInstance.setProperty(BugProperty.SEVERITY, String.valueOf(index + 1));
					}
				}
			}
		};
		
		severityItemList = new MenuItem[SEVERITY_LABEL_LIST.length];
		for (int i = 0; i < SEVERITY_LABEL_LIST.length; ++i) {
			MenuItem menuItem= new MenuItem(menu, SWT.RADIO);
			menuItem.setText(SEVERITY_LABEL_LIST[i]);
			menuItem.addSelectionListener(menuItemSelectionListener);
			
			severityItemList[i] = menuItem;
		}
		
		// Keep menu in sync with current BugInstance.
		menu.addMenuListener(new MenuAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.MenuAdapter#menuShown(org.eclipse.swt.events.MenuEvent)
			 */
			@Override
			public void menuShown(MenuEvent e) {
				syncMenu();
			}
		});
	}

	/**
	 * Synchronize the menu with the current BugInstance.
	 */
	private void syncMenu() {
		if (bugInstance != null) {
			BugProperty severityProperty = bugInstance.lookupProperty(BugProperty.SEVERITY);
			if (severityProperty != null) {
				try {
					int severity = severityProperty.getValueAsInt();
					if (severity > 0 && severity <= severityItemList.length) {
						selectSeverity(severity);
						return;
					}
				} catch (NumberFormatException e) {
					// Ignore: we'll allow the user to select a valid severity
				}
			}
			
			// We didn't get a valid severity from the BugInstance.
			// So, leave the menu items enabled but cleared, so
			// the user can select a severity.
			resetMenuItems(true);
		} else {
			// No BugInstance - disable all menu items.
			resetMenuItems(false);
		}
	}
	
	/**
	 * Set the menu to given severity level.
	 * 
	 * @param severity the severity level (1..5)
	 */
	private void selectSeverity(int severity) {
		// Severity is 1-based, but the menu item list is 0-based
		int index = severity - 1;
		
		for (int i = 0; i < severityItemList.length; ++i) {
			MenuItem menuItem = severityItemList[i];
			menuItem.setEnabled(true);
			menuItem.setSelection(i == index);
		}
	}

	/**
	 * Reset menu items so they are unchecked.
	 * 
	 * @param enable true if menu items should be enabled,
	 *               false if they should be disabled
	 */
	private void resetMenuItems(boolean enable) {
		for (int i = 0; i < severityItemList.length; ++i) {
			MenuItem menuItem = severityItemList[i];
			menuItem.setEnabled(enable);
			menuItem.setSelection(false);
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// TODO: open classification dialog
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		bugInstance = null;
		IMarker marker = MarkerUtil.getMarkerFromSelection(selection);
		if (marker == null)
			return;
		bugInstance = MarkerUtil.findBugInstanceForMarker(marker);
	}

}
