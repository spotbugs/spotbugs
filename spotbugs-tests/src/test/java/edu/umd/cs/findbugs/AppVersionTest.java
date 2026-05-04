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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class AppVersionTest {

    @Test
    void constructorWithSequenceSetsDefaults() {
        AppVersion v = new AppVersion(42L);
        assertEquals(42L, v.getSequenceNumber());
        assertEquals("", v.getReleaseName());
        assertTrue(v.getTimestamp() > 0);
    }

    @Test
    void constructorWithSequenceTimestampAndName() {
        AppVersion v = new AppVersion(1L, 1000L, "release-1.0");
        assertEquals(1L, v.getSequenceNumber());
        assertEquals(1000L, v.getTimestamp());
        assertEquals("release-1.0", v.getReleaseName());
    }

    @Test
    void constructorWithDateSetsTimestamp() {
        Date date = new Date(5000L);
        AppVersion v = new AppVersion(2L, date, "v2");
        assertEquals(2L, v.getSequenceNumber());
        assertEquals(5000L, v.getTimestamp());
        assertEquals("v2", v.getReleaseName());
    }

    @Test
    void setTimestampUpdatesValue() {
        AppVersion v = new AppVersion(1L);
        v.setTimestamp(9999L);
        assertEquals(9999L, v.getTimestamp());
    }

    @Test
    void setTimestampReturnsThis() {
        AppVersion v = new AppVersion(1L);
        AppVersion returned = v.setTimestamp(1234L);
        assertEquals(v, returned);
    }

    @Test
    void getTimestampWhenNegativeReturnsCurrentTime() {
        AppVersion v = new AppVersion(1L, -1L, "");
        long ts = v.getTimestamp();
        assertTrue(ts > 0, "getTimestamp should return a positive value when stored is negative");
    }

    @Test
    void setReleaseNameUpdatesValue() {
        AppVersion v = new AppVersion(1L);
        v.setReleaseName("v1.2.3");
        assertEquals("v1.2.3", v.getReleaseName());
    }

    @Test
    void setReleaseNameReturnsThis() {
        AppVersion v = new AppVersion(1L);
        AppVersion returned = v.setReleaseName("x");
        assertEquals(v, returned);
    }

    @Test
    void setNumClassesAndGet() {
        AppVersion v = new AppVersion(1L);
        v.setNumClasses(100);
        assertEquals(100, v.getNumClasses());
    }

    @Test
    void setNumClassesReturnsThis() {
        AppVersion v = new AppVersion(1L);
        AppVersion returned = v.setNumClasses(50);
        assertEquals(v, returned);
    }

    @Test
    void setCodeSizeAndGet() {
        AppVersion v = new AppVersion(1L);
        v.setCodeSize(2048);
        assertEquals(2048, v.getCodeSize());
    }

    @Test
    void setCodeSizeReturnsThis() {
        AppVersion v = new AppVersion(1L);
        AppVersion returned = v.setCodeSize(1024);
        assertEquals(v, returned);
    }

    @Test
    void cloneProducesEqualButDistinctObject() {
        AppVersion v = new AppVersion(3L, 7777L, "clone-test");
        v.setNumClasses(200);
        v.setCodeSize(4096);
        AppVersion clone = (AppVersion) v.clone();

        assertNotNull(clone);
        assertNotSame(v, clone);
        assertEquals(v.getSequenceNumber(), clone.getSequenceNumber());
        assertEquals(v.getTimestamp(), clone.getTimestamp());
        assertEquals(v.getReleaseName(), clone.getReleaseName());
        assertEquals(v.getNumClasses(), clone.getNumClasses());
        assertEquals(v.getCodeSize(), clone.getCodeSize());
    }

    @Test
    void writeXMLProducesExpectedAttributes() throws IOException {
        AppVersion v = new AppVersion(5L, 12345L, "test-release");
        v.setNumClasses(10);
        v.setCodeSize(500);

        String xml = writeXMLAndGetStringOutput(v);
        assertTrue(xml.contains("sequence=\"5\""), "should contain sequence");
        assertTrue(xml.contains("timestamp=\"12345\""), "should contain timestamp");
        assertTrue(xml.contains("release=\"test-release\""), "should contain release name");
        assertTrue(xml.contains("numClasses=\"10\""), "should contain numClasses");
        assertTrue(xml.contains("codeSize=\"500\""), "should contain codeSize");
    }

    @Test
    void toStringContainsSequenceAndReleaseName() {
        AppVersion v = new AppVersion(7L, 100L, "my-release");
        String str = v.toString();
        assertTrue(str.contains("7"), "toString should contain sequence number");
        assertTrue(str.contains("my-release"), "toString should contain release name");
    }

    private String writeXMLAndGetStringOutput(AppVersion version) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(outputStream);
        version.writeXML(xmlOutput);
        xmlOutput.finish();
        return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
