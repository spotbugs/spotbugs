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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test class to ensure that {@link SpotBugsExtension} really work as expected.
 *
 */
@ExtendWith(SpotBugsExtension.class)
class SpotBugsExtensionTest {
    @Test
    void test(SpotBugsRunner spotbugs) {
        addAuxClassPathEntry(spotbugs, "build/spotbugs/auxclasspath/spotbugsMain");
        addAuxClassPathEntry(spotbugs, "build/spotbugs/auxclasspath/spotbugsTest");
        spotbugs.performAnalysis(Paths.get("build/classes/java/main/edu/umd/cs/findbugs/test/SpotBugsRunner.class"));
    }

    private void addAuxClassPathEntry(SpotBugsRunner spotbugs, String dir) {
        final Path dependencies = Paths.get(dir);
        try {
            final List<String> lines = Files.readAllLines(dependencies);
            for (String line : lines) {
                Path path = Paths.get(line);
                if (Files.isReadable(path)) {
                    spotbugs.addAuxClasspathEntry(e -> {
                    }, path);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
