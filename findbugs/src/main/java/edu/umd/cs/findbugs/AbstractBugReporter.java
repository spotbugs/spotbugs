/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007, University of Maryland
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * An abstract class which provides much of the functionality required of all
 * BugReporter objects.
 */
public abstract class AbstractBugReporter implements BugReporter {
    private static final boolean DEBUG = SystemProperties.getBoolean("abreporter.debug");

    private static final boolean DEBUG_MISSING_CLASSES = SystemProperties.getBoolean("findbugs.debug.missingclasses");

    protected static class Error {
        private final int sequence;

        private final String message;

        private final Throwable cause;

        public Error(int sequence, String message) {
            this(sequence, message, null);
        }

        public Error(int sequence, String message, Throwable cause) {
            this.sequence = sequence;
            this.message = message;
            this.cause = cause;
        }

        public int getSequence() {
            return sequence;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public int hashCode() {
            int hashCode = message.hashCode();
            if (cause != null) {
                hashCode += 1009 * cause.hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Error other = (Error) obj;
            if (!message.equals(other.message)) {
                return false;
            }
            if (this.cause == other.cause) {
                return true;
            }
            if (this.cause == null || other.cause == null) {
                return false;
            }
            return this.cause.equals(other.cause);
        }
    }

    private int verbosityLevel;

    private int priorityThreshold;

    private int rankThreshold;

    private boolean relaxedSet, relaxed;

    private int errorCount;

    private final Set<String> missingClassMessageList;

    private final Set<Error> errorSet;

    private final List<BugReporterObserver> observerList;

    private final ProjectStats projectStats;

    public AbstractBugReporter() {
        super();
        verbosityLevel = NORMAL;
        missingClassMessageList = new LinkedHashSet<String>();
        errorSet = new HashSet<Error>();
        observerList = new LinkedList<BugReporterObserver>();
        projectStats = new ProjectStats();
        // bug 2815983: no bugs are reported anymore
        // there is no info which value should be default, so using the max
        rankThreshold = BugRanker.VISIBLE_RANK_MAX;
    }

    @Override
    public void setErrorVerbosity(int level) {
        this.verbosityLevel = level;
    }

    @Override
    public void setPriorityThreshold(int threshold) {
        this.priorityThreshold = threshold;
    }

    public void setRankThreshold(int threshold) {
        this.rankThreshold = threshold;
    }

    public void setIsRelaxed(boolean relaxed) {
        this.relaxed = relaxed;
        this.relaxedSet = true;
    }

    protected boolean isRelaxed() {
        if (!relaxedSet) {
            if (FindBugsAnalysisFeatures.isRelaxedMode()) {
                relaxed = true;
            }

            relaxedSet = true;
        }
        return relaxed;
    }
    // Subclasses must override doReportBug(), not this method.
    @Override
    public final void reportBug(@Nonnull BugInstance bugInstance) {
        if (isRelaxed()) {
            doReportBug(bugInstance);
            return;
        }
        if (priorityThreshold == 0) {
            throw new IllegalStateException("Priority threshold not set");
        }

        ClassAnnotation primaryClass = bugInstance.getPrimaryClass();
        if (primaryClass != null && !AnalysisContext.currentAnalysisContext().isApplicationClass(primaryClass.getClassName())) {
            if (DEBUG) {
                System.out.println("AbstractBugReporter: Filtering due to non-primary class");
            }
            return;
        }
        int priority = bugInstance.getPriority();
        int bugRank = bugInstance.getBugRank();
        if (priority <= priorityThreshold && bugRank <= rankThreshold) {
            doReportBug(bugInstance);
        } else {
            if (DEBUG) {
                if (priority <= priorityThreshold) {
                    System.out.println("AbstractBugReporter: Filtering due to priorityThreshold " + priority + " > "
                            + priorityThreshold);
                } else {
                    System.out.println("AbstractBugReporter: Filtering due to rankThreshold " + bugRank + " > " + rankThreshold);
                }
            }
        }
    }

    public final void reportBugsFromXml(@WillClose InputStream in, Project theProject) throws IOException, DocumentException {
        SortedBugCollection theCollection = new SortedBugCollection(theProject);
        theCollection.readXML(in);
        for (BugInstance bug : theCollection.getCollection()) {
            doReportBug(bug);
        }
    }

    public static @CheckForNull @DottedClassName
    String getMissingClassName(ClassNotFoundException ex) {

        // Try to decode the error message by extracting the class name.
        String className = ClassNotFoundExceptionParser.getMissingClassName(ex);
        if (className != null) {
            ClassName.assertIsDotted(className);
            return className;
        }

        return null;
    }

    @Override
    public void reportMissingClass(ClassNotFoundException ex) {
        if (DEBUG_MISSING_CLASSES) {
            System.out.println("Missing class: " + ex.toString());
            ex.printStackTrace(System.out);
        }

        if (verbosityLevel == SILENT) {
            return;
        }

        logMissingClass(getMissingClassName(ex));
    }

    static final protected boolean isValidMissingClassMessage(String message) {
        if (message == null) {
            return false;
        }

        message = message.trim();

        if (message.startsWith("[")) {
            // Sometimes we see methods called on array classes.
            // Obviously, these don't exist as class files.
            // So, we should just ignore the exception.
            // Really, we should fix the class/method search interfaces
            // to be much more intelligent in resolving method
            // implementations.
            return false;
        }

        if ("".equals(message)) {
            // Subtypes2 throws ClassNotFoundExceptions with no message in
            // some cases. Ignore them (the missing classes will already
            // have been reported).
            return false;
        }

        if (message.endsWith(".package-info")) {
            // we ignore all "package-info" issues
            return false;
        }
        if ("java.lang.Synthetic".equals(message)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd
     * .cs.findbugs.classfile.ClassDescriptor)
     */
    @Override
    public void reportMissingClass(ClassDescriptor classDescriptor) {
        if (DEBUG_MISSING_CLASSES) {
            System.out.println("Missing class: " + classDescriptor);
            new Throwable().printStackTrace(System.out);
        }

        if (verbosityLevel == SILENT) {
            return;
        }

        logMissingClass(classDescriptor.toDottedClassName());
    }

    /**
     * @param message
     */
    private void logMissingClass(String message) {
        if (!isValidMissingClassMessage(message)) {
            return;
        }
        missingClassMessageList.add(message);
    }

    /**
     * Report that we skipped some analysis of a method
     *
     * @param method
     */
    @Override
    public void reportSkippedAnalysis(MethodDescriptor method) {
        // TODO: log this
    }

    @Override
    public void logError(String message) {
        if (verbosityLevel == SILENT) {
            return;
        }

        Error error = new Error(errorCount++, message);
        if (!errorSet.contains(error)) {
            errorSet.add(error);
        }
    }

    /**
     * @return the set with all analysis errors reported so far
     */
    protected Set<Error> getQueuedErrors() {
        return errorSet;
    }

    /**
     * @return the set with all missing classes reported so far
     */
    protected Set<String> getMissingClasses() {
        return missingClassMessageList;
    }

    @Override
    public void logError(String message, Throwable e) {

        if (e instanceof MethodUnprofitableException) {
            // TODO: log this
            return;
        }
        if (e instanceof edu.umd.cs.findbugs.classfile.MissingClassException) {
            edu.umd.cs.findbugs.classfile.MissingClassException e2 = (edu.umd.cs.findbugs.classfile.MissingClassException) e;
            reportMissingClass(e2.getClassDescriptor());
            return;
        }
        if (e instanceof edu.umd.cs.findbugs.ba.MissingClassException) {
            // Record the missing class, in case the exception thrower didn't.
            edu.umd.cs.findbugs.ba.MissingClassException missingClassEx = (edu.umd.cs.findbugs.ba.MissingClassException) e;
            ClassNotFoundException cnfe = missingClassEx.getClassNotFoundException();

            reportMissingClass(cnfe);
            // Don't report dataflow analysis exceptions due to missing classes.
            // Too much noise.
            return;
        }

        if (verbosityLevel == SILENT) {
            return;
        }

        Error error = new Error(errorCount++, message, e);
        if (!errorSet.contains(error)) {
            errorSet.add(error);
        }
    }

    @Override
    public void reportQueuedErrors() {
        // Report unique errors in order of their sequence
        Error[] errorList = errorSet.toArray(new Error[errorSet.size()]);
        Arrays.sort(errorList, new Comparator<Error>() {
            @Override
            public int compare(Error o1, Error o2) {
                return o1.getSequence() - o2.getSequence();
            }
        });
        for (Error error : errorList) {
            reportAnalysisError(new AnalysisError(error.getMessage(), error.getCause()));
        }

        for (String aMissingClassMessageList : missingClassMessageList) {
            reportMissingClass(aMissingClassMessageList);
        }
    }

    @Override
    public void addObserver(BugReporterObserver observer) {
        observerList.add(observer);
    }

    @Override
    public ProjectStats getProjectStats() {
        return projectStats;
    }

    /**
     * This should be called when a bug is reported by a subclass.
     *
     * @param bugInstance
     *            the bug to inform observers of
     */
    protected void notifyObservers(BugInstance bugInstance) {
        for (BugReporterObserver aObserverList : observerList) {
            aObserverList.reportBug(bugInstance);
        }
    }

    /**
     * Subclasses must override this. It will be called only for bugs which meet
     * the priority threshold.
     *
     * @param bugInstance
     *            the bug to report
     */
    protected abstract void doReportBug(BugInstance bugInstance);

    /**
     * Report a queued error.
     *
     * @param error
     *            the queued error
     */
    public abstract void reportAnalysisError(AnalysisError error);

    /**
     * Report a missing class.
     *
     * @param string
     *            the name of the class
     */
    public abstract void reportMissingClass(String string);
}

