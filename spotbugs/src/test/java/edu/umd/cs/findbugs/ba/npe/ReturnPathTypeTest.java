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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;

/**
 * Test cases for ReturnPathType class.
 *
 * @author David Hovemeyer
 */
public class ReturnPathTypeTest {

    ReturnPathType top;

    ReturnPathType normal;

    ReturnPathType abnormal;

    @Before
    public void setUp() {
        top = new ReturnPathType();
        normal = new ReturnPathType();
        normal.setCanReturnNormally(true);
        abnormal = new ReturnPathType();
        abnormal.setCanReturnNormally(false);
    }

    @Test
    public void testTop() throws Exception {
        Assert.assertFalse(top.isValid());
        Assert.assertTrue(top.isTop());
        try {
            top.canReturnNormally();// should throw exception
            Assert.assertTrue(false);
        } catch (DataflowAnalysisException e) {
            // Good
        }
    }

    @Test
    public void testCanReturnNormally() throws Exception {
        Assert.assertTrue(normal.isValid());
        Assert.assertTrue(normal.canReturnNormally());
    }

    @Test
    public void testCannotReturnNormally() throws Exception {
        Assert.assertTrue(abnormal.isValid());
        Assert.assertFalse(abnormal.canReturnNormally());
    }

    @Test
    public void testMergeWithTop() throws Exception {
        normal.mergeWith(top);
        Assert.assertTrue(normal.canReturnNormally());
        abnormal.mergeWith(top);
        Assert.assertFalse(abnormal.canReturnNormally());
    }

    @Test
    public void testTopMergeWithNormalReturn() throws Exception {
        top.mergeWith(normal);
        Assert.assertTrue(top.canReturnNormally());
    }

    @Test
    public void testTopMergeWithAbnormalReturn() throws Exception {
        top.mergeWith(abnormal);
        Assert.assertFalse(top.canReturnNormally());
    }

    @Test
    public void testNormalMergeWIthAbnormal() throws Exception {
        normal.mergeWith(abnormal);
        Assert.assertTrue(normal.canReturnNormally());
    }

    @Test
    public void testAbnormalMergeWithNormal() throws Exception {
        abnormal.mergeWith(normal);
        Assert.assertTrue(abnormal.canReturnNormally());
    }

    @Test
    public void testNormalMergeWithNormal() throws Exception {
        ReturnPathType otherNormal = new ReturnPathType();
        otherNormal.setCanReturnNormally(true);

        normal.mergeWith(otherNormal);
        Assert.assertTrue(normal.canReturnNormally());
    }

    @Test
    public void testAbnormalMergeWithAbnormal() throws Exception {
        ReturnPathType otherAbnormal = new ReturnPathType();
        otherAbnormal.setCanReturnNormally(false);

        abnormal.mergeWith(otherAbnormal);
        Assert.assertFalse(abnormal.canReturnNormally());
    }
}
