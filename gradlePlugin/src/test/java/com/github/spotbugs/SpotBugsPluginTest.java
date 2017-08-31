package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class SpotBugsPluginTest extends Assert{
  @Rule
  public TemporaryFolder folder= new TemporaryFolder();

  /**
   * The root directory of generated project by {@link #createProject()}
   */
  private File root;

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
      "}";
    root = folder.newFolder();
    File buildFile = new File(root, "build.gradle");
    Files.write(buildScript.getBytes(), buildFile);

    File sourceDir = new File(root, "src/main/java");
    assertTrue(sourceDir.mkdirs());

    File to = new File(sourceDir, "Foo.java");
    File from = new File("src/test/java/com/github/spotbugs/Foo.java");
    Files.copy(from, to);
  }

  @Test
  public void TestSpotBugsTasksExist() throws Exception{
    BuildResult result = GradleRunner.create().withProjectDir(root).withArguments(Arrays.asList("tasks", "--all")).withPluginClasspath().build();
    assertTrue(result.getOutput().contains("spotbugsMain"));
    assertTrue(result.getOutput().contains("spotbugsTest"));
  }

  @Test
  public void testSpotBugsTaskCanRun() throws Exception {
    BuildResult result = GradleRunner.create()
            .withProjectDir(root)
            .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
            .withPluginClasspath().build();
    Optional<BuildTask> spotbugsMain = result.getTasks().stream()
            .filter(task -> task.getPath().equals(":spotbugsMain"))
            .findAny();
    assertTrue(spotbugsMain.isPresent());
    assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.SUCCESS));
  }

  @Test
  public void testLoadToolVersion() {
    SpotBugsPlugin plugin = new SpotBugsPlugin();
    assertThat(plugin.loadToolVersion(), is(notNullValue()));
  }
}
