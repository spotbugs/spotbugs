/**
 *
 */
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	BugPatternGroup(IResource parent, String shortPatternDescription) {
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

	void addmarker(IMarker marker) {
		children.add(marker);
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
