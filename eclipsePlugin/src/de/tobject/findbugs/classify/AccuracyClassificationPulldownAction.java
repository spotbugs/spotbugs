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

import java.util.Iterator;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Pulldown toolbar action for classifying a FindBugs warning
 * as "bug" or "not a bug". 
 * 
 * @author David Hovemeyer
 */
public class AccuracyClassificationPulldownAction
		//extends Action
		implements IWorkbenchWindowPulldownDelegate2 {
	
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
			
			// TODO: populate the menu
		}
		return menu;
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
		
//		System.out.println("Init called");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// TODO Auto-generated method stub
		System.out.println("Classifying a warning!");
		
		//action.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		System.out.println("Selection is " + selection.getClass().getName());
		
		Marker marker = getMarkerFromSelection(selection);
		
		if (marker == null)
			// FIXME: should really ensure that action can only
			// be run when a single findbugs problem marker is
			// selected.
			return;
		

		try {
			String markerType = marker.getType();
			System.out.println("Marker type is " + markerType);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get marker type");
		}
	}

	/**
	 * @param selection
	 */
	private Marker getMarkerFromSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;
			
			for (Iterator i = structuredSelection.iterator(); i.hasNext(); ) {
				Object o = i.next();
				System.out.println("\tSelection element: " + o.getClass().getName());
				if (o instanceof Marker) {
					return (Marker) o;
				}
			}
		}
		
		return null;
	}

}
