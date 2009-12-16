/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud.db;

import java.util.prefs.Preferences;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.cloud.NameLookup;

/**
 * @author pugh
 */
public class LocalNameLookup implements NameLookup {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.cloud.NameLookup#getUserName()
	 */
	public String getUserName(BugCollection bugCollection) {
		String findbugsUser = DBCloud.getCloudProperty("findbugsUser");

		if (findbugsUser == null) {
			if (DBCloud.PROMPT_FOR_USER_NAME) {
				Preferences prefs = Preferences.userNodeForPackage(DBCloud.class);
				findbugsUser = prefs.get(DBCloud.USER_NAME, null);
				findbugsUser = bugCollection.getProject().getGuiCallback().showQuestionDialog(
				        "Name/handle/email for recording your evaluations?\n"
				                + "(sorry, no authentication or confidentiality currently provided)",
				        "Name for recording your evaluations", findbugsUser == null ? "" : findbugsUser);
				if (findbugsUser != null)
					prefs.put(DBCloud.USER_NAME, findbugsUser);
			} else
				findbugsUser = System.getProperty(DBCloud.USER_NAME, "");

			if (findbugsUser == null && DBCloud.THROW_EXCEPTION_IF_CANT_CONNECT)
				throw new RuntimeException("Unable to get reviewer user name for database");

		}
		return findbugsUser;
	}

}
