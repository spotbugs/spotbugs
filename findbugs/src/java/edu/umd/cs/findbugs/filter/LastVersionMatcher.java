/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.filter;

import java.io.IOException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Matcher to select BugInstances with a particular last version.
 */
public class LastVersionMatcher extends VersionMatcher implements Matcher {

    public final static LastVersionMatcher DEAD_BUG_MATCHER = new LastVersionMatcher(-1, RelationalOp.NEQ);

    public LastVersionMatcher(String versionAsString, String relOpAsString) {
        this(Long.parseLong(versionAsString), RelationalOp.byName(relOpAsString));
    }

    public LastVersionMatcher(String versionAsString, RelationalOp relOp) {
        this(Long.parseLong(versionAsString), relOp);
    }

    public LastVersionMatcher(long version, RelationalOp relOp) {
        super(version, relOp);
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        return relOp.check(bugInstance.getLastVersion(), version);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("value", Long.toString(version)).addAttribute("relOp",
                relOp.getName());
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("LastVersion", attributes);
    }

    @Override
    public String toString() {
        if (version == -1 && relOp == RelationalOp.EQ) {
            return "ActiveBugs";
        } else if (version == -1 && relOp == RelationalOp.NEQ) {
            return "DeadBugs";
        }
        return "LastVersion(version " + relOp + version + ")";
    }
}
