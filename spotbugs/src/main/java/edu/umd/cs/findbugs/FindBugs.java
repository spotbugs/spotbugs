/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.CommandLine.HelpRequestedException;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;

import static java.util.logging.Level.*;

/**
 * Static methods and fields useful for working with instances of
 * IFindBugsEngine.
 *
 * This class was previously the main driver for FindBugs analyses, but has been
 * replaced by {@link FindBugs2 FindBugs2}.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public abstract class FindBugs {
    /**
     * Analysis settings for -effort:min.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_ARRAY", justification = "Would have to break APIs to fix this properly")
    public static final AnalysisFeatureSetting[] MIN_EFFORT = new AnalysisFeatureSetting[] {
        new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, true),
        new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, false),
        new AnalysisFeatureSetting(AnalysisFeatures.MERGE_SIMILAR_WARNINGS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, false),
        new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, false),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, false),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, false),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false), };

    /**
     * Analysis settings for -effort:less.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_ARRAY", justification = "Would have to break APIs to fix this properly")
    public static final AnalysisFeatureSetting[] LESS_EFFORT = new AnalysisFeatureSetting[] {
        new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
        new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MERGE_SIMILAR_WARNINGS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
        new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, false),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, false),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, false),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false), };

    /**
     * Analysis settings for -effort:default.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_ARRAY", justification = "Would have to break APIs to fix this properly")
    public static final AnalysisFeatureSetting[] DEFAULT_EFFORT = new AnalysisFeatureSetting[] {
        new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
        new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MERGE_SIMILAR_WARNINGS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
        new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false), };

    /**
     * Analysis settings for -effort:more.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_ARRAY", justification = "Would have to break APIs to fix this properly")
    public static final AnalysisFeatureSetting[] MORE_EFFORT = new AnalysisFeatureSetting[] {
        new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
        new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MERGE_SIMILAR_WARNINGS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
        new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, false), };

    /**
     * Analysis settings for -effort:max.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_ARRAY", justification = "Would have to break APIs to fix this properly")
    public static final AnalysisFeatureSetting[] MAX_EFFORT = new AnalysisFeatureSetting[] {
        new AnalysisFeatureSetting(AnalysisFeatures.CONSERVE_SPACE, false),
        new AnalysisFeatureSetting(AnalysisFeatures.ACCURATE_EXCEPTIONS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MERGE_SIMILAR_WARNINGS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.MODEL_INSTANCEOF, true),
        new AnalysisFeatureSetting(AnalysisFeatures.SKIP_HUGE_METHODS, false),
        new AnalysisFeatureSetting(AnalysisFeatures.INTERATIVE_OPCODE_STACK_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS, true),
        new AnalysisFeatureSetting(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS_OF_REFERENCED_CLASSES, true), };

    /**
     * Debug tracing.
     */
    public static final boolean DEBUG = Boolean.getBoolean("findbugs.debug");

    /**
     * FindBugs home directory.
     */
    private static String home = System.getProperty("spotbugs.home", System.getProperty("findbugs.home"));

    private static boolean noAnalysis = Boolean.getBoolean("findbugs.noAnalysis");

    /**
     * Disable analysis within FindBugs. Turns off loading of bug detectors.
     */
    public static void setNoAnalysis() {
        noAnalysis = true;
    }

    /**
     * @return Returns the noAnalysis.
     */
    public static boolean isNoAnalysis() {
        return noAnalysis;
    }

    private static boolean noMains = Boolean.getBoolean("findbugs.noMains");

    /**
     * Disable loading of FindBugsMain classes.
     */
    public static void setNoMains() {
        noMains = true;
    }

    /**
     * @return Returns the noMains.
     */
    public static boolean isNoMains() {
        return noMains;
    }

    public static final Logger LOG = Logger.getLogger(FindBugs.class.getPackage().getName());

    static {
        LOG.setLevel(WARNING);
    }

    /**
     * Known URL protocols. Filename URLs that do not have an explicit protocol
     * are assumed to be files.
     */
    @StaticConstant
    public static final Set<String> knownURLProtocolSet;
    static {
        Set<String> protocols = new HashSet<>();
        protocols.add("file");
        protocols.add("http");
        protocols.add("https");
        protocols.add("jar");
        knownURLProtocolSet = Collections.unmodifiableSet(protocols);
    }

    /**
     * Set the FindBugs home directory.
     */
    public static void setHome(String home) {
        FindBugs.home = home;
    }

    /**
     * Get the FindBugs home directory.
     */
    public static String getHome() {
        return home;
    }

    /**
     * Configure training databases.
     *
     * @param findBugs
     *            the IFindBugsEngine to configure
     * @throws IOException
     */
    public static void configureTrainingDatabases(IFindBugsEngine findBugs) throws IOException {
        if (findBugs.emitTrainingOutput()) {
            String trainingOutputDir = findBugs.getTrainingOutputDir();

            if (!new File(trainingOutputDir).isDirectory()) {
                throw new IOException("Training output directory " + trainingOutputDir + " does not exist");
            }
            AnalysisContext.currentAnalysisContext().setDatabaseOutputDir(trainingOutputDir);
            // XXX: hack
            System.setProperty("findbugs.checkreturn.savetraining", new File(trainingOutputDir, "checkReturn.db").getPath());
        }
        if (findBugs.useTrainingInput()) {
            String trainingInputDir = findBugs.getTrainingInputDir();

            if (!new File(trainingInputDir).isDirectory()) {
                throw new IOException("Training input directory " + trainingInputDir + " does not exist");
            }
            AnalysisContext.currentAnalysisContext().setDatabaseInputDir(trainingInputDir);
            AnalysisContext.currentAnalysisContext().loadInterproceduralDatabases();
            // XXX: hack
            System.setProperty("findbugs.checkreturn.loadtraining", new File(trainingInputDir, "checkReturn.db").getPath());
        } else {
            AnalysisContext.currentAnalysisContext().loadDefaultInterproceduralDatabases();
        }
    }

    /**
     * Determines whether or not given DetectorFactory should be enabled.
     *
     * @param findBugs
     *            the IFindBugsEngine
     * @param factory
     *            the DetectorFactory
     * @param rankThreshold
     *            TODO
     * @return true if the DetectorFactory should be enabled, false otherwise
     */
    public static boolean isDetectorEnabled(IFindBugsEngine findBugs, DetectorFactory factory, int rankThreshold) {

        if (!findBugs.getUserPreferences().isDetectorEnabled(factory)) {
            return false;
        }

        if (!factory.isEnabledForCurrentJRE()) {
            return false;
        }

        // Slow first pass detectors are usually disabled, but may be explicitly
        // enabled
        if (!AnalysisContext.currentAnalysisContext().getBoolProperty(FindBugsAnalysisFeatures.INTERPROCEDURAL_ANALYSIS)
                && factory.isDetectorClassSubtypeOf(InterproceduralFirstPassDetector.class)) {
            return false;
        }

        int maxRank = Integer.MAX_VALUE;
        Set<BugPattern> reportedBugPatterns = factory.getReportedBugPatterns();
        boolean isNonReportingDetector = factory.isDetectorClassSubtypeOf(FirstPassDetector.class);
        if (!isNonReportingDetector && !reportedBugPatterns.isEmpty()) {
            for (BugPattern b : reportedBugPatterns) {
                int rank = BugRanker.findRank(b, factory);
                if (maxRank > rank) {
                    maxRank = rank;
                }
            }
            if (maxRank > rankThreshold) {
                return false;
            }
        }

        // Training detectors are enabled if, and only if, we are emitting
        // training output
        boolean isTrainingDetector = factory.isDetectorClassSubtypeOf(TrainingDetector.class);
        if (findBugs.emitTrainingOutput()) {
            return isTrainingDetector || isNonReportingDetector;
        }

        return !isTrainingDetector;
    }

    /**
     * Process -bugCategories option.
     *
     * @param categories
     *            comma-separated list of bug categories
     *
     * @return Set of categories to be used
     */
    public static Set<String> handleBugCategories(String categories) {
        // Parse list of bug categories
        Set<String> categorySet = new HashSet<>();
        StringTokenizer tok = new StringTokenizer(categories, ",");
        while (tok.hasMoreTokens()) {
            categorySet.add(tok.nextToken());
        }

        return categorySet;
    }

    /**
     * Process the command line.
     *
     * @param commandLine
     *            the TextUICommandLine object which will parse the command line
     * @param argv
     *            the command line arguments
     * @param findBugs
     *            the IFindBugsEngine to configure
     * @throws IOException
     * @throws FilterException
     */
    public static void processCommandLine(TextUICommandLine commandLine, String[] argv, IFindBugsEngine findBugs)
            throws IOException, FilterException {
        // Expand option files in command line.
        // An argument beginning with "@" is treated as specifying
        // the name of an option file.
        // Each line of option files are treated as a single argument.
        // Blank lines and comment lines (beginning with "#")
        // are ignored.
        try {
            argv = commandLine.expandOptionFiles(argv, true, true);
        } catch (HelpRequestedException e) {
            showHelp(commandLine);
        }

        int argCount = 0;
        try {
            argCount = commandLine.parse(argv);
        } catch (IllegalArgumentException e) {
            LOG.severe(e.getMessage());
            showHelp(commandLine);
        } catch (HelpRequestedException e) {
            showHelp(commandLine);
        }

        Project project = commandLine.getProject();
        for (int i = argCount; i < argv.length; ++i) {
            project.addFile(argv[i]);
        }
        commandLine.handleXArgs();

        commandLine.configureEngine(findBugs);
        if (commandLine.getProject().getFileCount() == 0 &&
                !commandLine.justPrintConfiguration() && !commandLine.justPrintVersion()) {
            LOG.warning("No files to be analyzed");

            showHelp(commandLine);
        }
    }

    /**
     * Show -help message.
     *
     * @param commandLine
     */
    @SuppressFBWarnings("DM_EXIT")
    public static void showHelp(TextUICommandLine commandLine) {
        showSynopsis();
        ShowHelp.showGeneralOptions();
        FindBugs.showCommandLineOptions(commandLine);
        System.exit(1);
    }

    /**
     * Given a fully-configured IFindBugsEngine and the TextUICommandLine used
     * to configure it, execute the analysis.
     *
     * @param findBugs
     *            a fully-configured IFindBugsEngine
     * @param commandLine
     *            the TextUICommandLine used to configure the IFindBugsEngine
     */
    @SuppressFBWarnings("DM_EXIT")
    public static void runMain(IFindBugsEngine findBugs, TextUICommandLine commandLine) throws IOException {
        boolean verbose = !commandLine.quiet();

        try {
            findBugs.execute();
        } catch (InterruptedException e) {
            assert false; // should not occur
            checkExitCodeFail(commandLine, e);
            throw new RuntimeException(e);
        } catch (RuntimeException | IOException e) {
            checkExitCodeFail(commandLine, e);
            throw e;
        }

        int bugCount = findBugs.getBugCount();
        int missingClassCount = findBugs.getMissingClassCount();
        int errorCount = findBugs.getErrorCount();

        if (verbose || commandLine.setExitCode()) {
            LOG.log(FINE, "Warnings generated: {0}", bugCount);
            LOG.log(FINEST, "Missing classes: {0}", missingClassCount);
            LOG.log(FINE, "Analysis errors: {0}", errorCount);
        }

        if (commandLine.setExitCode()) {
            int exitCode = 0;
            LOG.finest("Calculating exit code...");
            if (errorCount > 0) {
                exitCode |= ExitCodes.ERROR_FLAG;
                LOG.log(FINE, "Setting 'errors encountered' flag ({0})", ExitCodes.ERROR_FLAG);
            }
            if (missingClassCount > 0) {
                exitCode |= ExitCodes.MISSING_CLASS_FLAG;
                LOG.log(FINEST, "Setting 'missing class' flag ({0})", ExitCodes.MISSING_CLASS_FLAG);
            }
            if (bugCount > 0) {
                exitCode |= ExitCodes.BUGS_FOUND_FLAG;
                LOG.log(FINEST, "Setting 'bugs found' flag ({0})", ExitCodes.BUGS_FOUND_FLAG);
            }
            LOG.log(FINEST, "Exit code set to: {0}", exitCode);

            System.exit(exitCode);
        }
    }

    /**
     * @param commandLine
     * @param e
     */
    private static void checkExitCodeFail(TextUICommandLine commandLine, Exception e) {
        if (commandLine.setExitCode()) {
            e.printStackTrace(System.err);
            System.exit(ExitCodes.ERROR_FLAG);
        }
    }

    /**
     * Print command line options synopses to stdout.
     */
    public static void showCommandLineOptions() {
        showCommandLineOptions(new TextUICommandLine());
    }

    /**
     * Print command line options synopses to stdout.
     *
     * @param commandLine
     *            the TextUICommandLine whose options should be printed
     */
    public static void showCommandLineOptions(TextUICommandLine commandLine) {
        commandLine.printUsage(System.out);
    }

    /**
     * Show the overall FindBugs command synopsis.
     */
    public static void showSynopsis() {
        LOG.warning("Usage: findbugs [general options] -textui [command line options...] [jar/zip/class files, directories...]");
    }

    /**
     * Configure the (bug instance) Filter for the given DelegatingBugReporter.
     *
     * @param bugReporter
     *            a DelegatingBugReporter
     * @param filterFileName
     *            filter file name
     * @param include
     *            true if the filter is an include filter, false if it's an
     *            exclude filter
     * @throws java.io.IOException
     * @throws edu.umd.cs.findbugs.filter.FilterException
     */
    public static BugReporter configureFilter(BugReporter bugReporter, String filterFileName, boolean include)
            throws IOException, FilterException {
        Filter filter = new Filter(filterFileName);
        return new FilterBugReporter(bugReporter, filter, include);

    }

    /**
     * Configure a baseline bug instance filter.
     *
     * @param bugReporter
     *            a DelegatingBugReporter
     * @param baselineFileName
     *            filename of baseline Filter
     * @throws java.io.IOException
     * @throws org.dom4j.DocumentException
     */
    public static BugReporter configureBaselineFilter(BugReporter bugReporter, String baselineFileName) throws IOException,
            DocumentException {
        return new ExcludingHashesBugReporter(bugReporter, baselineFileName);
    }

    /**
     * Configure the BugCollection (if the BugReporter being used is
     * constructing one).
     *
     * @param findBugs
     *            the IFindBugsEngine
     */
    public static void configureBugCollection(IFindBugsEngine findBugs) {
        BugCollection bugs = findBugs.getBugReporter().getBugCollection();
        if (bugs == null) {
            return;
        }

        bugs.setReleaseName(findBugs.getReleaseName());

        Project project = findBugs.getProject();

        String projectName = project.getProjectName();

        if (projectName == null) {
            projectName = findBugs.getProjectName();
            project.setProjectName(projectName);
        }

        long timestamp = project.getTimestamp();
        if (FindBugs.validTimestamp(timestamp)) {
            bugs.setTimestamp(timestamp);
            bugs.getProjectStats().setTimestamp(timestamp);
        }
    }

    /** Date of release of Java 1.0 */
    public final static long MINIMUM_TIMESTAMP = new GregorianCalendar(1996, 0, 23).getTime().getTime();

    public static boolean validTimestamp(long timestamp) {
        return timestamp > MINIMUM_TIMESTAMP;
    }
}
