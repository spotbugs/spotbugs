package com.github.spotbugs;

import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.Internal;

/**
 * The reporting configuration for the {@link SpotBugsTask} task.
 *
 * Only one of the reports can be enabled when the task executes. If more than one is enabled, an {@link org.gradle.api.InvalidUserDataException}
 * will be thrown.
 */
public interface SpotBugsReports extends ReportContainer<SingleFileReport> {

    /**
     * The spotbugs XML report
     *
     * @return The spotbugs XML report
     */
    @Internal
    SpotBugsXmlReport getXml();

    /**
     * The spotbugs HTML report
     *
     * @return The spotbugs HTML report
     */
    @Internal
    SingleFileReport getHtml();

    /**
     * The spotbugs Text report
     *
     * @return The spotbugs Text report
     */
    @Internal
    SingleFileReport getText();

    /**
     * The spotbugs Emacs report
     *
     * @return The spotbugs Emacs report
     */
    @Internal
    SingleFileReport getEmacs();
}