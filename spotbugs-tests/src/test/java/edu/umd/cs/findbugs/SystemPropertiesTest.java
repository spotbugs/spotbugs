/*
 * SpotBugs - Find bugs in Java programs
 * Copyright (C) 2024, SpotBugs authors
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SystemProperties} covering property access, mutation, and
 * the classpath-based bootstrap loading introduced to break the circular
 * class-initialization chain with {@link DetectorFactoryCollection}.
 */
class SystemPropertiesTest {

    private static final String TEST_KEY = "findbugs.test.SystemPropertiesTest.key";

    @AfterEach
    void removeTestProperty() {
        SystemProperties.removeProperty(TEST_KEY);
    }

    // --- getBoolean ---

    @Test
    void getBooleanReturnsFalseWhenNotSet() {
        assertFalse(SystemProperties.getBoolean(TEST_KEY));
    }

    @Test
    void getBooleanReturnsTrueWhenSetToTrue() {
        SystemProperties.setProperty(TEST_KEY, "true");
        assertTrue(SystemProperties.getBoolean(TEST_KEY));
    }

    @Test
    void getBooleanIsCaseInsensitive() {
        SystemProperties.setProperty(TEST_KEY, "TRUE");
        assertTrue(SystemProperties.getBoolean(TEST_KEY));
    }

    @Test
    void getBooleanReturnsFalseWhenSetToFalse() {
        SystemProperties.setProperty(TEST_KEY, "false");
        assertFalse(SystemProperties.getBoolean(TEST_KEY));
    }

    @Test
    void getBooleanWithDefaultReturnsDefaultWhenNotSet() {
        assertTrue(SystemProperties.getBoolean(TEST_KEY, true));
        assertFalse(SystemProperties.getBoolean(TEST_KEY, false));
    }

    @Test
    void getBooleanWithDefaultReturnsParsedValueWhenSet() {
        SystemProperties.setProperty(TEST_KEY, "true");
        assertTrue(SystemProperties.getBoolean(TEST_KEY, false));
    }

    // --- getProperty / setProperty / removeProperty ---

    @Test
    void getPropertyReturnsNullWhenNotSet() {
        assertNull(SystemProperties.getProperty(TEST_KEY));
    }

    @Test
    void setAndGetPropertyRoundTrip() {
        SystemProperties.setProperty(TEST_KEY, "hello");
        assertEquals("hello", SystemProperties.getProperty(TEST_KEY));
    }

    @Test
    void removePropertyMakesPropertyAbsent() {
        SystemProperties.setProperty(TEST_KEY, "value");
        SystemProperties.removeProperty(TEST_KEY);
        assertNull(SystemProperties.getProperty(TEST_KEY));
    }

    @Test
    void getPropertyWithDefaultReturnsDefaultWhenNotSet() {
        assertEquals("default", SystemProperties.getProperty(TEST_KEY, "default"));
    }

    @Test
    void getPropertyWithDefaultReturnsValueWhenSet() {
        SystemProperties.setProperty(TEST_KEY, "set-value");
        assertEquals("set-value", SystemProperties.getProperty(TEST_KEY, "default"));
    }

    // --- getInt ---

    @Test
    void getIntReturnsDefaultWhenNotSet() {
        assertEquals(42, SystemProperties.getInt(TEST_KEY, 42));
    }

    @Test
    void getIntReturnsParsedValueWhenSet() {
        SystemProperties.setProperty(TEST_KEY, "7");
        assertEquals(7, SystemProperties.getInt(TEST_KEY, 0));
    }

    @Test
    void getIntReturnsDefaultWhenValueIsNotNumeric() {
        SystemProperties.setProperty(TEST_KEY, "not-a-number");
        assertEquals(99, SystemProperties.getInt(TEST_KEY, 99));
    }

    // --- getLocalProperties / getAllProperties ---

    @Test
    void getLocalPropertiesReturnsNonNull() {
        assertNotNull(SystemProperties.getLocalProperties());
    }

    @Test
    void getAllPropertiesReturnsNonNull() {
        assertNotNull(SystemProperties.getAllProperties());
    }

    @Test
    void getAllPropertiesContainsLocalProperty() {
        SystemProperties.setProperty(TEST_KEY, "all-props-value");
        Properties all = SystemProperties.getAllProperties();
        assertEquals("all-props-value", all.getProperty(TEST_KEY));
    }

    // --- loadPropertiesFromURL ---

    @Test
    void loadPropertiesFromNullUrlIsNoOp() {
        // Must not throw; exercises the null-guard in loadPropertiesFromURL
        SystemProperties.loadPropertiesFromURL(null);
    }

    @Test
    void loadPropertiesFromUrlLoadsProperties() throws IOException {
        Path tmp = Files.createTempFile("spotbugs-test", ".properties");
        try {
            Files.writeString(tmp, TEST_KEY + "=loaded-from-url\n");
            URL url = tmp.toUri().toURL();
            SystemProperties.loadPropertiesFromURL(url);
            assertEquals("loaded-from-url", SystemProperties.getProperty(TEST_KEY));
        } finally {
            Files.deleteIfExists(tmp);
            SystemProperties.removeProperty(TEST_KEY);
        }
    }

    // --- rewriteURLAccordingToProperties ---

    @Test
    void rewriteURLReturnsUrlUnchangedWhenNoPatternConfigured() {
        String url = "https://example.com/some/path";
        assertEquals(url, SystemProperties.rewriteURLAccordingToProperties(url));
    }

    // --- Class initialization ---

    @Test
    void systemPropertiesInitializesWithoutError() {
        // Accessing ASSERTIONS_ENABLED forces the class to be initialized (it already is,
        // but this makes the coverage path explicit). The key requirement is that
        // loadPropertiesFromConfigFile() no longer routes through
        // DetectorFactoryCollection.getCoreResource(), so there is no circular <clinit>.
        assertFalse(SystemProperties.RUNNING_IN_ECLIPSE);
    }
}
