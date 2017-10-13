/*
 * Contributions to SpotBugs
 * Copyright (C) 2017, kengo
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @since 3.1
 */
public class NameMatchTest {
    @Test
    public void testExactMatch() {
        NameMatch match = new NameMatch(SignatureUtil.createMethodSignature("", "void"));
        assertTrue(match.match("()V"));
        assertFalse(match.match("(I)V"));
        assertFalse(match.match("()J"));
    }

    @Test
    public void testParameterPatternMatch() {
        NameMatch anyParamReturnVoid = new NameMatch(SignatureUtil.createMethodSignature(null, "void"));
        assertTrue(anyParamReturnVoid.match("()V"));
        assertTrue(anyParamReturnVoid.match("(I)V"));
        assertTrue(anyParamReturnVoid.match("(Ljava/lang/String;)V"));
        assertFalse(anyParamReturnVoid.match("()J"));
    }

    @Test
    public void testReturnValuePatternMatch() {
        NameMatch anyParamReturnVoid = new NameMatch(SignatureUtil.createMethodSignature("", null));
        assertTrue(anyParamReturnVoid.match("()V"));
        assertTrue(anyParamReturnVoid.match("()I"));
        assertTrue(anyParamReturnVoid.match("()Ljava/lang/String;"));
        assertFalse(anyParamReturnVoid.match("(B)J"));
    }
}
