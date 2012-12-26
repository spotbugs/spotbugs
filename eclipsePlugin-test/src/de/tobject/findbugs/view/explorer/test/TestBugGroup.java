/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey loskutov
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
package de.tobject.findbugs.view.explorer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import de.tobject.findbugs.view.explorer.BugGroup;
import de.tobject.findbugs.view.explorer.GroupType;

public class TestBugGroup {

    @Test
    public void testEquals() {
        BugGroup bg1 = new BugGroup(null, null, GroupType.Marker);
        BugGroup bg2 = new BugGroup(null, null, GroupType.Marker);
        assertEquals(bg1, bg2);

        bg1 = new BugGroup(null, "", GroupType.Marker);
        bg2 = new BugGroup(null, null, GroupType.Marker);
        assertFalse(bg1.equals(bg2));

        bg1 = new BugGroup(null, "", GroupType.Marker);
        bg2 = new BugGroup(null, "", GroupType.Marker);
        assertEquals(bg1, bg2);

        bg1 = new BugGroup(null, "", GroupType.Marker);
        bg2 = new BugGroup(null, "", GroupType.Category);
        assertFalse(bg1.equals(bg2));

        bg1 = new BugGroup(null, "", GroupType.Marker);
        bg2 = new BugGroup(null, "aa", GroupType.Marker);
        assertFalse(bg1.equals(bg2));

        bg1 = new BugGroup(null, "", GroupType.Marker);
        bg2 = new BugGroup(null, "", GroupType.Marker);
        assertEquals(bg1, bg2);
    }

}
