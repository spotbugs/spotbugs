/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey Loskutov
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

/**
 * @author Andrei
 */
public class BugGroup implements IAdaptable, IActionFilter, Comparable<BugGroup> {

    private String shortDescription;

    private final Set<Object> children;

    private Set<IMarker> allMarkers;

    @CheckForNull
    private Object parent;

    @CheckForNull
    private final Object identifier;

    @Nonnull
    private final GroupType type;

    public BugGroup(Object parent, Object identifier, @Nonnull GroupType type) {
        super();
        this.parent = parent;
        Assert.isNotNull(type, "Group type cannot be null");
        this.type = type;
        this.identifier = identifier;
        this.children = new HashSet<Object>();
        this.allMarkers = new HashSet<IMarker>();
        if (parent instanceof BugGroup) {
            BugGroup bugGroup = (BugGroup) parent;
            bugGroup.addChild(this);
        }
    }

    public Object[] getChildren() {
        if (children.size() == 0) {
            // TODO should we ask content provider to create children???
            return allMarkers.toArray(new Object[allMarkers.size()]);
        }
        return children.toArray(new Object[children.size()]);
    }

    public Set<IMarker> getAllMarkers() {
        return allMarkers;
    }

    public boolean contains(Object elt) {
        if (elt instanceof IMarker) {
            return allMarkers.contains(elt);
        }
        return children.contains(elt);
    }

    /**
     * @return the short group description
     */
    @SuppressWarnings("unchecked")
    public String getShortDescription() {
        if (shortDescription == null) {
            switch (type) {
            case Marker:
                break;
            case Workspace:
                return "Overall issues number: ";
            case WorkingSet:
                return "Overall issues number: ";
            default:
                @SuppressWarnings("rawtypes")
                MarkerMapper mapper = type.getMapper();
                if(identifier == null) {
                    shortDescription = mapper.getShortDescription(this);
                } else {
                    shortDescription = mapper.getShortDescription(identifier);
                }
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

    public int getMarkersCount() {
        return allMarkers.size();
    }

    void addChild(Object child) {
        children.add(child);
        if (BugContentProvider.DEBUG) {
            System.out.println("Adding child: " + child + " to " + this);
        }
    }

    boolean removeChild(BugGroup child) {
        boolean removed = children.remove(child);
        if (BugContentProvider.DEBUG) {
            System.out.println("Removing child: " + child + " from " + this);
        }
        if (children.size() == 0) {
            if (getMarkersCount() > 0) {
                removeMarkers(allMarkers);
            }
        } else {
            removeMarkers(child.getAllMarkers());
        }
        child.dispose();
        return removed;
    }

    void addMarker(IMarker marker) {
        boolean added = allMarkers.add(marker);
        if (BugContentProvider.DEBUG) {
            System.out.println("Adding marker: " + marker.getId() + " to " + this + ", new? " + added);
        }
    }

    void removeMarker(IMarker marker) {
        if (allMarkers.isEmpty()) {
            return;
        }
        if (BugContentProvider.DEBUG) {
            System.out.println("Removing marker: " + marker.getId() + " from " + this);
        }
        allMarkers.remove(marker);
        if (parent instanceof BugGroup) {
            ((BugGroup) parent).removeMarker(marker);
        }
    }

    private void removeMarkers(Set<IMarker> markers) {
        if (markers.isEmpty() || allMarkers.isEmpty()) {
            return;
        }
        if (BugContentProvider.DEBUG) {
            for (IMarker marker : markers) {
                System.out.println("Removing marker: " + marker.getId() + " from " + this);
            }
        }
        if (markers == allMarkers) {
            allMarkers.clear();
        } else {
            allMarkers.removeAll(markers);
        }
        if (parent instanceof BugGroup) {
            ((BugGroup) parent).removeMarkers(markers);
        }
    }

    void setMarkers(Set<IMarker> markers) {
        allMarkers = markers;
    }

    @Override
    public String toString() {
        return shortDescription == null ? getShortDescription() : shortDescription;
    }

    public Object getAdapter(Class adapter) {
        if (identifier != null && adapter.isAssignableFrom(identifier.getClass())) {
            return identifier;
        }
        if (BugGroup.class == adapter) {
            return this;
        }
        if (ITaskListResourceAdapter.class == adapter) {
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=246409
            return null;
        }
        // followed caused more troubles then uses.
        // if(self instanceof IAdaptable){
        // IAdaptable adaptable = (IAdaptable) self;
        // return adaptable.getAdapter(adapter);
        // }
        return null;
    }

    @Nonnull
    public GroupType getType() {
        return type;
    }

    @CheckForNull
    public Object getData() {
        return identifier;
    }

    void dispose() {
        children.clear();
        allMarkers.clear();
        parent = null;
    }

    public boolean testAttribute(Object target, String name, String value) {
        if ("type".equals(name)) {
            String groupType = getType().name();
            boolean match = groupType.equals(value);
            if(match) {
                return match;
            }
            if(value.indexOf('|') > 0){
                String[] split = value.split("\\|");
                for (String string : split) {
                    if(groupType.equals(string)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if (!(obj instanceof BugGroup)) {
            return false;
        }
        BugGroup bugGroup = (BugGroup) obj;
        if (!equals(parent, bugGroup.parent)) {
            return false;
        }
        if (!equals(type, bugGroup.type)) {
            return false;
        }
        if (!equals(identifier, bugGroup.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (identifier == null) {
            return super.hashCode();
        }
        return identifier.hashCode();
    }

    private static boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1 == o2 || o1.equals(o2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compareTo(BugGroup o) {
        if(identifier == null || !getType().equals(o.getType())){
            return 0;
        }
        if(identifier instanceof Comparable<?>){
            return ((Comparable) identifier).compareTo(o.identifier);
        }
        return 0;
    }
}
