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

package edu.umd.cs.findbugs.ba;

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author David Hovemeyer
 */
public class MethodHashTest extends TestCase {

    byte[] hash;

    String s;

    byte[] sameHash;

    byte[] greaterHash;

    byte[] lesserHash;

    byte[] shorterHash;

    byte[] longerHash;

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */

    @Override
    protected void setUp() throws Exception {
        hash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        s = "0604deadbeef";
        sameHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        greaterHash = new byte[] { 0x06, 0x05, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        lesserHash = new byte[] { 0x06, 0x03, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        shorterHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE };
        longerHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x01 };
    }

    public void testHashToString() {
        String s2 = ClassHash.hashToString(hash);
        Assert.assertEquals(s, s2);
    }

    public void testStringToHash() {
        byte[] hash2 = ClassHash.stringToHash(s);
        Assert.assertTrue(Arrays.equals(hash, hash2));
    }

    public void testSame() {
        Assert.assertTrue(MethodHash.compareHashes(hash, sameHash) == 0);
        Assert.assertTrue(MethodHash.compareHashes(sameHash, hash) == 0);
    }

    public void testGreater() {
        Assert.assertTrue(MethodHash.compareHashes(hash, greaterHash) < 0);
    }

    public void testLesser() {
        Assert.assertTrue(MethodHash.compareHashes(hash, lesserHash) > 0);
    }

    public void testShorter() {
        Assert.assertTrue(MethodHash.compareHashes(hash, shorterHash) > 0);
    }

    public void testLonger() {
        Assert.assertTrue(MethodHash.compareHashes(hash, longerHash) < 0);
    }
}
