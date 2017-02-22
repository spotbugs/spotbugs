package com.github.spotbugs.reporting;

import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.Internal;

/**
 * Code borrowed from https://github.com/gradle/gradle/blob/f58f3439c50ac0ee72d5552e5381bfcfaa25d1bd/subprojects/code-quality/src/main/groovy/org/gradle/api/plugins/quality/SpotBugsReports.java
 */
public interface SpotBugsReports extends ReportContainer<SingleFileReport>{
  /**
   * The SpotBugs XML report
   *
   * @return The SpotBugs XML report
   */
  @Internal
  SpotBugsXmlReport getXml();

  /**
   * The SpotBugs HTML report
   *
   * @return The SpotBugs HTML report
   */
  @Internal
  SingleFileReport getHtml();

  /**
   * The SpotBugs Text report
   *
   * @return The SpotBugs Text report
   */
  @Internal
  SingleFileReport getText();

  /**
   * The SpotBugs Emacs report
   *
   * @return The SpotBugs Emacs report
   */
  @Internal
  SingleFileReport getEmacs();
}
