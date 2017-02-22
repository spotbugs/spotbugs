package com.github.spotbugs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import com.github.spotbugs.extension.SpotBugsExtension;
import com.github.spotbugs.task.SpotBugsTask;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Callables;

public class SpotBugsPlugin implements Plugin<Project>{
  public static final String DEFAULT_FINDBUGS_VERSION = "3.1.0-RC1";
  private SpotBugsExtension extension;
  private Project project;
  
  @Override
  public final void apply(Project project) {
      this.project = project;

      beforeApply();
      project.getPluginManager().apply(ReportingBasePlugin.class);
      createConfigurations();
      extension = createExtension();
      configureExtensionRule();
      configureTaskRule();
      configureSourceSetRule();
      configureCheckTask();
  }
  
  private void configureCheckTask() {
    withBasePlugin(new Action<Plugin>() {
        @Override
        public void execute(Plugin plugin) {
            configureCheckTaskDependents();
        }
    });
}
  
  private void configureCheckTaskDependents() {
    final String taskBaseName = getTaskBaseName();
    project.getTasks().getByName("check").dependsOn(new Callable() {
        @Override
        public Object call() {
            return Iterables.transform(extension.getSourceSets(), new Function<SourceSet, String>() {
                @Override
                public String apply(SourceSet sourceSet) {
                    return sourceSet.getTaskName(taskBaseName, null);
                }
            });
        }
    });
}
  
  protected void withBasePlugin(Action<Plugin> action) {
    project.getPlugins().withType(JavaBasePlugin.class, action);
}

  protected String getToolName() {
      return "SpotBugs";
  }

  protected Class<SpotBugsTask> getTaskType() {
      return SpotBugsTask.class;
  }

  protected void beforeApply() {
      configureFindBugsConfigurations();
  }
  
  protected void createConfigurations() {
    Configuration configuration = project.getConfigurations().create(getConfigurationName());
    configuration.setVisible(false);
    configuration.setTransitive(true);
    configuration.setDescription("The " + getToolName() + " libraries to be used for this project.");
    // Don't need these things, they're provided by the runtime
    configuration.exclude(excludeProperties("ant", "ant"));
    configuration.exclude(excludeProperties("org.apache.ant", "ant"));
    configuration.exclude(excludeProperties("org.apache.ant", "ant-launcher"));
    configuration.exclude(excludeProperties("org.slf4j", "slf4j-api"));
    configuration.exclude(excludeProperties("org.slf4j", "jcl-over-slf4j"));
    configuration.exclude(excludeProperties("org.slf4j", "log4j-over-slf4j"));
    configuration.exclude(excludeProperties("commons-logging", "commons-logging"));
    configuration.exclude(excludeProperties("log4j", "log4j"));
}
  
  private Map<String, String> excludeProperties(String group, String module) {
    return ImmutableMap.<String, String>builder()
        .put("group", group)
        .put("module", module)
        .build();
}
  
  private void configureExtensionRule() {
    final ConventionMapping extensionMapping = conventionMappingOf(extension);
    extensionMapping.map("sourceSets", Callables.returning(new ArrayList()));
    extensionMapping.map("reportsDir", new Callable<File>() {
        @Override
        public File call() {
            return project.getExtensions().getByType(ReportingExtension.class).file(getReportName());
        }
    });
    withBasePlugin(new Action<Plugin>() {
        @Override
        public void execute(Plugin plugin) {
            extensionMapping.map("sourceSets", new Callable<SourceSetContainer>() {
                @Override
                public SourceSetContainer call() {
                    return getJavaPluginConvention().getSourceSets();
                }
            });
        }
    });
}
  
  protected JavaPluginConvention getJavaPluginConvention() {
    return project.getConvention().getPlugin(JavaPluginConvention.class);
}

private void configureTaskRule() {
    project.getTasks().withType(SpotBugsTask.class, new Action<Task>() {
        @Override
        public void execute(Task task) {
            String prunedName = task.getName().replaceFirst(getTaskBaseName(), "");
            if (prunedName.isEmpty()) {
                prunedName = task.getName();
            }
            prunedName = ("" + prunedName.charAt(0)).toLowerCase() + prunedName.substring(1);
            configureTaskDefaults((SpotBugsTask)task, prunedName);
        }
    });
}

private void configureSourceSetRule() {
    withBasePlugin(new Action<Plugin>() {
        @Override
        public void execute(Plugin plugin) {
            configureForSourceSets(getJavaPluginConvention().getSourceSets());
        }
    });
}

private void configureForSourceSets(SourceSetContainer sourceSets) {
  System.err.println(sourceSets);
    sourceSets.all(new Action<SourceSet>() {
        @Override
        public void execute(SourceSet sourceSet) {
            Task task = project.getTasks().create(sourceSet.getTaskName(getTaskBaseName(), null), SpotBugsTask.class);
            configureForSourceSet(sourceSet, (SpotBugsTask)task);
        }
    });
}

protected String getTaskBaseName() {
  return getToolName().toLowerCase();
}

protected String getConfigurationName() {
  return getToolName().toLowerCase();
}

protected String getReportName() {
  return getToolName().toLowerCase();
}

  private void configureFindBugsConfigurations() {
      Configuration configuration = project.getConfigurations().create("spotbugsPlugins");
      configuration.setVisible(false);
      configuration.setTransitive(true);
      configuration.setDescription("The SpotBugs plugins to be used for this project.");
  }

  protected SpotBugsExtension createExtension() {
      extension = project.getExtensions().create("spotbugs", SpotBugsExtension.class, project);
      extension.setToolVersion(DEFAULT_FINDBUGS_VERSION);
      return extension;
  }

  protected void configureTaskDefaults(SpotBugsTask task, String baseName) {
      task.setPluginClasspath(project.getConfigurations().getAt("spotbugsPlugins"));
      Configuration configuration = project.getConfigurations().getAt("spotbugs");
      configureDefaultDependencies(configuration);
      configureTaskConventionMapping(configuration, task);
      configureReportsConventionMapping(task, baseName);
  }

  private void configureDefaultDependencies(Configuration configuration) {
      configuration.defaultDependencies(new Action<DependencySet>() {
          @Override
          public void execute(DependencySet dependencies) {
              dependencies.add(project.getDependencies().create("com.github.spotbugs:spotbugs:" + extension.getToolVersion()));
          }
      });
  }

  private void configureTaskConventionMapping(Configuration configuration, SpotBugsTask task) {
      ConventionMapping taskMapping = task.getConventionMapping();
      taskMapping.map("spotbugsClasspath", Callables.returning(configuration));
      taskMapping.map("ignoreFailures", new Callable<Boolean>() {
          @Override
          public Boolean call() {
              return extension.isIgnoreFailures();
          }
      });
      taskMapping.map("effort", new Callable<String>() {
          @Override
          public String call() {
              return extension.getEffort();
          }
      });
      taskMapping.map("reportLevel", new Callable<String>() {
          @Override
          public String call() {
              return extension.getReportLevel();
          }
      });
      taskMapping.map("visitors", new Callable<Collection<String>>() {
          @Override
          public Collection<String> call() {
              return extension.getVisitors();
          }
      });
      taskMapping.map("omitVisitors", new Callable<Collection<String>>() {
          @Override
          public Collection<String> call() {
              return extension.getOmitVisitors();
          }
      });

      taskMapping.map("excludeFilterConfig", new Callable<TextResource>() {
          @Override
          public TextResource call() {
              return extension.getExcludeFilterConfig();
          }
      });
      taskMapping.map("includeFilterConfig", new Callable<TextResource>() {
          @Override
          public TextResource call() {
              return extension.getIncludeFilterConfig();
          }
      });
      taskMapping.map("excludeBugsFilterConfig", new Callable<TextResource>() {
          @Override
          public TextResource call() {
              return extension.getExcludeBugsFilterConfig();
          }
      });

      taskMapping.map("extraArgs", new Callable<Collection<String>>() {
          @Override
          public Collection<String> call() {
              return extension.getExtraArgs();
          }
      });
  }

  private void configureReportsConventionMapping(SpotBugsTask task, final String baseName) {
      task.getReports().all(new Action<SingleFileReport>() {
          @Override
          public void execute(final SingleFileReport report) {
              ConventionMapping reportMapping = conventionMappingOf(report);
              reportMapping.map("enabled", new Callable<Boolean>() {
                  @Override
                  public Boolean call() {
                      return report.getName().equals("xml");
                  }
              });
              reportMapping.map("destination", new Callable<File>() {
                  @Override
                  public File call() {
                      return new File(extension.getReportsDir(), baseName + "." + report.getName());
                  }
              });
          }
      });
  }
  
  protected static ConventionMapping conventionMappingOf(Object object) {
    return ((IConventionAware) object).getConventionMapping();
}

  protected void configureForSourceSet(final SourceSet sourceSet, SpotBugsTask task) {
      task.setDescription("Run FindBugs analysis for " + sourceSet.getName() + " classes");
      task.setSource(sourceSet.getAllJava());
      ConventionMapping taskMapping = task.getConventionMapping();
      taskMapping.map("classes", new Callable<FileCollection>() {
          @Override
          public FileCollection call() {
              // the simple "classes = sourceSet.output" may lead to non-existing resources directory
              // being passed to FindBugs Ant task, resulting in an error
              return project.fileTree(sourceSet.getOutput().getClassesDir()).builtBy(sourceSet.getOutput());
          }
      });
      taskMapping.map("classpath", new Callable<FileCollection>() {
          @Override
          public FileCollection call() {
              return sourceSet.getCompileClasspath();
          }
      });
  }
  
}
