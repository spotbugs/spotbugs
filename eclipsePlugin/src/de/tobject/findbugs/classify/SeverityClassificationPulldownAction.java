/*
 * Created on Feb 8, 2005
 */
package de.tobject.findbugs.classify;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

/**
 * Pulldown menu action for classifying warning severity.
 * 
 * @author David Hovemeyer
 */
public class SeverityClassificationPulldownAction implements
		IWorkbenchWindowPulldownDelegate2 {

	private Menu menu;
	
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
		MenuItem s1 = new MenuItem(menu, SWT.RADIO);
		s1.setText("1 (Least Severe)");
		MenuItem s2 = new MenuItem(menu, SWT.RADIO);
		s2.setText("2");
		MenuItem s3 = new MenuItem(menu, SWT.RADIO);
		s3.setText("3");
		MenuItem s4 = new MenuItem(menu, SWT.RADIO);
		s4.setText("4");
		MenuItem s5 = new MenuItem(menu, SWT.RADIO);
		s5.setText("5 (Most Severe)");
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
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
