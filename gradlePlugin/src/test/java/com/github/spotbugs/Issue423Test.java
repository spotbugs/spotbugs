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
package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * If reports.xml.destination is null, we should print user friendly error message
 * @since 3.1.0
 */
public class Issue423Test {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Before
    public void createProject() throws IOException {
      String buildScript = "plugins {\n" +
        "  id 'java'\n" +
        "  id 'com.github.spotbugs'\n" +
        "}\n" +
        "version = 1.0\n" +
        "repositories {\n" +
        "  mavenCentral()\n" +
        "  mavenLocal()\n" +
        "}\n" +
        "task spotbugs(type: com.github.spotbugs.SpotBugsTask) {\n" +
        "  reports {\n" +
        "    xml.enabled true\n" +
        "    xml.destination ((java.io.File) null)\n" +
        "  }\n" +
        "}";
      File buildFile = folder.newFile("build.gradle");
      Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

      File sourceDir = folder.newFolder("src", "main", "java");
      File to = new File(sourceDir, "Foo.java");
      File from = new File("src/test/java/com/github/spotbugs/Foo.java");
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    @Test
    public void testSpotBugsTaskCanRun() throws Exception {
        BuildResult result = GradleRunner.create().withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("classes", "spotbugs")).withPluginClasspath().buildAndFail();
        assertThat(result.getOutput(),
                containsString(
                        "Task 'spotbugs' has no destination for XML report. Set reports.xml.destination to this task."));
    }
}
