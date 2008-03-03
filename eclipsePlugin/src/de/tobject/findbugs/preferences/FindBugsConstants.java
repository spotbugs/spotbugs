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
}
