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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author David Hovemeyer
 */
class MethodHashTest {

    byte[] hash;

    String s;

    byte[] sameHash;

    byte[] greaterHash;

    byte[] lesserHash;

    byte[] shorterHash;

    byte[] longerHash;

    @BeforeEach
    void setUp() {
        hash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        s = "0604deadbeef";
        sameHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        greaterHash = new byte[] { 0x06, 0x05, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        lesserHash = new byte[] { 0x06, 0x03, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        shorterHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE };
        longerHash = new byte[] { 0x06, 0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, (byte) 0x01 };
    }

    @Test
    void testHashToString() {
        String s2 = ClassHash.hashToString(hash);
        Assertions.assertEquals(s, s2);
    }

    @Test
    void testStringToHash() {
        byte[] hash2 = ClassHash.stringToHash(s);
        Assertions.assertArrayEquals(hash, hash2);
    }

    @Test
    void testSame() {
        Assertions.assertEquals(0, MethodHash.compareHashes(hash, sameHash));
        Assertions.assertEquals(0, MethodHash.compareHashes(sameHash, hash));
    }

    @Test
    void testGreater() {
        Assertions.assertTrue(MethodHash.compareHashes(hash, greaterHash) < 0);
    }

    @Test
    void testLesser() {
        Assertions.assertTrue(MethodHash.compareHashes(hash, lesserHash) > 0);
    }

    @Test
    void testShorter() {
        Assertions.assertTrue(MethodHash.compareHashes(hash, shorterHash) > 0);
    }

    @Test
    void testLonger() {
        Assertions.assertTrue(MethodHash.compareHashes(hash, longerHash) < 0);
    }
}
