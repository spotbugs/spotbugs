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
 * Matcher to select BugInstances with a particular confidence.
 *
 * @author David Hovemeyer
 */
public class ConfidenceMatcher implements Matcher {
    private final int confidence;

    @Override
    public String toString() {
        return "Confidence(confidence=" + confidence + ")";
    }

    /**
     * Constructor.
     *
     * @param confidenceAsString
     *            the confidence, as a String
     * @throws FilterException
     */
    public ConfidenceMatcher(String confidenceAsString) {
        this.confidence = Integer.parseInt(confidenceAsString);
    }

    @Override
    public int hashCode() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConfidenceMatcher)) {
            return false;
        }
        ConfidenceMatcher other = (ConfidenceMatcher) o;
        return confidence == other.confidence;
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        return bugInstance.getPriority() == confidence;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("value", Integer.toString(confidence));
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("Confidence", attributes);
    }
}
