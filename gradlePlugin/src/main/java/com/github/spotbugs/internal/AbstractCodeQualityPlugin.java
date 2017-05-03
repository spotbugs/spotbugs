package com.github.spotbugs.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.plugins.quality.CodeQualityExtension;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Callables;

public abstract class AbstractCodeQualityPlugin<T> implements Plugin<ProjectInternal> {

  protected static ConventionMapping conventionMappingOf(Object object) {
      return ((IConventionAware) object).getConventionMapping();
  }

  protected ProjectInternal project;
  protected CodeQualityExtension extension;

  @Override
  public final void apply(ProjectInternal project) {
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

  protected abstract String getToolName();

  protected abstract Class<T> getTaskType();

  private Class<? extends Task> getCastedTaskType() {
      return (Class<? extends Task>) getTaskType();
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

  protected Class<? extends Plugin> getBasePlugin() {
      return JavaBasePlugin.class;
  }

  protected void beforeApply() {
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

  protected abstract CodeQualityExtension createExtension();

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

  private void configureTaskRule() {
      project.getTasks().withType(getCastedTaskType(), new Action<Task>() {
          @Override
          public void execute(Task task) {
              String prunedName = task.getName().replaceFirst(getTaskBaseName(), "");
              if (prunedName.isEmpty()) {
                  prunedName = task.getName();
              }
              prunedName = ("" + prunedName.charAt(0)).toLowerCase() + prunedName.substring(1);
              configureTaskDefaults((T) task, prunedName);
          }
      });
  }

  protected void configureTaskDefaults(T task, String baseName) {
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
      sourceSets.all(new Action<SourceSet>() {
          @Override
          public void execute(SourceSet sourceSet) {
              Task task = project.getTasks().create(sourceSet.getTaskName(getTaskBaseName(), null), getCastedTaskType());
              configureForSourceSet(sourceSet, (T)task);
          }
      });
  }

  protected void configureForSourceSet(SourceSet sourceSet, T task) {
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
      project.getPlugins().withType(getBasePlugin(), action);
  }

  protected JavaPluginConvention getJavaPluginConvention() {
      return project.getConvention().getPlugin(JavaPluginConvention.class);
  }
}