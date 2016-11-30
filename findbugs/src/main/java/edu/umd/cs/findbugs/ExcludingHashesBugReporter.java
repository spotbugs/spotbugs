/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.dom4j.DocumentException;

/**
 * @author pugh
 */
public class ExcludingHashesBugReporter extends DelegatingBugReporter {

    Set<String> excludedHashes = new HashSet<String>();

    /**
     * @param delegate
     * @throws DocumentException
     * @throws IOException
     */
    public ExcludingHashesBugReporter(BugReporter delegate, String baseline) throws IOException, DocumentException {
        super(delegate);
        addToExcludedInstanceHashes(excludedHashes, baseline);
    }

    /**
     * @param baseline
     * @throws IOException
     * @throws DocumentException
     */
    public static void addToExcludedInstanceHashes(Set<String> instanceHashesToExclude, String baseline) throws IOException,
    DocumentException {
        Project project = new Project();
        BugCollection origCollection;
        origCollection = new SortedBugCollection(project);
        origCollection.readXML(baseline);
        for (BugInstance b : origCollection.getCollection()) {
            instanceHashesToExclude.add(b.getInstanceHash());
        }
    }

    @Override
    public void reportBug(@Nonnull BugInstance bugInstance) {
        String instanceHash = bugInstance.getInstanceHash();
        if (!excludedHashes.contains(instanceHash)) {
            getDelegate().reportBug(bugInstance);
        }
    }
}
