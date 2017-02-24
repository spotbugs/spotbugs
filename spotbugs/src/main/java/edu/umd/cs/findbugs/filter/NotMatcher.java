/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 * Author: Graham Allan
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
import java.util.Iterator;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class NotMatcher extends CompoundMatcher {

    @Override
    public boolean match(BugInstance bugInstance) {
        if(!childIterator().hasNext() ) {
            return false;
        }

        Matcher invertedMatcher = childIterator().next();
        return ! invertedMatcher.match(bugInstance);
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled)  throws IOException {
        if(childIterator().hasNext()) {
            xmlOutput.startTag("Not");
            if (disabled) {
                xmlOutput.addAttribute("disabled","true");
            }
            Matcher invertedMatcher = childIterator().next();
            xmlOutput.stopTag(false);

            invertedMatcher.writeXML(xmlOutput, disabled);

            xmlOutput.closeTag("Not");
        }
    }

    @Override
    public String toString() {
        Matcher invertedMatcher = childIterator().hasNext() ? childIterator().next() : null;
        String invertedMatcherString = invertedMatcher == null ? "" : invertedMatcher.toString();
        return "Not(" + invertedMatcherString +")";
    }

    @Override
    public int maxChildren() {
        return 1;
    }

    public Matcher originalMatcher() {
        Iterator<Matcher> childMatchers = childIterator();
        if (childMatchers.hasNext()) {
            return childMatchers.next();
        } else {
            throw new IllegalStateException("Tried to retrieve child matcher of empty NotMatcher");
        }
    }
}
