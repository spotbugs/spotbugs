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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DetectorFactoryCollection}.
 */
class DetectorFactoryCollectionTest {

    private static final String JAWS_DEBUG_PROP = "findbugs.jaws.debug";

    private final String previousJawsDebugProperty = System.getProperty(JAWS_DEBUG_PROP);

    @AfterEach
    void clearJawsDebugProperty() {
        SystemProperties.removeProperty(JAWS_DEBUG_PROP);
        if (previousJawsDebugProperty == null) {
            System.clearProperty(JAWS_DEBUG_PROP);
        } else {
            System.setProperty(JAWS_DEBUG_PROP, previousJawsDebugProperty);
        }
    }

    /**
     * Calling {@code jawsDebugMessage} when {@code findbugs.jaws.debug} is {@code false}
     * must not throw.
     */
    @Test
    void jawsDebugMessageDoesNotThrowWhenDebugIsDisabled() {
        SystemProperties.setProperty(JAWS_DEBUG_PROP, "false");
        assertDoesNotThrow(() -> DetectorFactoryCollection.jawsDebugMessage("test message"));
    }

    /**
     * Verify that instantiating {@link DetectorFactoryCollection} with a single
     * {@link Plugin} completes without error.
     */
    @Test
    void instanceCreationForSinglePluginDoesNotThrow() {
        Plugin corePlugin = PluginLoader.getCorePluginLoader().getPlugin();
        assertNotNull(corePlugin);
        DetectorFactoryCollection dfc =
                assertDoesNotThrow(() -> new DetectorFactoryCollection(corePlugin));
        assertNotNull(dfc);
    }
}
