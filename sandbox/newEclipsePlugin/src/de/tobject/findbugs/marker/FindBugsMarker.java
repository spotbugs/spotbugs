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

package de.tobject.findbugs.marker;

import org.eclipse.jdt.core.IJavaElement;

import edu.umd.cs.findbugs.Priorities;

/**
 * Marker ids for the findbugs.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 13.08.2003
 */
public interface FindBugsMarker {
	/**
	 * Marker type for FindBugs warnings.
	 * (should be the plugin id concatenated with ".findbugsMarker")
	 */
	public static final String NAME = "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarker";
	public static final String NAME_HIGH = "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarkerHigh";
	public static final String NAME_NORMAL = "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarkerNormal";
	public static final String NAME_LOW = "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarkerLow";
	public static final String NAME_EXPERIMENTAL = "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarkerExperimental";
	/**
	 * Marker attribute recording the bug type (specific bug pattern).
	 */
	public static final String BUG_TYPE = "BUGTYPE";
	/**
	 * Marker attribute recording the pattern type (more general pattern group).
	 */
	public static final String PATTERN_TYPE = "PATTERNTYPE";

	/**
	 * Marker attribute recording the unique id of the BugInstance
	 * in its BugCollection.
	 */
	public static final String UNIQUE_ID = "FINDBUGS_UNIQUE_ID";

	/**
	 * Marker attribute recording the unique Java handle identifier, see
	 * {@link IJavaElement#getHandleIdentifier()}
	 */
	public static final String UNIQUE_JAVA_ID = "UNIQUE_JAVA_ID";

	/**
	 * Marker attribute recording the primary (first) line of the BugInstance
	 * in its BugCollection (in case same bug reported on many lines).
	 */
	public static final String PRIMARY_LINE = "PRIMARY_LINE";

	/**
	 * Marker attribute recording the name and timestamp of the first version.
	 */
	public static final String FIRST_VERSION = "FIRST_VERSION";

	/**
	 * Marker attribute recording the priority and type of the bug (e.g. "High Priority Correctness")
	 */
	public static final String PRIORITY_TYPE = "PRIORITY_TYPE";

//	/**
//	 * Marker attribute recording the "group" of the bug (e.g. "Unread field")
//	 */
//	public static final String PATTERN_DESCR_SHORT = "PATTERN_DESCR_SHORT";

	enum Priority {
		High(NAME_HIGH, "buggy-tiny.png", Priorities.HIGH_PRIORITY),
		Normal(NAME_NORMAL, "buggy-tiny-orange.png", Priorities.NORMAL_PRIORITY),
		Low(NAME_LOW, "buggy-tiny-yellow.png", Priorities.LOW_PRIORITY),
		Experimental(NAME_EXPERIMENTAL, "buggy-tiny-blue.png", Priorities.EXP_PRIORITY),
		Ignore("", "buggy-tiny-green.png", Priorities.IGNORE_PRIORITY),
		Unknown("", "buggy-tiny-gray.png", Priorities.IGNORE_PRIORITY);

		private final String prioName;
		private final String icon;
		private final int detectorPrio;

		Priority(String prioName, String icon, int detectorPrio){
			this.prioName = prioName;
			this.icon = icon;
			this.detectorPrio = detectorPrio;
		}

		public static Priority label(int prioId) {
			Priority[] values = Priority.values();
			for (Priority priority : values) {
				if(priority.detectorPrio == prioId) {
					return priority;
				}
			}
			return Unknown;
		}

		public static int ordinal(String prioId) {
			Priority[] values = Priority.values();
			for (Priority priority : values) {
				if(priority.prioName.equals(prioId)) {
					return priority.ordinal();
				}
			}
			return -1;
		}

		public String iconName(){
			return icon;
		}
	}
}
