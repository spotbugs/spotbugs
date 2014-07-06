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
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Matcher to select BugInstances with a particular rank or higher.
 *
 * @author William Pugh
 */
public class RankMatcher implements Matcher {
    private final int rank;

    @Override
    public String toString() {
        return "Rank(rank=" + rank + ")";
    }

    /**
     * Constructor.
     *
     * @param rankAsString
     *            the rank, as a String
     * @throws NumberFormatException
     *             if the rank cannot be parsed
     */
    public RankMatcher(String rankAsString) {
        this.rank = Integer.parseInt(rankAsString);
    }

    @Override
    public int hashCode() {
        return rank;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RankMatcher)) {
            return false;
        }
        RankMatcher other = (RankMatcher) o;
        return rank == other.rank;
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        return BugRanker.findRank(bugInstance) >= rank;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("value", Integer.toString(rank));
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("Rank", attributes);
    }
}
