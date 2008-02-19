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
import org.eclipse.core.resources.IResource;
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

        if(e1 instanceof IMarker) {
			IMarker marker1 = (IMarker) e1;
			IMarker marker2 = (IMarker) e2;
			return compare(marker1, marker2, true);
        }

        // Sorts groups on priority first, then on group name
        if(e1 instanceof BugPatternGroup) {
        	BugPatternGroup group1 = (BugPatternGroup)e1;
        	BugPatternGroup group2 = (BugPatternGroup)e2;
			IMarker marker1 = group1.getFirstElement();
			IMarker marker2 = group2.getFirstElement();
        	int result = compare(marker1, marker2, false);
        	if (result == 0) {
				return group1.getShortPatternDescription().compareTo(
						group2.getShortPatternDescription());
			}
			return result;
        }
        return super.compare(viewer, e1, e2);
    }

    /**
     * Sorts markers on priority first, then on name if requested
     * @param marker1
     * @param marker2
     * @param compareNames
     * @return
     */
	private int compare(IMarker marker1, IMarker marker2, boolean compareNames) {
		try {
			int ordinal1 = FindBugsMarker.Priority.ordinal(marker1.getType());
			int ordinal2 = FindBugsMarker.Priority.ordinal(marker2.getType());
			int result = ordinal1 - ordinal2;
			if(compareNames && result == 0) {
				String a1 = marker1.getAttribute(IMarker.MESSAGE, "");
				String a2 = marker1.getAttribute(IMarker.MESSAGE, "");
				return a1.compareTo(a2);
			}
			return result;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Sort error");
		}
		return 0;
	}

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
     */
    @Override
    public int category(Object element) {
    	if(element instanceof IMarker) {
    		return 1;
    	}
    	if(element instanceof IResource) {
    		return 10;
    	}
    	if(element instanceof BugPatternGroup) {
    		return 5;
    	}
    	// return smallest value: 0
    	return super.category(element);
    }

}
