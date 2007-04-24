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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.view.BugTreeView;
import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;

/**
 * Creates a FindBugs marker in a runnable window.
 */
public class MarkerReporter implements IWorkspaceRunnable {
	BugInstance bug;
	IResource resource;
	int startLine;
	BugCollection collection;
	IProject project;

	public MarkerReporter(BugInstance bug, IResource resource, int startLine, BugCollection theCollection, IProject project) {
		this.startLine = startLine;
		this.bug=bug;
		this.resource=resource;
		this.collection=theCollection;
		this.project=project;
	}

	public void run(IProgressMonitor monitor) throws CoreException {

		int priority = this.bug.getPriority();
		String markerName;
		switch(priority)
		{
			case Priorities.HIGH_PRIORITY:
				markerName = FindBugsMarker.NAME_HIGH;
				break;
			case Priorities.NORMAL_PRIORITY:
				markerName = FindBugsMarker.NAME_NORMAL;
				break;
			case Priorities.LOW_PRIORITY:
				markerName = FindBugsMarker.NAME_LOW;
				break;
			case Priorities.EXP_PRIORITY:
				markerName = FindBugsMarker.NAME_EXPERIMENTAL;
				break;
			case Priorities.IGNORE_PRIORITY:
				markerName = FindBugsMarker.NAME_IGNORE;
				break;
			default:
				FindbugsPlugin.getDefault().logError("Bug with unknown priority " + priority);
			return;

		}
		IMarker marker = resource.createMarker(markerName);
		marker.setAttribute(IMarker.LINE_NUMBER, startLine);
		marker.setAttribute(FindBugsMarker.BUG_LINE_NUMBER, startLine);
		marker.setAttribute(FindBugsMarker.BUG_TYPE, bug.getType());
		AppVersion theVersion;
		long seqNum = bug.getFirstVersion();
		if(seqNum == 0)
			marker.setAttribute(FindBugsMarker.FIRST_VERSION, Long.toString(-1));
		else
		{
			theVersion = collection.getAppVersionFromSequenceNumber(seqNum);
			if(theVersion == null)
				marker.setAttribute(FindBugsMarker.FIRST_VERSION, "Cannot find AppVersion: seqnum=" + seqNum + "; collection seqnum=" + collection.getSequenceNumber());
			else
				marker.setAttribute(FindBugsMarker.FIRST_VERSION, Long.toString(theVersion.getTimestamp()));
		}
		marker.setAttribute(IMarker.MESSAGE, bug.getMessageWithPriorityTypeAbbreviation());
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
		marker.setAttribute(FindBugsMarker.PRIORITY_TYPE, bug.getPriorityTypeString());
		if (bug.getPriority() == Detector.HIGH_PRIORITY)
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		else if (bug.getPriority() == Detector.NORMAL_PRIORITY)
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
		else marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);

		// Set unique id of warning, so we can easily refer back
		// to it later: for example, when the user classifies the warning.
		String uniqueId = bug.getInstanceHash();
		if (uniqueId != null) {
			marker.setAttribute(FindBugsMarker.UNIQUE_ID, uniqueId);
		}
		BugTreeView theView = BugTreeView.getBugTreeView();
		if(theView != null)
			theView.addMarker(project, marker);
	}
}
