package com.github.spotbugs;

import org.gradle.api.Incubating;
import org.gradle.api.reporting.SingleFileReport;
import org.gradle.api.tasks.Internal;

/**
 * The single file XML report for SpotBugs.
 */
@Incubating
public interface SpotBugsXmlReport extends SingleFileReport {
    /**
     * Whether or not SpotBugs should generate XML augmented with human-readable messages.
     * You should use this format if you plan to generate a report using an XSL stylesheet.
     * <p>
     * If {@code true}, SpotBugs will augment the XML with human-readable messages.
     * If {@code false}, SpotBugs will not augment the XML with human-readable messages.
     *
     * @return Whether or not SpotBugs should generate XML augmented with human-readable messages.
     */
    @Internal
    boolean isWithMessages();

    /**
     * Whether or not SpotBugs should generate XML augmented with human-readable messages.
     *
     * @see #isWithMessages()
     * @param withMessages Whether or not SpotBugs should generate XML augmented with human-readable messages.
     */
    void setWithMessages(boolean withMessages);

}