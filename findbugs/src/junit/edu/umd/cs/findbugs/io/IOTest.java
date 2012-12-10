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

package edu.umd.cs.findbugs.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author pugh
 */
public class IOTest {

    Random r = new Random();

    private byte[] randomBytes(int size) {
        byte [] result = new byte[size];
        r.nextBytes(result);
        return result;
    }
    @Test
    public void testReadAllWithCorrectSize() throws IOException {

        for(int i = 10; i <= 10000; i *= 10) {
            byte [] input = randomBytes(i);
            byte [] output = IO.readAll(new ByteArrayInputStream(input), i);
            Assert.assertArrayEquals(input, output);
        }
    }
    @Test
    public void testReadAllWithSmallSize() throws IOException {

        for(int i = 10; i <= 10000; i *= 10) {
            byte [] input = randomBytes(i);
            byte [] output = IO.readAll(new ByteArrayInputStream(input), i-9);
            Assert.assertArrayEquals(input, output);
        }
    }
    @Test
    public void testReadAllWithLargeSize() throws IOException {

        for(int i = 10; i <= 10000; i *= 10) {
            byte [] input = randomBytes(i);
            byte [] output = IO.readAll(new ByteArrayInputStream(input), i+29);
            Assert.assertArrayEquals(input, output);
        }
    }
    @Test
    public void testReadAllWithoutSize() throws IOException {

        for(int i = 10; i <= 10000; i *= 10) {
            byte [] input = randomBytes(i);
            byte [] output = IO.readAll(new ByteArrayInputStream(input));
            Assert.assertArrayEquals(input, output);
        }
    }
}
