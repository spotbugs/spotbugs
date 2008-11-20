/*
	* FindBugs Eclipse Plug-in.
	* Copyright (C) 2003 - 2004, Peter Friese
	* Copyright (C) 2005, University of Maryland
	*
	* This library is free software; you can redistribute it and/or
	* modify it under the terms of the GNU Lesser General Public
	* License as published by the Free Software Foundation; either
	* version 2.1 of the License, or (at your option) any later version.
	*
	* This library is distributed in the hope that it will be useful,
	* but WITHOUT ANY WARRANTY; without even the implied warranty of
	* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
	* Lesser General Public License for more details.
	*
	* You should have received a copy of the GNU Lesser General Public
	* License along with this library; if not, write to the Free Software
	* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
	*/

package de.tobject.findbugs.reporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
	* Creates a FindBugs marker in a runnable window.
	*/
public class MarkerReporter implements IWorkspaceRunnable {
	private final BugCollection collection;
	private static final boolean EXPERIMENTAL_BUGS = false;
	private final List<MarkerParameter> mpList;
	private final IProject project;

	public MarkerReporter(List<MarkerParameter> mpList,
			BugCollection theCollection, IProject project) {

		this.mpList = mpList;
		this.collection = theCollection;
		this.project = project;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		UserPreferences userPrefs = FindbugsPlugin.getUserPreferences(project);
		ProjectFilterSettings filterSettings = userPrefs.getFilterSettings();
		for (MarkerParameter mp : mpList) {
			if(!MarkerUtil.displayWarning(mp.bug, filterSettings)){
				continue;
			}
			String markerType = getMarkerType(mp.bug);
			if(markerType == null) {
				continue;
			}
			// This triggers resource update on IResourceChangeListener's (BugTreeView)
			addmarker(markerType, mp);
		}

	}

	private void addmarker(String markerType, MarkerParameter mp) throws CoreException {
		IMarker marker = mp.resource.createMarker(markerType);

		Map<String, Object> attributes = createMarkerAttributes(marker, mp);
		setAttributes(marker, attributes);
	}

	/**
	 * @param bug
	 * @return null if marker shouldn't be generated
	 */
	private String getMarkerType(BugInstance bug) {
		String markerType;
		switch (bug.getPriority()) {
		case Priorities.HIGH_PRIORITY:
			markerType = FindBugsMarker.NAME_HIGH;
			break;
		case Priorities.NORMAL_PRIORITY:
			markerType = FindBugsMarker.NAME_NORMAL;
			break;
		case Priorities.LOW_PRIORITY:
			markerType = FindBugsMarker.NAME_LOW;
			break;
		case Priorities.EXP_PRIORITY:
			if (!EXPERIMENTAL_BUGS) {
				return null;
			}
			markerType = FindBugsMarker.NAME_EXPERIMENTAL;
			break;
		case Priorities.IGNORE_PRIORITY:
			FindbugsPlugin.getDefault().logError("Bug with ignore priority ");
			return null;
		default:
			FindbugsPlugin.getDefault().logError(
					"Bug with unknown priority " + bug.getPriority());
			return null;
		}
		return markerType;
	}

	/**
	 * @param marker non null
	 * @param mp
	 * @return attributes map which should be assigned to the given marker
	 */
	private Map<String, Object> createMarkerAttributes(IMarker marker, MarkerParameter mp) {
		Map<String, Object> attributes = new HashMap<String, Object>(23);
		attributes.put(IMarker.LINE_NUMBER, mp.startLine);
		attributes.put(FindBugsMarker.PRIMARY_LINE, mp.primaryLine);
		attributes.put(FindBugsMarker.BUG_TYPE, mp.bug.getType());
		long seqNum = mp.bug.getFirstVersion();
		if(seqNum == 0) {
			attributes.put(FindBugsMarker.FIRST_VERSION, "-1");
		} else {
			AppVersion theVersion = collection.getAppVersionFromSequenceNumber(seqNum);
			if (theVersion == null) {
				attributes.put(FindBugsMarker.FIRST_VERSION,
						"Cannot find AppVersion: seqnum=" + seqNum
								+ "; collection seqnum="
								+ collection.getSequenceNumber());
			} else {
				attributes.put(FindBugsMarker.FIRST_VERSION, Long
						.toString(theVersion.getTimestamp()));
			}
		}
		try {
			attributes.put(IMarker.MESSAGE, mp.bug
					.getMessageWithPriorityTypeAbbreviation());
		} catch (RuntimeException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Error generating msg for " + mp.bug.getType());
			attributes.put(IMarker.MESSAGE, "??? " + mp.bug.getType());
		}
		attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
		attributes.put(FindBugsMarker.PRIORITY_TYPE, mp.bug.getPriorityTypeString());

		switch (mp.bug.getPriority()) {
		case Priorities.HIGH_PRIORITY:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
			break;
		case Priorities.NORMAL_PRIORITY:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_NORMAL));
			break;
		default:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_LOW));
			break;
		}

//		attributes.put(FindBugsMarker.PATTERN_DESCR_SHORT, bug.getBugPattern().getShortDescription());

		// Set unique id of warning, so we can easily refer back
		// to it later: for example, when the user classifies the warning.
		String uniqueId = mp.bug.getInstanceHash();
		if (uniqueId != null) {
			attributes.put(FindBugsMarker.UNIQUE_ID, uniqueId);
		}
		return attributes;
	}

	/**
	 * Set all the attributes to marker in one 'workspace transaction'
	 * @param marker non null
	 * @throws CoreException
	 */
	private void setAttributes(IMarker marker, Map<String, Object> attributes) throws CoreException {
		marker.setAttributes(attributes);
	}

}
