package de.tobject.findbugs.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;

import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupSelectionDialog;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

public class OpenGroupDialogAction implements IViewActionDelegate {

	private CommonNavigator navigator;

	public void init(IViewPart view) {
		if (view instanceof CommonNavigator) {
			navigator = (CommonNavigator) view;
		}
	}

	public void run(IAction action) {
		if (navigator == null) {
			return;
		}
		BugContentProvider provider = BugContentProvider.getProvider(navigator
				.getNavigatorContentService());
		List<GroupType> list = provider.getGrouping().asList();
		GroupSelectionDialog dialog = new GroupSelectionDialog(navigator.getSite()
				.getShell(), list);
		dialog.setTitle("Bug Group Configuration");
		int result = dialog.open();
		if (result != Window.OK) {
			return;
		}
		Grouping grouping = Grouping.createFrom(dialog.getGroups());
		if (grouping == null) {
			return;
		}
		provider.setGrouping(grouping);
		provider.reSetInput();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if(navigator == null){
			action.setEnabled(false);
			return;
		}
		BugContentProvider provider = BugContentProvider.getProvider(navigator
				.getNavigatorContentService());
		action.setEnabled(provider.getGrouping() != null);
	}

}
