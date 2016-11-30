/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Search for bug instances whose text annotations contain one of a set of
 * keywords.
 */
public abstract class QueryBugAnnotations {
    // Bug's text annotation must contain one of the key
    // words in order to match
    private final HashSet<String> keywordSet = new HashSet<String>();

    /**
     * Add a keyword to the query. A BugInstance's text annotation must contain
     * at least one keyword in order to match the query.
     *
     * @param keyword
     *            the keyword
     */
    public void addKeyword(String keyword) {
        keywordSet.add(keyword);
    }

    /**
     * Scan bug instances contained in given file, reporting those whose text
     * annotations contain at least one of the keywords in the query.
     *
     * @param filename
     *            an XML file containing bug instances
     */
    public void scan(String filename) throws Exception {
        BugCollection bugCollection = new SortedBugCollection();
        bugCollection.readXML(filename);
        scan(bugCollection, filename);
    }

    /**
     * Scan bug instances contained in given bug collection, reporting those
     * whose text annotations contain at least one of the keywords in the query.
     *
     * @param bugCollection
     *            the bug collection
     * @param filename
     *            the XML file from which the bug collection was read
     */
    public void scan(BugCollection bugCollection, String filename) throws Exception {
        Iterator<BugInstance> i = bugCollection.iterator();
        while (i.hasNext()) {
            BugInstance bugInstance = i.next();

            Set<String> contents = bugInstance.getTextAnnotationWords();
            for (String aKeywordSet : keywordSet) {
                if (contents.contains(aKeywordSet)) {
                    match(bugInstance, filename);
                    break;
                }
            }
        }
    }

    /**
     * Called when a bug instance contains a query keyword.
     *
     * @param bugInstance
     *            the bug instance containing the keyword
     * @param filename
     *            name of the file containing the bug instance
     */
    protected abstract void match(BugInstance bugInstance, String filename) throws Exception;
}

