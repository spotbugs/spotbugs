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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * @author Andrei
 */
public class BugPatternGroup {

	private final String shortPatternDescription;
	private final List<IMarker> children;
	private final IResource parent;

	private BugPatternGroup(IResource parent, String shortPatternDescription) {
		super();
		this.parent = parent;
		this.shortPatternDescription = shortPatternDescription;
		this.children = new ArrayList<IMarker>();
	}

	public IMarker [] getChildren() {
		return children.toArray(new IMarker[children.size()]);
	}

	/**
	 * @return the shortPatternDescription
	 */
	public String getShortPatternDescription() {
		return shortPatternDescription;
	}

	/**
	 * @return the parent
	 */
	public IResource getParent() {
		return parent;
	}

	public IMarker getFirstElement() {
		if(children.size() > 0) {
			return children.get(0);
		}
		return null;
	}

	public int size() {
		return children.size();
	}

	private void addmarker(IMarker marker) {
		children.add(marker);
	}

	/**
	 * Sorts bug groups on severity first, then on bug pattern name.
	 */
	private void sort() {
		Collections.sort(children,
		new Comparator<IMarker>() {
			public int compare(IMarker m1, IMarker m2) {
				try {
					int ordinal1 = FindBugsMarker.Priority.ordinal(m1.getType());
					int ordinal2 = FindBugsMarker.Priority.ordinal(m2.getType());
					int result = ordinal1 - ordinal2;
					if(result != 0) {
						return result;
					}
					String a1 = m1.getAttribute(IMarker.MESSAGE, "");
					String a2 = m1.getAttribute(IMarker.MESSAGE, "");
					return a1.compareTo(a2);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e, "Sort error");
				}
				return 0;
			}
		});
	}

	static BugPatternGroup [] createGroups(IResource parent) {
		IMarker[] markers = getMarkers(parent);
		Map<String, BugPatternGroup> groups = new HashMap<String, BugPatternGroup>();
		for (IMarker marker : markers) {
			String attribute = marker.getAttribute(FindBugsMarker.PATTERN_DESCR_SHORT, "unknown");
			BugPatternGroup group = groups.get(attribute);
			if(group == null) {
				group = new BugPatternGroup(parent, attribute);
				groups.put(attribute, group);
			}
			group.addmarker(marker);
		}
		Set<Entry<String,BugPatternGroup>> entrySet = groups.entrySet();
		for (Entry<String, BugPatternGroup> entry : entrySet) {
			entry.getValue().sort();
		}
		return groups.values().toArray(new BugPatternGroup[groups.size()]);
	}

	static IMarker [] getMarkers(IResource resource) {
		try {
			IMarker[] markerArr = resource.findMarkers(FindBugsMarker.NAME, true,
					IResource.DEPTH_INFINITE);
			return markerArr;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Core exception on getElements for input: " + resource);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return shortPatternDescription;
	}

}
