package com.github.spotbugs;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.github.spotbugs.extension.SpotBugsExtension;
import com.github.spotbugs.task.SpotBugsTask;

public class SpotBugsPlugin implements Plugin<Project>{
  private static final Logger logger = Logging.getLogger(SpotBugsPlugin.class);
  
  @Override
  public void apply(final Project project) {
    //define the spotbugs extension closure
    logger.debug("creating the spotbugs extension");
    project.getExtensions().create("spotbugs", SpotBugsExtension.class, project);
    
    
    project.getTasks().create("spotBugs", SpotBugsTask.class);
  }
  
}
