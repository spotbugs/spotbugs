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
			return compare(viewer, marker1, marker2);
        }

        if(e1 instanceof BugPatternGroup) {
        	IMarker marker1 = ((BugPatternGroup)e1).getFirstElement();
        	IMarker marker2 = ((BugPatternGroup)e2).getFirstElement();
        	return compare(viewer, marker1, marker2);
        }
        return super.compare(viewer, e1, e2);
    }

	private int compare(Viewer viewer, IMarker marker1, IMarker marker2) {
		try {
			int ordinal1 = FindBugsMarker.Priority.ordinal(marker1.getType());
			int ordinal2 = FindBugsMarker.Priority.ordinal(marker2.getType());
			return ordinal1 - ordinal2;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Sort error");
		}
		return super.compare(viewer, marker1, marker2);
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
