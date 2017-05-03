package com.github.spotbugs;

import java.io.File;
import java.util.Arrays;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class SpotBugsPluginTest extends Assert{
  @Rule
  public TemporaryFolder folder= new TemporaryFolder();
  
  @Test
  public void TestFindBugsTasksExist() throws Exception{
    String buildScript = "plugins {\n" + 
        "  id 'java'\n" +
        "  id 'com.github.spotbugs'\n" + 
        "}\n" + 
        "version = 1.0\n" + 
        "repositories {\n" + 
        "  mavenCentral()\n" + 
        "}";
    
    File sourceDir = folder.newFolder("src", "main", "java");
    File to = new File(sourceDir, "foo.java");
    File from = new File("src/test/java/com/github/spotbugs/Foo.java");
    Files.copy(from, to);
    
    File buildFile = folder.newFile("build.gradle");
    Files.write(buildScript.getBytes(), buildFile);
    
    
    BuildResult result = GradleRunner.create().withProjectDir(folder.getRoot()).withArguments(Arrays.asList("tasks", "--all")).withPluginClasspath().build();
    assertTrue(result.getOutput().contains("findbugsMain"));
    assertTrue(result.getOutput().contains("findbugsTest"));
  }
}
