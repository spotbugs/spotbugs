package de.tobject.findbugs.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;

public class RefreshAction implements IViewActionDelegate {

	private CommonNavigator navigator;

	public void init(IViewPart view) {
		if(view instanceof CommonNavigator) {
			navigator = (CommonNavigator) view;
		}
	}

	public void run(IAction action) {
		if(navigator != null) {
			navigator.getCommonViewer().refresh();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}

}
