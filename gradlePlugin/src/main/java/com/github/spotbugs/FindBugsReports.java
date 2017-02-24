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
public interface FindBugsReports extends ReportContainer<SingleFileReport> {

    /**
     * The findbugs XML report
     *
     * @return The findbugs XML report
     */
    @Internal
    FindBugsXmlReport getXml();

    /**
     * The findbugs HTML report
     *
     * @return The findbugs HTML report
     */
    @Internal
    SingleFileReport getHtml();

    /**
     * The findbugs Text report
     *
     * @return The findbugs Text report
     */
    @Internal
    SingleFileReport getText();

    /**
     * The findbugs Emacs report
     *
     * @return The findbugs Emacs report
     */
    @Internal
    SingleFileReport getEmacs();
}