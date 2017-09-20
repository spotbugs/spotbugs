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
package edu.umd.cs.findbugs.test;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test class to ensure that {@link SpotBugsExtension} really work as expected.
 *
 */
@ExtendWith(SpotBugsExtension.class)
public class SpotBugsExtensionTest {
    @Test
    public void test(SpotBugsRunner spotbugs) {
        spotbugs.performAnalysis(Paths.get("build/classes/java/main/edu/umd/cs/findbugs/test/SpotBugsRunner.class"));
    }
}
