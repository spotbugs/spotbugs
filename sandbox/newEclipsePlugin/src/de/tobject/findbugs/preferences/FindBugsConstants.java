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
package de.tobject.findbugs.preferences;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Andrei
 */
public final class FindBugsConstants {

	private FindBugsConstants() {
		// never call this
	}

	/** comma separated list of bug patterns which should be omitted on export operation */
	public final static String LAST_USED_EXPORT_FILTER = "lastUsedExportFilter";

	/** "sort by" preference for exporting data */
	public final static String EXPORT_SORT_ORDER = "exportSortOrder";

	public final static String ORDER_BY_NAME = "byName";

	public final static String ORDER_BY_OVERALL_BUGS_COUNT = "byOverallBugsCount";

	public final static String ORDER_BY_NOT_FILTERED_BUGS_COUNT = "byNotFilteredBugsCount";

	public final static String DONT_REMIND_ABOUT_FULL_BUILD = "dontRemindAboutFullBuild";

	public final static String LAST_USED_GROUPING = "lastUsedGrouping";

	public final static String LAST_USED_WORKING_SET = "lastUsedWorkingSet";

	//////////////////////////////////////////////////////////
	// FB workspace level settings

	/** ask if FB should automatically switch to the FB perspective after analysis is done */
	public final static String ASK_ABOUT_PERSPECTIVE_SWITCH = "askAboutPerspectiveSwitch";

	/** true to switch to FindBugs perspective after analysis is done */
	public final static String SWITCH_PERSPECTIVE_AFTER_ANALYSIS = "switchPerspectiveAfterAnalysis";

	public final static String DISABLED_CATEGORIES = "disabledCategories";

	public final static String DISABLED_DETECTORS = "disabledDetectors";

	public final static String DISABLED_PATTERN_TYPES = "disabledPatternTypes";

	public final static String DISABLED_PATTERNS = "disabledPatterns";

	public final static String RUN_ANALYSIS_AUTOMATICALLY = "runAnalysisAutomatically";

	public final static String RUN_ANALYSIS_ON_FULL_BUILD = "runAnalysisOnFullBuild";

	public final static String PROJECT_PROPS_DISABLED = "disableProjectProps";

	public final static String ENABLE_CONSOLE_OUTPUT = "enableConsoleOutput";

	public final static String PROFILE_OUTPUT_MODE = "profileOutputMode";
	public final static String PROFILE_OUTPUT_BY_TIME = "profileOutputByTime";
	public final static String PROFILE_OUTPUT_BY_CALL = "profileOutputByCall";
	public final static String PROFILE_OUTPUT_BY_TIME_PER_CALL = "profileOutputByTimePerCall";

	public static final String IDS_PATTERN = "\\s*,\\s*";

	public static final String IDS_SEPARATOR = ", ";

	public static String encodeIds(Set<String> ids){
		StringBuilder sb = new StringBuilder();
		for (String string : ids) {
			sb.append(string).append(IDS_SEPARATOR);
		}
		if(sb.length() > 0) {
			sb.setLength(sb.length() - IDS_SEPARATOR.length());
		}
		return sb.toString();
	}

	public static Set<String> decodeIds(String text) {
		Set<String> sortedIds = new TreeSet<String>();
		if(text == null || text.trim().length() == 0) {
			return sortedIds;
		}

		String[] strings = text.split(IDS_PATTERN);
		for (String string : strings) {
			string = string.trim();
			if(string.length() > 0){
				sortedIds.add(string);
			}
		}
		return sortedIds;
	}
}
