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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;

/**
 * Creates a FindBugs marker in a runnable window.
 */
public class MarkerReporter implements IWorkspaceRunnable {
	BugInstance bug;
	IResource resource;
	int startLine;

	public MarkerReporter(BugInstance bug, IResource resource, int startLine) {
		this.startLine = startLine;
		this.bug=bug;
		this.resource=resource;
	}

	public void run(IProgressMonitor monitor) throws CoreException {

		IMarker marker = resource.createMarker(FindBugsMarker.NAME);
		marker.setAttribute(IMarker.LINE_NUMBER, startLine);
		marker.setAttribute(FindBugsMarker.BUG_TYPE, bug.getType());
		marker.setAttribute(IMarker.MESSAGE, bug.getMessage());
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
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

	}
}
