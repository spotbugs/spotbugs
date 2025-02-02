/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2007, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;

/**
 * Test cases for ReturnPathType class.
 *
 * @author David Hovemeyer
 */
class ReturnPathTypeTest {

    ReturnPathType top;

    ReturnPathType normal;

    ReturnPathType abnormal;

    @BeforeEach
    void setUp() {
        top = new ReturnPathType();
        normal = new ReturnPathType();
        normal.setCanReturnNormally(true);
        abnormal = new ReturnPathType();
        abnormal.setCanReturnNormally(false);
    }

    @Test
    void testTop() throws Exception {
        Assertions.assertFalse(top.isValid());
        Assertions.assertTrue(top.isTop());
        try {
            top.canReturnNormally();// should throw exception
            Assertions.assertTrue(false);
        } catch (DataflowAnalysisException e) {
            // Good
        }
    }

    @Test
    void testCanReturnNormally() throws Exception {
        Assertions.assertTrue(normal.isValid());
        Assertions.assertTrue(normal.canReturnNormally());
    }

    @Test
    void testCannotReturnNormally() throws Exception {
        Assertions.assertTrue(abnormal.isValid());
        Assertions.assertFalse(abnormal.canReturnNormally());
    }

    @Test
    void testMergeWithTop() throws Exception {
        normal.mergeWith(top);
        Assertions.assertTrue(normal.canReturnNormally());
        abnormal.mergeWith(top);
        Assertions.assertFalse(abnormal.canReturnNormally());
    }

    @Test
    void testTopMergeWithNormalReturn() throws Exception {
        top.mergeWith(normal);
        Assertions.assertTrue(top.canReturnNormally());
    }

    @Test
    void testTopMergeWithAbnormalReturn() throws Exception {
        top.mergeWith(abnormal);
        Assertions.assertFalse(top.canReturnNormally());
    }

    @Test
    void testNormalMergeWIthAbnormal() throws Exception {
        normal.mergeWith(abnormal);
        Assertions.assertTrue(normal.canReturnNormally());
    }

    @Test
    void testAbnormalMergeWithNormal() throws Exception {
        abnormal.mergeWith(normal);
        Assertions.assertTrue(abnormal.canReturnNormally());
    }

    @Test
    void testNormalMergeWithNormal() throws Exception {
        ReturnPathType otherNormal = new ReturnPathType();
        otherNormal.setCanReturnNormally(true);

        normal.mergeWith(otherNormal);
        Assertions.assertTrue(normal.canReturnNormally());
    }

    @Test
    void testAbnormalMergeWithAbnormal() throws Exception {
        ReturnPathType otherAbnormal = new ReturnPathType();
        otherAbnormal.setCanReturnNormally(false);

        abnormal.mergeWith(otherAbnormal);
        Assertions.assertFalse(abnormal.canReturnNormally());
    }
}
