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

package edu.umd.cs.findbugs.cloud.username;

import java.util.prefs.Preferences;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;

/**
 * @author pugh
 */
public class PromptForNameLookup implements NameLookup {

    BugCollection bugCollection;

    String username;

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.cloud.NameLookup#getUserName()
     */
    public boolean init() {
        Preferences prefs = Preferences.userNodeForPackage(PromptForNameLookup.class);
        String findbugsUser = prefs.get(USER_NAME, null);
        findbugsUser = bugCollection
                .getProject()
                .getGuiCallback()
                .showQuestionDialog(
                        "Name/handle/email for recording your reviews?\n"
                                + "(sorry, no authentication or confidentiality currently provided)",
                                "Name for recording your reviews", findbugsUser == null ? "" : findbugsUser);
        if (findbugsUser != null) {
            prefs.put(USER_NAME, findbugsUser);
            username = findbugsUser;
            return true;
        }
        return false;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean signIn(CloudPlugin plugin, BugCollection bugCollection) {
        this.bugCollection = bugCollection;
        return true;
    }

}
