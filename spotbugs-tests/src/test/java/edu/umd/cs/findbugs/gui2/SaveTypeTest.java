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

package edu.umd.cs.findbugs.gui2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * @author Bill Pugh
 */
class SaveTypeTest {

    private void check(SaveType type, String file) {
        assertEquals(type, SaveType.forFile(new File(file)));
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "No actual disk access, just fake paths for testing")
    @Test
    void testSaveTypes() {
        check(SaveType.HTML_OUTPUT, "/home/pugh/bugs.html");
        check(SaveType.HTML_OUTPUT, "/home/pugh/bugs.htm");
        check(SaveType.HTML_OUTPUT, "/home/pugh/bugs.HTML");
        check(SaveType.XML_ANALYSIS, "/home/pugh/bugs.xml");
        check(SaveType.XML_ANALYSIS, "/home/pugh/bugs.XML");
        check(SaveType.XML_ANALYSIS, "/home/pugh/bugs.xml.gz");
        check(SaveType.FBP_FILE, "/home/pugh/bugs.fbp");
        check(SaveType.FBA_FILE, "/home/pugh/bugs.fba");
    }
}
