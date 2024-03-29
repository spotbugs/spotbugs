/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpcodeStackItemTest {

    @Test
    void testMergeIntAndZero() {
        OpcodeStack.Item intItem = new OpcodeStack.Item("I");
        OpcodeStack.Item zeroItem = new OpcodeStack.Item("I", 0);
        OpcodeStack.Item m1 = OpcodeStack.Item.merge(intItem, zeroItem);
        assertNull(m1.getConstant());
        OpcodeStack.Item m2 = OpcodeStack.Item.merge(zeroItem, intItem);
        assertNull(m2.getConstant());
    }

    @Test
    void testMergeTypeOnly() {
        OpcodeStack.Item intOnly = OpcodeStack.Item.typeOnly("I");
        OpcodeStack.Item zeroItem = new OpcodeStack.Item("I", 0);

        OpcodeStack.Item m1 = OpcodeStack.Item.merge(intOnly, zeroItem);
        assertEquals(0, m1.getConstant());
        OpcodeStack.Item m2 = OpcodeStack.Item.merge(zeroItem, intOnly);
        assertEquals(0, m2.getConstant());
    }

    private static final String NEW_ITEM_KIND_NAME = "newItemKindName";

    @Test
    void testDefineNewItemKind() {
        int defined = OpcodeStack.Item.defineSpecialKind(NEW_ITEM_KIND_NAME);
        assertEquals(NEW_ITEM_KIND_NAME,
                OpcodeStack.Item.getSpecialKindName(defined).get());
    }

    @Test
    void testDefinedItemKindIsUsedInToStringMethod() {
        int defined = OpcodeStack.Item.defineSpecialKind(NEW_ITEM_KIND_NAME);
        OpcodeStack.Item intItem = new OpcodeStack.Item("I");
        intItem.setSpecialKind(defined);
        String result = intItem.toString();
        assertTrue(result.contains(NEW_ITEM_KIND_NAME),
                "Item.toString() does not use proper name of special kind:" + result);
    }
}
