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

package edu.umd.cs.findbugs.visitclass;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.bcel.Const;
import org.apache.bcel.Constants;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class DismantleBytecodeTest extends TestCase {
    public void testIsMethodCall() {
        DismantleBytecode dismantleBytecode = new DismantleBytecode() {
        };
        dismantleBytecode.setOpcode(Constants.INVOKEDYNAMIC);
        assertThat(dismantleBytecode.isMethodCall(), is(equalTo(true)));
    }

    public void testAreOppositeBranches() {
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IF_ACMPEQ, Const.IF_ACMPNE));
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IF_ICMPEQ, Const.IF_ICMPNE));
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IF_ICMPLT, Const.IF_ICMPGE));
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IFNE, Const.IFEQ));
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IFLT, Const.IFGE));
        assertTrue(DismantleBytecode.areOppositeBranches(Const.IFNULL, Const.IFNONNULL));
    }
}
