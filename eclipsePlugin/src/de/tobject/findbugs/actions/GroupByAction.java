package de.tobject.findbugs.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;

import de.tobject.findbugs.view.explorer.BugContentProvider;
import de.tobject.findbugs.view.explorer.GroupType;
import de.tobject.findbugs.view.explorer.Grouping;

public class GroupByAction implements IViewActionDelegate {


	private static final String ACTION_ID_PREFIX = "findBugsEclipsePlugin.toggleGrouping.";
	private CommonNavigator navigator;

	public void init(IViewPart view) {
		if(view instanceof CommonNavigator) {
			navigator = (CommonNavigator) view;
		}
	}

	public void run(IAction action) {
		if(navigator == null) {
			return;
		}
		Grouping grouping = getGrouping(action.getId());
		if(grouping == null){
			return;
		}
		BugContentProvider provider = BugContentProvider.getProvider(navigator
				.getNavigatorContentService());
		provider.setGrouping(grouping);
		provider.reSetInput();
	}



	private Grouping getGrouping(String id) {
		if(id == null){
			return null;
		}
		if(!id.startsWith(ACTION_ID_PREFIX)){
			return null;
		}
		id = id.substring(ACTION_ID_PREFIX.length());
		String[] typesArr = id.split("\\.");
		List<GroupType> types = new ArrayList<GroupType>();
		for (String string : typesArr) {
			GroupType type = GroupType.valueOf(string);
			types.add(type);
		}
		return Grouping.createFrom(types);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// noop
	}

}
