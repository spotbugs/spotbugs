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
  public void foo() throws Exception{
    String buildScript = "buildscript {\n" + 
        "    repositories {\n" +
        "      jcenter()\n" +
        "      flatDir {\n" + 
        "         dirs '/Users/jscancella/development/repos/spotbugs/gradlePlugin/build/libs'\n" + 
        "      }\n" + 
        "    }\n" + 
        "    dependencies {\n" + 
        "        classpath 'com.github.spotbugs:gradlePlugin:1.0'\n" + 
        "        classpath 'com.google.guava:guava:21.0'\n" +
        "    }\n" + 
        "}\n" + 
        "apply plugin: 'com.github.spotbugs'"; //TODO
    File sourceDir = folder.newFolder("src", "main", "java");
    File to = new File(sourceDir, "foo.java");
    File from = new File("src/main/java/com/github/spotbugs/SpotBugsPlugin.java");
    Files.copy(from, to);
    
    File buildFile = folder.newFile("build.gradle");
    Files.write(buildScript.getBytes(), buildFile);
    
    
    BuildResult result = GradleRunner.create().withProjectDir(folder.getRoot()).withArguments(Arrays.asList("tasks")).withPluginClasspath().build();
    System.err.println(result.getOutput());
    result.getOutput().contains("spotBugs");
    
    
    //assertEquals(TaskOutcome.SUCCESS, result.task("").getOutcome());
  }
}
