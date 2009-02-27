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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.marker.FindBugsMarker.Priority;

/**
 * @author Andrei
 */
public class BugGroup implements IAdaptable, IActionFilter {

	private String shortDescription;
	private final Set<Object> children;
	private Set<IMarker> allMarkers;
	private Object parent;
	private final FindBugsMarker.Priority priority;
	private final Object self;
	private final GroupType type;

	BugGroup(Object parent, Object self, GroupType type, Priority priority) {
		super();
		this.parent = parent;
		Assert.isNotNull(type, "Group type cannot be null");
		this.type = type;
		this.self = self == null? this : self;
		this.children = new HashSet<Object>(); /*new TreeSet<Object>(new Comparator<Object>() {
			public int compare(Object m1, Object m2) {
				if (m1 instanceof IMarker && m2 instanceof IMarker) {
					return BugPrioritySorter.compareMarkers((IMarker) m1, (IMarker) m2);
				}
				if (m1 instanceof BugGroup && m2 instanceof BugGroup) {
					return BugPrioritySorter.compareGroups((BugGroup) m1, (BugGroup) m2);
				}
				return 0;
			}
		});*/
		this.priority = priority == null? Priority.Unknown : priority;
		this.allMarkers = new HashSet<IMarker>();
		if(parent instanceof BugGroup){
			BugGroup bugGroup = (BugGroup) parent;
			bugGroup.addChild(this);
		}
	}

	public Object [] getChildren() {
		if(children.size() == 0){
			// TODO should we ask content provider to create children???
			return allMarkers.toArray(new Object[allMarkers.size()]);
		}
		return children.toArray(new Object[children.size()]);
	}

	public Set<IMarker> getAllMarkers() {
		return allMarkers;
	}

	public boolean contains(Object elt){
		if(elt instanceof IMarker){
			return allMarkers.contains(elt);
		}
		return children.contains(elt);
	}

	/**
	 * @return the short group description
	 */
	public String getShortDescription() {
		if(shortDescription == null){
			switch (type) {
			case Marker:
				break;
			case Workspace:
				return "Overall issues number: ";
			case WorkingSet:
				return "Overall issues number: ";
			default:
				shortDescription = type.getMapper().getShortDescription(self);
			break;
			}
		}
		return shortDescription;
	}

	public Object getParent() {
		return parent;
	}

	public int size() {
		return children.size();
	}

	public int getMarkersCount(){
		return allMarkers.size();
	}

	void addChild(Object child) {
		children.add(child);
		if(BugContentProvider.DEBUG) {
			System.out.println("Adding child: " + child + " to " + this);
		}
	}

	boolean removeChild(BugGroup child) {
		boolean removed = children.remove(child);
		if(BugContentProvider.DEBUG) {
			System.out.println("Removing child: " + child + " from " + this);
		}
		if(children.size() == 0){
			if(getMarkersCount() > 0) {
				removeMarkers(allMarkers);
			}
		} else {
			removeMarkers(child.getAllMarkers());
		}
		child.dispose();
		return removed;
	}

	void addMarker(IMarker marker){
		boolean added = allMarkers.add(marker);
		if(BugContentProvider.DEBUG) {
			System.out.println("Adding marker: " + marker.getId() + " to " + this + ", new? " + added);
		}
	}

	void removeMarker(IMarker marker){
		if(allMarkers.isEmpty()){
			return;
		}
		if(BugContentProvider.DEBUG) {
			System.out.println("Removing marker: " + marker.getId() + " from " + this);
		}
		allMarkers.remove(marker);
		if(parent instanceof BugGroup){
			((BugGroup) parent).removeMarker(marker);
		}
	}

	private void removeMarkers(Set<IMarker> markers){
		if(markers.isEmpty() || allMarkers.isEmpty()){
			return;
		}
		if(BugContentProvider.DEBUG) {
			for (IMarker marker : markers) {
				System.out.println("Removing marker: " + marker.getId() + " from " + this);
			}
		}
		if(markers == allMarkers){
			allMarkers.clear();
		} else {
			allMarkers.removeAll(markers);
		}
		if(parent instanceof BugGroup){
			((BugGroup) parent).removeMarkers(markers);
		}
	}

	void setMarkers(Set<IMarker> markers){
		allMarkers = markers;
	}



	@Override
	public String toString() {
		return shortDescription == null? getShortDescription() : shortDescription;
	}

	public FindBugsMarker.Priority getPriority() {
		return priority;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if(adapter.isAssignableFrom(self.getClass())){
			return self;
		}
		if(ITaskListResourceAdapter.class == adapter){
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=246409
			return null;
		}
		// followed caused more troubles then uses.
//		if(self instanceof IAdaptable){
//			IAdaptable adaptable = (IAdaptable) self;
//			return adaptable.getAdapter(adapter);
//		}
		return null;
	}

	public GroupType getType() {
		return type;
	}

	public Object getData(){
		return self;
	}

	void dispose(){
		children.clear();
		allMarkers.clear();
		parent = null;
	}

	public boolean testAttribute(Object target, String name, String value) {
		if("type".equals(name)){
			return getType().name().equals(value);
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof BugGroup)){
			return false;
		}
		BugGroup bugGroup = (BugGroup) obj;
		if(!equals(parent, bugGroup.parent)){
			return false;
		}
		if (self == null) {
			return super.equals(obj);
		}
		return self.equals(bugGroup.self);
	}

	@Override
	public int hashCode() {
		if (self == null) {
			return super.hashCode();
		}
		return self.hashCode();
	}

	private boolean equals(Object o1, Object o2){
		if(o1 == null){
			return o2 == null;
		}
		return o1.equals(o2);
	}
}
