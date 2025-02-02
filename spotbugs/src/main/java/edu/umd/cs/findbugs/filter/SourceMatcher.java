/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
 *
 * Author: Andrey Loskutov
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class SourceMatcher implements Matcher {
    private static final Logger LOG = LoggerFactory.getLogger(SourceMatcher.class);

    private final NameMatch fileName;

    @Override
    public String toString() {
        return "Source(file=\"" + fileName.getValue() + "\")";
    }

    public SourceMatcher(String fileName) {
        this.fileName = new NameMatch(fileName);
    }

    @Override
    public boolean match(BugInstance bugInstance) {
        // Match on the pure filename first
        ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
        if (primaryClassAnnotation == null) {
            return false;
        }

        // Create the variable to store the result. If there is no valid bug file name,
        // the result is false no matter what.
        boolean result = false;
        String bugFileName = primaryClassAnnotation.getSourceFileName();
        if (bugFileName != null && !bugFileName.isEmpty()) {

            // Check if the files are already matching and store the result
            // This is the check which just compares in a simple way and does not obey
            // the full path of the file.
            result = fileName.match(bugFileName);

            // if no result try again to match with the full path as well
            if (!result && bugInstance.getPrimarySourceLineAnnotation().isSourceFileKnown()) {
                bugFileName = bugInstance.getPrimarySourceLineAnnotation().getRealSourcePath();
                result = fileName.match(bugFileName);
            }
        }

        LOG.debug("Matching {} with {}, result = {}", bugFileName, fileName, result);
        return result;
    }

    @Override
    public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
        XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", fileName.getSpec());
        if (disabled) {
            attributes.addAttribute("disabled", "true");
        }
        xmlOutput.openCloseTag("Source", attributes);
    }
}
