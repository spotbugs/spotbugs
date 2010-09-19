/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
package de.tobject.findbugs.reporter;

import org.eclipse.core.resources.IMarker;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;

/**
 * @author Andrei Loskutov
 */
public class MarkerParameter {
    private static final boolean EXPERIMENTAL_BUGS = false;

    public final BugInstance bug;
    public final WorkItem resource;
    public final Integer primaryLine;
	public final Integer startLine;

    /**
     * Might be null if marker shouldn't be generated
     */
	public final String markerType;

    /**
     * Default value is {@link IMarker#SEVERITY_WARNING}, might be configured by user
     */
	public Integer markerSeverity;

    public MarkerParameter(BugInstance bug, WorkItem resource, int startLine, int primaryLine) {
        this.bug = bug;
        this.resource = resource;
		this.startLine = Integer.valueOf(startLine);
        this.primaryLine = Integer.valueOf(primaryLine);
        markerType = getMarkerType();
        // default
		markerSeverity = Integer.valueOf(IMarker.SEVERITY_WARNING);
    }

    /**
     * @return null if marker shouldn't be generated
     */
	private String getMarkerType() {
        String type;
        switch (bug.getPriority()) {
        case Priorities.HIGH_PRIORITY:
			type = FindBugsMarker.NAME_HIGH;
            break;
        case Priorities.NORMAL_PRIORITY:
            type = FindBugsMarker.NAME_NORMAL;
			break;
        case Priorities.LOW_PRIORITY:
            type = FindBugsMarker.NAME_LOW;
            break;
		case Priorities.EXP_PRIORITY:
            if (!EXPERIMENTAL_BUGS) {
                return null;
            }
			type = FindBugsMarker.NAME_EXPERIMENTAL;
            break;
        case Priorities.IGNORE_PRIORITY:
            FindbugsPlugin.getDefault().logError("Bug with ignore priority ");
			return null;
        default:
            FindbugsPlugin.getDefault().logError(
                    "Bug with unknown priority " + bug.getPriority());
			return null;
        }
        return type;
    }
}
