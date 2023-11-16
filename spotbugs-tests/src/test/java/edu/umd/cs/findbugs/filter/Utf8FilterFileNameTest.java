/*
 * Contributions to SpotBugs
 * Copyright (C) 2023, the SpotBugs authors
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
package edu.umd.cs.findbugs.filter;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * @author gtoison
 */
class Utf8FilterFileNameTest {
    @TempDir
    private Path folderPath;

    @Test
    void loadFilter() {
        Path filterPath = folderPath.resolve("äéàùçæð.xml");

        try {
            Files.createFile(filterPath);
            Files.writeString(filterPath, "<FindBugsFilter/>");

            Filter filter = new Filter(filterPath.toAbsolutePath().toString());
        } catch (IOException e) {
            fail("Error loading filter file " + filterPath, e);
        }
    }
}
