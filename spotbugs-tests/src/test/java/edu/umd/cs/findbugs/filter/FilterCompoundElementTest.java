/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2026, University of Maryland
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;

class FilterCompoundElementTest {

    private static Filter parseFilter(String orElementName) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "\n<FindBugsFilter>"
                + "\n<Match>"
                + "\n<" + orElementName + ">"
                + "\n<Bug pattern=\"THROWS_METHOD_THROWS_RUNTIMEEXCEPTION\" />"
                + "\n<Bug pattern=\"THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION\" />"
                + "\n</" + orElementName + ">"
                + "\n</Match>"
                + "\n</FindBugsFilter>\n";
        return new Filter(new StringInputStream(xml));
    }

    @Test
    void lowercaseOrMatchesOnlyListedPatterns() throws Exception {
        Filter filter = parseFilter("or");

        BugInstance sqlBug = new BugInstance("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", 3);
        BugInstance throwsBug = new BugInstance("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 2);

        assertFalse(filter.match(sqlBug));
        assertTrue(filter.match(throwsBug));
    }

    @Test
    void uppercaseOrMatchesOnlyListedPatterns() throws Exception {
        Filter filter = parseFilter("Or");

        BugInstance sqlBug = new BugInstance("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", 3);
        BugInstance throwsBug = new BugInstance("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 2);

        assertFalse(filter.match(sqlBug));
        assertTrue(filter.match(throwsBug));
    }
}
