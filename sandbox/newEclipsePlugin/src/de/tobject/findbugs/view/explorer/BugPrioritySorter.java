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

import java.text.Collator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

public class BugPrioritySorter extends ViewerSorter {

	public BugPrioritySorter() {
		super();
	}

	public BugPrioritySorter(Collator collator) {
		super(collator);
	}

    @Override
	public int compare(Viewer viewer, Object e1, Object e2) {
        int cat1 = category(e1);
        int cat2 = category(e2);

        if (cat1 != cat2) {
			return cat1 - cat2;
		}

        if(e1 instanceof IMarker && e2 instanceof IMarker) {
			return compareMarkers((IMarker) e1, (IMarker) e2);
        }

        // Sorts groups on priority first, then on group name
        if(e1 instanceof BugGroup && e2 instanceof BugGroup) {
        	return compareGroups((BugGroup)e1, (BugGroup)e2);
        }
        return super.compare(viewer, e1, e2);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
     */
    @Override
    public int category(Object element) {
    	if(element instanceof IMarker) {
    		return 1;
    	}
    	if(element instanceof BugGroup) {
    		return 5;
    	}
    	// return smallest value: 0
    	return super.category(element);
    }

	/**
	 * Sorts bug groups on severity first, then on bug pattern name.
	 */
	static int compareGroups(BugGroup m1, BugGroup m2) {
		int result = m1.getPriority().compareTo(m2.getPriority());
		if(result == 0){
			return m1.getShortDescription().compareToIgnoreCase(m2.getShortDescription());
		}
		return result;
	}

	/**
	 * Sorts markers on priority first, then on name if requested
	 * @param m1
	 * @param m2
	 * @return
	 */
	static int compareMarkers(IMarker m1, IMarker m2) {
		if(m1 == null || m2 == null || !m1.exists() || !m2.exists()){
			return 0;
		}
		try {
			int ordinal1 = FindBugsMarker.Priority.ordinal(m1.getType());
			int ordinal2 = FindBugsMarker.Priority.ordinal(m2.getType());
			int result = ordinal1 - ordinal2;
			if(result != 0) {
				return result;
			}
			String a1 = m1.getAttribute(IMarker.MESSAGE, "");
			String a2 = m2.getAttribute(IMarker.MESSAGE, "");
			return a1.compareToIgnoreCase(a2);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Sort error");
		}
		return 0;
	}

}
