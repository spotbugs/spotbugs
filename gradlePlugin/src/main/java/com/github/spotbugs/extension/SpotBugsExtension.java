package com.github.spotbugs.extension;

import java.io.File;
import java.util.Collection;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CodeQualityExtension;
import org.gradle.api.resources.TextResource;

/**
 * Used for defining the artifactory closure where you can configure the settings
 * A lot of this was borrowed from https://github.com/gradle/gradle/blob/361506e94a513eed9b173cc83492bcc425673c1d/subprojects/code-quality/src/main/groovy/org/gradle/api/plugins/quality/FindBugsExtension.java
 */
public class SpotBugsExtension extends CodeQualityExtension{
  private final Project project;
  private String effort;
  private String reportLevel;
  private Collection<String> visitors;
  private Collection<String> omitVisitors;
  private TextResource includeFilterConfig;
  private TextResource excludeFilterConfig;
  private TextResource excludeBugsFilterConfig;
  private Collection<String> extraArgs;
  
  public SpotBugsExtension(Project project) {
    this.project = project;
}
  
  /**
   * The analysis effort level.
   * The value specified should be one of {@code min}, {@code default}, or {@code max}.
   * Higher levels increase precision and find more bugs at the expense of running time and memory consumption.
   */
  public String getEffort() {
    return effort;
  }
  public void setEffort(String effort) {
    this.effort = effort;
  }
  
  /**
   * The priority threshold for reporting bugs.
   * If set to {@code low}, all bugs are reported.
   * If set to {@code medium} (the default), medium and high priority bugs are reported.
   * If set to {@code high}, only high priority bugs are reported.
   */
  public String getReportLevel() {
    return reportLevel;
  }
  public void setReportLevel(String reportLevel) {
    this.reportLevel = reportLevel;
  }
  
  /**
   * The bug detectors which should be run.
   * The bug detectors are specified by their class names, without any package qualification.
   * By default, all detectors which are not disabled by default are run.
   */
  public Collection<String> getVisitors() {
    return visitors;
  }
  public void setVisitors(Collection<String> visitors) {
    this.visitors = visitors;
  }
  
  /**
   * Similar to {@code visitors} except that it specifies bug detectors which should not be run.
   * By default, no visitors are omitted.
   */
  public Collection<String> getOmitVisitors() {
    return omitVisitors;
  }
  public void setOmitVisitors(Collection<String> omitVisitors) {
    this.omitVisitors = omitVisitors;
  }
  
  /**
   * A filter specifying which bugs are reported. Replaces the {@code includeFilter} property.
   *
   * @since 2.2
   */
  public TextResource getIncludeFilterConfig() {
    return includeFilterConfig;
  }
  public void setIncludeFilterConfig(TextResource includeFilterConfig) {
    this.includeFilterConfig = includeFilterConfig;
  }
  public TextResource getExcludeFilterConfig() {
    return excludeFilterConfig;
  }
  
  /**
   * The filename of a filter specifying which bugs are reported.
   */
  public File getIncludeFilter() {
      TextResource includeFilterConfig = getIncludeFilterConfig();
      if (includeFilterConfig == null) {
          return null;
      }
      return includeFilterConfig.asFile();
  }

  /**
   * The filename of a filter specifying which bugs are reported.
   */
  public void setIncludeFilter(File filter) {
      setIncludeFilterConfig(project.getResources().getText().fromFile(filter));
  }

  
  public void setExcludeFilterConfig(TextResource excludeFilterConfig) {
    this.excludeFilterConfig = excludeFilterConfig;
  }
  public TextResource getExcludeBugsFilterConfig() {
    return excludeBugsFilterConfig;
  }
  public void setExcludeBugsFilterConfig(TextResource excludeBugsFilterConfig) {
    this.excludeBugsFilterConfig = excludeBugsFilterConfig;
  }
  
  /**
   * The filename of a filter specifying baseline bugs to exclude from being reported.
   */
  public File getExcludeBugsFilter() {
      TextResource excludeBugsFilterConfig = getExcludeBugsFilterConfig();
      if (excludeBugsFilterConfig == null) {
          return null;
      }
      return excludeBugsFilterConfig.asFile();
  }

  /**
   * The filename of a filter specifying baseline bugs to exclude from being reported.
   */
  public void setExcludeBugsFilter(File filter) {
      setExcludeBugsFilterConfig(project.getResources().getText().fromFile(filter));
  }
  
  /**
   * Any additional arguments (not covered here more explicitly like {@code effort}) to be passed along to FindBugs.
   * <p>
   * Extra arguments are passed to FindBugs after the arguments Gradle understands (like {@code effort} but before the list of classes to analyze.
   * This should only be used for arguments that cannot be provided by Gradle directly.
   * Gradle does not try to interpret or validate the arguments before passing them to FindBugs.
   * <p>
   * See the <a href="https://code.google.com/p/findbugs/source/browse/findbugs/src/java/edu/umd/cs/findbugs/TextUICommandLine.java">FindBugs
   * TextUICommandLine source</a> for available options.
   *
   * @since 2.6
   */
  public Collection<String> getExtraArgs() {
    return extraArgs;
  }
  public void setExtraArgs(Collection<String> extraArgs) {
    this.extraArgs = extraArgs;
  }
}
