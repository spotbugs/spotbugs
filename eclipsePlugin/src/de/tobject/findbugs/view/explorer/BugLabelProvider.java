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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.eclipse.ui.navigator.IDescriptionProvider;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * @author Andrei
 */
public class BugLabelProvider implements ILabelProvider, IDescriptionProvider, ICommonLabelProvider {

	private final WorkbenchLabelProvider wbProvider;
	private ICommonContentExtensionSite config;

	public BugLabelProvider() {
		super();
		wbProvider = new WorkbenchLabelProvider();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof BugGroup) {
			BugGroup group = (BugGroup) element;
			if(group.getType() == GroupType.Class || group.getType() == GroupType.Package
					|| group.getType() == GroupType.Project || group.getType() == GroupType.Marker){
				return wbProvider.getImage(group.getData());
			}
			FindBugsMarker.Priority prio = group.getPriority();
			ImageRegistry imageRegistry = FindbugsPlugin.getDefault().getImageRegistry();
			return imageRegistry.get(prio.iconName());
		}
		if(element instanceof IMarker){
			IMarker marker = (IMarker) element;
			if(!marker.exists()){
				return null;
			}
		}
		return wbProvider.getImage(element);
	}

	public String getText(Object element) {
		if (element instanceof BugGroup) {
			BugGroup group = (BugGroup) element;
//			GroupType type = group.getType();
//			if(type == GroupType.Class || type == GroupType.Package
//					|| type == GroupType.Project || type == GroupType.Marker){
//				return wbProvider.getText(group.getData());
//			}
			return group.getShortDescription() + " (" + group.getMarkersCount() + ")";
		}
		if(element instanceof IMarker){
			IMarker marker = (IMarker) element;
			if(!marker.exists()){
				return null;
			}
		}
		if(element instanceof IStructuredSelection){
			return getDescriptionAndMarkersCount(((IStructuredSelection)element).toArray());
		}
		if(element instanceof Object[]){
			return getDescriptionAndMarkersCount((Object[]) element);
		}
		return wbProvider.getText(element);
	}

	private String getDescriptionAndMarkersCount(Object[] objects) {
		if(objects.length == 0){
			return "Nothing...";
		}
		if(objects.length == 1){
			return getText(objects[0]);
		}
		List<BugGroup> groups = new ArrayList<BugGroup>();
		List<IMarker> markers = new ArrayList<IMarker>();
		for (Object object : objects) {
			if(object instanceof BugGroup){
				groups.add((BugGroup) object);
			} else if(object instanceof IMarker){
				markers.add((IMarker) object);
			}
		}
		if(groups.size() > 1) {
			Collections.sort(groups, new Comparator<BugGroup>(){
				Grouping grouping = getGrouping();
				public int compare(BugGroup o1, BugGroup o2) {
					return grouping.compare(o1.getType(), o2.getType());
				}

			});
		}
		Set<BugGroup> finalGroups = new HashSet<BugGroup>();
		int count = 0;
		while(!groups.isEmpty()){
			BugGroup g1 = groups.remove(groups.size() - 1);
			boolean keepIt = true;
			for (BugGroup g2 : groups) {
				Object parent = g1.getParent();
				while(g2 != parent && parent instanceof BugGroup){
					parent = ((BugGroup) parent).getParent();
				}
				if(g2 == parent){
					keepIt = false;
					break;
				}
			}
			if(keepIt){
				finalGroups.add(g1);
				count += g1.getMarkersCount();
			}
		}
//		Set<IMarker> finalMarkers = new HashSet<IMarker>();
		while(!markers.isEmpty()) {
			IMarker marker = markers.remove(markers.size() - 1);
			boolean keepIt = true;
			for (BugGroup group : finalGroups) {
				if(group.contains(marker)){
					keepIt = false;
					break;
				}
			}
			if(keepIt){
				count ++;
			}
		}
		StringBuffer sb = new StringBuffer("Selection contains ");
		if(count == 1){
			sb.append("exactly one single bug");
		} else {
			sb.append(count).append(" bugs");
		}
		return sb.toString();
	}

	public void init(ICommonContentExtensionSite config) {
		this.config = config;
	}

	Grouping getGrouping(){
		BugContentProvider provider = BugContentProvider.getProvider(config.getService());
		return provider.getGrouping();
	}

	public void addListener(ILabelProviderListener listener) {
		// noop
	}

	public void dispose() {
		// noop
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
		// noop
	}

	public String getDescription(Object anElement) {
		return getText(anElement);
	}


	public void restoreState(IMemento memento) {
		// noop
	}

	public void saveState(IMemento memento) {
		// noop
	}

}
