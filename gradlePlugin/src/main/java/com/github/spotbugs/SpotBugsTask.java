package com.github.spotbugs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Incubating;
import org.gradle.api.JavaVersion;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.process.internal.worker.WorkerProcessFactory;

import com.github.spotbugs.internal.SpotBugsReportsImpl;
import com.github.spotbugs.internal.SpotBugsReportsInternal;
import com.github.spotbugs.internal.spotbugs.SpotBugsClasspathValidator;
import com.github.spotbugs.internal.spotbugs.SpotBugsResult;
import com.github.spotbugs.internal.spotbugs.SpotBugsSpec;
import com.github.spotbugs.internal.spotbugs.SpotBugsSpecBuilder;
import com.github.spotbugs.internal.spotbugs.SpotBugsWorkerManager;

import groovy.lang.Closure;

/**
 * Analyzes code with <a href="https://spotbugs.github.io/">SpotBugs</a>. See the
 * <a href="https://spotbugs.readthedocs.io/en/latest/">SpotBugs Manual</a> for additional
 * information on configuration options.
 */
@CacheableTask
public class SpotBugsTask extends SourceTask implements VerificationTask, Reporting<SpotBugsReports> {
  private FileCollection classes;

  private FileCollection classpath;

  private FileCollection spotbugsClasspath;

  private FileCollection pluginClasspath;

  private boolean ignoreFailures;

  private String effort;

  private String reportLevel;

  private String maxHeapSize;

  private Collection<String> visitors = new ArrayList<String>();

  private Collection<String> omitVisitors = new ArrayList<String>();

  private TextResource includeFilterConfig;

  private TextResource excludeFilterConfig;

  private TextResource excludeBugsFilterConfig;

  private Collection<String> extraArgs = new ArrayList<String>();

  @Nested
  private final SpotBugsReportsInternal reports;

  public SpotBugsTask() {
      reports = getInstantiator().newInstance(SpotBugsReportsImpl.class, this);
  }

  @Inject
  public Instantiator getInstantiator() {
      throw new UnsupportedOperationException();
  }

  @Inject
  public WorkerProcessFactory getWorkerProcessBuilderFactory() {
      throw new UnsupportedOperationException();
  }

  /**
   * The reports to be generated by this task.
   *
   * @return The reports container
   */
  public SpotBugsReports getReports() {
      return reports;
  }

  /**
   * Configures the reports to be generated by this task.
   *
   * The contained reports can be configured by name and closures. Example:
   *
   * <pre>
   * spotbugsTask {
   *   reports {
   *     xml {
   *       destination "build/spotbugs.xml"
   *     }
   *   }
   * }
   * </pre>
   *
   * @param closure The configuration
   * @return The reports container
   */
  public SpotBugsReports reports(Closure closure) {
      return reports(new ClosureBackedAction<SpotBugsReports>(closure));
  }

  /**
   * Configures the reports to be generated by this task.
   *
   * The contained reports can be configured by name and closures. Example:
   *
   * <pre>
   * spotbugsTask {
   *   reports {
   *     xml {
   *       destination "build/spotbugs.xml"
   *     }
   *   }
   * }
   * </pre>
   *
   *
   * @param configureAction The configuration
   * @return The reports container
   */
  public SpotBugsReports reports(Action<? super SpotBugsReports> configureAction) {
      configureAction.execute(reports);
      return reports;
  }

  /**
   * The filename of a filter specifying which bugs are reported.
   */
  @Internal
  public File getIncludeFilter() {
      TextResource config = getIncludeFilterConfig();
      return config == null ? null : config.asFile();
  }

  /**
   * The filename of a filter specifying which bugs are reported.
   */
  public void setIncludeFilter(File filter) {
      setIncludeFilterConfig(getProject().getResources().getText().fromFile(filter));
  }

  /**
   * The filename of a filter specifying bugs to exclude from being reported.
   */
  @Internal
  public File getExcludeFilter() {
      TextResource config = getExcludeFilterConfig();
      return config == null ? null : config.asFile();
  }

  /**
   * The filename of a filter specifying bugs to exclude from being reported.
   */
  public void setExcludeFilter(File filter) {
      setExcludeFilterConfig(getProject().getResources().getText().fromFile(filter));
  }

  /**
   * The filename of a filter specifying baseline bugs to exclude from being reported.
   */
  @Internal
  public File getExcludeBugsFilter() {
      TextResource config = getExcludeBugsFilterConfig();
      return config == null ? null : config.asFile();
  }

  /**
   * The filename of a filter specifying baseline bugs to exclude from being reported.
   */
  public void setExcludeBugsFilter(File filter) {
      setExcludeBugsFilterConfig(getProject().getResources().getText().fromFile(filter));
  }

  @TaskAction
  public void run() throws IOException, InterruptedException {
      new SpotBugsClasspathValidator(JavaVersion.current()).validateClasspath(
          getSpotbugsClasspath().getFiles().stream().map(File::getName).collect(Collectors.toSet()));
      SpotBugsSpec spec = generateSpec();
      SpotBugsWorkerManager manager = new SpotBugsWorkerManager();

      getLogging().captureStandardOutput(LogLevel.DEBUG);
      getLogging().captureStandardError(LogLevel.DEBUG);

      SpotBugsResult result = manager.runWorker(getProject().getProjectDir(), getWorkerProcessBuilderFactory(), getSpotbugsClasspath(), spec);
      evaluateResult(result);
  }

  SpotBugsSpec generateSpec() {
      SpotBugsSpecBuilder specBuilder = new SpotBugsSpecBuilder(getClasses())
          .withPluginsList(getPluginClasspath())
          .withSources(getSource())
          .withClasspath(getClasspath())
          .withDebugging(getLogger().isDebugEnabled())
          .withEffort(getEffort())
          .withReportLevel(getReportLevel())
          .withMaxHeapSize(getMaxHeapSize())
          .withVisitors(getVisitors())
          .withOmitVisitors(getOmitVisitors())
          .withExcludeFilter(getExcludeFilter())
          .withIncludeFilter(getIncludeFilter())
          .withExcludeBugsFilter(getExcludeBugsFilter())
          .withExtraArgs(getExtraArgs())
          .configureReports(getReports());

      return specBuilder.build();
  }

  void evaluateResult(SpotBugsResult result) {
      if (result.getException() != null) {
          throw new GradleException("SpotBugs encountered an error. Run with --debug to get more information.", result.getException());
      }

      if (result.getErrorCount() > 0) {
          throw new GradleException("SpotBugs encountered an error. Run with --debug to get more information.");
      }

      if (result.getBugCount() > 0) {
          String message = "SpotBugs rule violations were found.";
          SingleFileReport report = reports.getFirstEnabled();
          if (report != null) {
              String reportUrl = new ConsoleRenderer().asClickableFileUrl(report.getDestination());
              message += " See the report at: " + reportUrl;
          }

          if (getIgnoreFailures()) {
              getLogger().warn(message);
          } else {
              throw new GradleException(message);
          }

      }

  }

  public SpotBugsTask extraArgs(Iterable<String> arguments) {
      for (String argument : arguments) {
          extraArgs.add(argument);
      }

      return this;
  }

  public SpotBugsTask extraArgs(String... arguments) {
      extraArgs.addAll(Arrays.asList(arguments));
      return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileTree getSource() {
      return super.getSource();
  }

  /**
   * The classes to be analyzed.
   */
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  @InputFiles
  public FileCollection getClasses() {
      return classes;
  }

  public void setClasses(FileCollection classes) {
      this.classes = classes;
  }

  /**
   * Compile class path for the classes to be analyzed. The classes on this class path are used during analysis but aren't analyzed themselves.
   */
  @Classpath
  public FileCollection getClasspath() {
      return classpath;
  }

  public void setClasspath(FileCollection classpath) {
      this.classpath = classpath;
  }

  /**
   * Class path holding the SpotBugs library.
   */
  @Classpath
  public FileCollection getSpotbugsClasspath() {
      return spotbugsClasspath;
  }

  public void setSpotbugsClasspath(FileCollection spotbugsClasspath) {
      this.spotbugsClasspath = spotbugsClasspath;
  }

  /**
   * Class path holding any additional SpotBugs plugins.
   */
  @Classpath
  public FileCollection getPluginClasspath() {
      return pluginClasspath;
  }

  public void setPluginClasspath(FileCollection pluginClasspath) {
      this.pluginClasspath = pluginClasspath;
  }

  /**
   * Whether or not to allow the build to continue if there are warnings.
   */
  @Input
  @Override
  public boolean getIgnoreFailures() {
      return ignoreFailures;
  }

  public void setIgnoreFailures(boolean ignoreFailures) {
      this.ignoreFailures = ignoreFailures;
  }

  /**
   * The analysis effort level. The value specified should be one of {@code min}, {@code default}, or {@code max}. Higher levels increase precision and find more bugs at the expense of running time
   * and memory consumption.
   */
  @Input
  @Optional
  public String getEffort() {
      return effort;
  }

  public void setEffort(String effort) {
      this.effort = effort;
  }

  /**
   * The priority threshold for reporting bugs. If set to {@code low}, all bugs are reported. If set to {@code medium} (the default), medium and high priority bugs are reported. If set to {@code
   * high}, only high priority bugs are reported.
   */
  @Input
  @Optional
  public String getReportLevel() {
      return reportLevel;
  }

  public void setReportLevel(String reportLevel) {
      this.reportLevel = reportLevel;
  }

  /**
   * The maximum heap size for the forked spotbugs process (ex: '1g').
   */
  @Input
  @Optional
  public String getMaxHeapSize() {
      return maxHeapSize;
  }

  public void setMaxHeapSize(String maxHeapSize) {
      this.maxHeapSize = maxHeapSize;
  }

  /**
   * The bug detectors which should be run. The bug detectors are specified by their class names, without any package qualification. By default, all detectors which are not disabled by default are
   * run.
   */
  @Input
  @Optional
  public Collection<String> getVisitors() {
      return visitors;
  }

  public void setVisitors(Collection<String> visitors) {
      this.visitors = visitors;
  }

  /**
   * Similar to {@code visitors} except that it specifies bug detectors which should not be run. By default, no visitors are omitted.
   */
  @Input
  @Optional
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
  @Incubating
  @Nested
  @Optional
  public TextResource getIncludeFilterConfig() {
      return includeFilterConfig;
  }

  public void setIncludeFilterConfig(TextResource includeFilterConfig) {
      this.includeFilterConfig = includeFilterConfig;
  }

  /**
   * A filter specifying bugs to exclude from being reported. Replaces the {@code excludeFilter} property.
   *
   * @since 2.2
   */
  @Incubating
  @Nested
  @Optional
  public TextResource getExcludeFilterConfig() {
      return excludeFilterConfig;
  }

  public void setExcludeFilterConfig(TextResource excludeFilterConfig) {
      this.excludeFilterConfig = excludeFilterConfig;
  }

  /**
   * A filter specifying baseline bugs to exclude from being reported.
   */
  @Incubating
  @Nested
  @Optional
  public TextResource getExcludeBugsFilterConfig() {
      return excludeBugsFilterConfig;
  }

  public void setExcludeBugsFilterConfig(TextResource excludeBugsFilterConfig) {
      this.excludeBugsFilterConfig = excludeBugsFilterConfig;
  }

  /**
   * Any additional arguments (not covered here more explicitly like {@code effort}) to be passed along to SpotBugs. <p> Extra arguments are passed to SpotBugs after the arguments Gradle understands
   * (like {@code effort} but before the list of classes to analyze. This should only be used for arguments that cannot be provided by Gradle directly. Gradle does not try to interpret or validate
   * the arguments before passing them to SpotBugs. <p> See the <a href="https://code.google.com/p/spotbugs/source/browse/spotbugs/src/java/edu/umd/cs/spotbugs/TextUICommandLine.java">SpotBugs
   * TextUICommandLine source</a> for available options.
   *
   * @since 2.6
   */
  @Input
  @Optional
  public Collection<String> getExtraArgs() {
      return extraArgs;
  }

  public void setExtraArgs(Collection<String> extraArgs) {
      this.extraArgs = extraArgs;
  }

}
