/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;

import edu.umd.cs.findbugs.charsets.UTF8;

/**
 * Base class for BugReporters which provides convenient formatting and
 * reporting of warnings and analysis errors.
 *
 * <p>
 * "TextUIBugReporter" is a bit of a misnomer, since this class is useful in
 * GUIs, too.
 * </p>
 *
 * @author David Hovemeyer
 */
public abstract class TextUIBugReporter extends AbstractBugReporter {
    private boolean reportStackTrace;

    private boolean useLongBugCodes = false;

    private boolean showRank = false;

    private boolean reportHistory = false;

    private boolean reportUserDesignations = false;

    private boolean applySuppressions = false;

    static final String OTHER_CATEGORY_ABBREV = "X";

    protected PrintWriter outputStream = UTF8.printWriter(System.out, true);

    public TextUIBugReporter() {
        reportStackTrace = true;
    }

    /**
     * Set the PrintStream to write bug output to.
     *
     * @param outputStream
     *            the PrintStream to write bug output to
     */
    public void setOutputStream(PrintStream outputStream) {
        this.outputStream = UTF8.printWriter(outputStream, true);
    }

    public void setWriter(PrintWriter writer) {
        this.outputStream = writer;
    }

    /**
     * Set whether or not stack traces should be reported in error output.
     *
     * @param reportStackTrace
     *            true if stack traces should be reported, false if not
     */
    public void setReportStackTrace(boolean reportStackTrace) {
        this.reportStackTrace = reportStackTrace;
    }

    /**
     * Print bug in one-line format.
     *
     * @param bugInstance
     *            the bug to print
     */
    protected void printBug(BugInstance bugInstance) {
        if (showRank) {
            int rank = BugRanker.findRank(bugInstance);
            outputStream.printf("%2d ", rank);
        }
        switch (bugInstance.getPriority()) {
        case Priorities.EXP_PRIORITY:
            outputStream.print("E ");
            break;
        case Priorities.LOW_PRIORITY:
            outputStream.print("L ");
            break;
        case Priorities.NORMAL_PRIORITY:
            outputStream.print("M ");
            break;
        case Priorities.HIGH_PRIORITY:
            outputStream.print("H ");
            break;
        default:
            assert false;
        }

        BugPattern pattern = bugInstance.getBugPattern();
        if (pattern != null) {
            String categoryAbbrev = null;
            BugCategory bcat = DetectorFactoryCollection.instance().getBugCategory(pattern.getCategory());
            if (bcat != null) {
                categoryAbbrev = bcat.getAbbrev();
            }
            if (categoryAbbrev == null) {
                categoryAbbrev = OTHER_CATEGORY_ABBREV;
            }
            outputStream.print(categoryAbbrev);
            outputStream.print(" ");
        }

        if (useLongBugCodes) {
            outputStream.print(bugInstance.getType());
            outputStream.print(" ");
        }
        if (reportUserDesignations) {
            outputStream.print(bugInstance.getUserDesignationKey());
            outputStream.print(" ");
        }

        if (reportHistory) {
            long first = bugInstance.getFirstVersion();
            long last = bugInstance.getLastVersion();
            outputStream.print(first);
            outputStream.print(" ");
            outputStream.print(last);
            outputStream.print(" ");
        }
        SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation();
        outputStream.println(bugInstance.getMessage().replace('\n', ' ') + "  " + line.toString());
    }

    private boolean analysisErrors;

    private boolean missingClasses;

    @Override
    public void reportQueuedErrors() {
        boolean errors = analysisErrors || missingClasses || getQueuedErrors().size() > 0;
        analysisErrors = missingClasses = false;
        super.reportQueuedErrors();
        if (errors) {
            emitLine("");
        }
    }

    @Override
    public void reportAnalysisError(AnalysisError error) {
        if (!analysisErrors) {
            emitLine("The following errors occurred during analysis:");
            analysisErrors = true;
        }
        emitLine("\t" + error.getMessage());
        if (error.getExceptionMessage() != null) {
            emitLine("\t\t" + error.getExceptionMessage());
            if (reportStackTrace) {
                String[] stackTrace = error.getStackTrace();
                if (stackTrace != null) {
                    for (String aStackTrace : stackTrace) {
                        emitLine("\t\t\tAt " + aStackTrace);
                    }
                }
            }
        }
    }

    @Override
    public void reportMissingClass(String message) {
        if (!missingClasses) {
            emitLine("The following classes needed for analysis were missing:");
            missingClasses = true;
        }
        emitLine("\t" + message);
    }

    /**
     * Emit one line of the error message report. By default, error messages are
     * printed to System.err. Subclasses may override.
     *
     * @param line
     *            one line of the error report
     */
    protected void emitLine(String line) {
        line = line.replaceAll("\t", "  ");
        System.err.println(line);
    }

    public boolean getUseLongBugCodes() {
        return useLongBugCodes;
    }

    public void setReportHistory(boolean reportHistory) {
        this.reportHistory = reportHistory;
    }

    public void setUseLongBugCodes(boolean useLongBugCodes) {
        this.useLongBugCodes = useLongBugCodes;
    }

    public void setShowRank(boolean showRank) {
        this.showRank = showRank;
    }

    public void setApplySuppressions(boolean applySuppressions) {
        this.applySuppressions = applySuppressions;
    }

    public void setReportUserDesignations(boolean reportUserDesignations) {
        this.reportUserDesignations = reportUserDesignations;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    public BugReporter getRealBugReporter() {
        return this;
    }

    /**
     * For debugging: check a BugInstance to make sure it is valid.
     *
     * @param bugInstance
     *            the BugInstance to check
     */
    protected void checkBugInstance(BugInstance bugInstance) {
        for (Iterator<BugAnnotation> i = bugInstance.annotationIterator(); i.hasNext();) {
            BugAnnotation bugAnnotation = i.next();
            if (bugAnnotation instanceof PackageMemberAnnotation) {
                PackageMemberAnnotation pkgMember = (PackageMemberAnnotation) bugAnnotation;
                if (pkgMember.getSourceLines() == null) {
                    throw new IllegalStateException("Package member " + pkgMember + " reported without source lines!");
                }
            }
        }
    }

    public boolean isApplySuppressions() {
        return applySuppressions;
    }

}

