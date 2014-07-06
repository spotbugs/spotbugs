/*
 * FindBugs - Find Bugs in Java programs
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.util.Util;

/**
 * Helper class to parse the command line and configure the IFindBugsEngine
 * object. As a side-effect it also configures a DetectorFactoryCollection (to
 * enable and disable detectors as requested).
 */
public class TextUICommandLine extends FindBugsCommandLine {
    /**
     * Handling callback for choose() method, used to implement the
     * -chooseVisitors and -choosePlugins options.
     */
    private interface Chooser {
        /**
         * Choose a detector, plugin, etc.
         *
         * @param enable
         *            whether or not the item should be enabled
         * @param what
         *            the item
         */
        public void choose(boolean enable, String what);
    }

    private static final boolean DEBUG = Boolean.getBoolean("textui.debug");

    private static final int PRINTING_REPORTER = 0;

    private static final int SORTING_REPORTER = 1;

    private static final int XML_REPORTER = 2;

    private static final int EMACS_REPORTER = 3;

    private static final int HTML_REPORTER = 4;

    private static final int XDOCS_REPORTER = 5;

    private int bugReporterType = PRINTING_REPORTER;

    private boolean relaxedReportingMode = false;

    private boolean useLongBugCodes = false;

    private boolean showProgress = false;

    private boolean xmlMinimal = false;

    private boolean xmlWithMessages = false;

    private boolean xmlWithAbridgedMessages = false;

    private String stylesheet = null;

    private boolean quiet = false;

    private final ClassScreener classScreener = new ClassScreener();

    private final Set<String> enabledBugReporterDecorators = new LinkedHashSet<String>();

    private final Set<String> disabledBugReporterDecorators = new LinkedHashSet<String>();

    private boolean setExitCode = false;

    private boolean noClassOk = false;

    private int priorityThreshold = Detector.NORMAL_PRIORITY;

    private int rankThreshold = SystemProperties.getInt("findbugs.maxRank", BugRanker.VISIBLE_RANK_MAX);

    private PrintStream outputStream = null;

    private Set<String> bugCategorySet = null;

    private String trainingOutputDir;

    private String trainingInputDir;

    private String releaseName = "";

    private String projectName = "";

    private String sourceInfoFile = null;

    private String redoAnalysisFile = null;

    private boolean mergeSimilarWarnings = true;

    private boolean xargs = false;

    private boolean scanNestedArchives = true;

    private boolean applySuppression;

    private boolean printConfiguration;

    private boolean printVersion;

    /**
     * Constructor.
     */
    public TextUICommandLine() {
        addSwitch("-showPlugins", "show list of available detector plugins");

        addOption("-userPrefs", "filename", "user preferences file, e.g /path/to/project/.settings/edu.umd.cs.findbugs.core.prefs for Eclipse projects");

        startOptionGroup("Output options:");
        addSwitch("-justListOptions", "throw an exception that lists the provided options");
        makeOptionUnlisted("-justListOptions");


        addSwitch("-timestampNow", "set timestamp of results to be current time");
        addSwitch("-quiet", "suppress error messages");
        addSwitch("-longBugCodes", "report long bug codes");
        addSwitch("-progress", "display progress in terminal window");
        addOption("-release", "release name", "set the release name of the analyzed application");
        addSwitch("-experimental", "report of any confidence level including experimental bug patterns");
        addSwitch("-low", "report warnings of any confidence level");
        addSwitch("-medium", "report only medium and high confidence warnings [default]");
        addSwitch("-high", "report only high confidence warnings");
        addOption("-maxRank", "rank", "only report issues with a bug rank at least as scary as that provided");
        addSwitch("-dontCombineWarnings", "Don't combine warnings that differ only in line number");

        addSwitch("-sortByClass", "sort warnings by class");
        addSwitchWithOptionalExtraPart("-xml", "withMessages", "XML output (optionally with messages)");
        addSwitch("-xdocs", "xdoc XML output to use with Apache Maven");
        addSwitchWithOptionalExtraPart("-html", "stylesheet", "Generate HTML output (default stylesheet is default.xsl)");
        addSwitch("-emacs", "Use emacs reporting format");
        addSwitch("-relaxed", "Relaxed reporting mode (more false positives!)");
        addSwitchWithOptionalExtraPart("-train", "outputDir", "Save training data (experimental); output dir defaults to '.'");
        addSwitchWithOptionalExtraPart("-useTraining", "inputDir", "Use training data (experimental); input dir defaults to '.'");
        addOption("-redoAnalysis", "filename", "Redo analysis using configureation from previous analysis");
        addOption("-sourceInfo", "filename", "Specify source info file (line numbers for fields/classes)");
        addOption("-projectName", "project name", "Descriptive name of project");

        addOption("-reanalyze", "filename", "redo analysis in provided file");

        addOption("-outputFile", "filename", "Save output in named file");
        addOption("-output", "filename", "Save output in named file");
        makeOptionUnlisted("-outputFile");
        addSwitchWithOptionalExtraPart("-nested", "true|false", "analyze nested jar/zip archives (default=true)");

        startOptionGroup("Output filtering options:");
        addOption("-bugCategories", "cat1[,cat2...]", "only report bugs in given categories");
        addOption("-onlyAnalyze", "classes/packages",
                "only analyze given classes and packages; end with .* to indicate classes in a package, .- to indicate a package prefix");
        addOption("-excludeBugs", "baseline bugs", "exclude bugs that are also reported in the baseline xml output");
        addOption("-exclude", "filter file", "exclude bugs matching given filter");
        addOption("-include", "filter file", "include only bugs matching given filter");
        addSwitch("-applySuppression", "Exclude any bugs that match suppression filter loaded from fbp file");

        startOptionGroup("Detector (visitor) configuration options:");
        addOption("-visitors", "v1[,v2...]", "run only named visitors");
        addOption("-omitVisitors", "v1[,v2...]", "omit named visitors");
        addOption("-chooseVisitors", "+v1,-v2,...", "selectively enable/disable detectors");
        addOption("-choosePlugins", "+p1,-p2,...", "selectively enable/disable plugins");
        addOption("-adjustPriority", "v1=(raise|lower)[,...]", "raise/lower priority of warnings for given visitor(s)");

        startOptionGroup("Project configuration options:");
        addOption("-auxclasspath", "classpath", "set aux classpath for analysis");
        addSwitch("-auxclasspathFromInput", "read aux classpath from standard input");
        addOption("-auxclasspathFromFile", "filepath", "read aux classpaths from a designated file");
        addOption("-sourcepath", "source path", "set source path for analyzed classes");
        addSwitch("-exitcode", "set exit code of process");
        addSwitch("-noClassOk", "output empty warning file if no classes are specified");
        addSwitch("-xargs", "get list of classfiles/jarfiles from standard input rather than command line");
        addOption("-analyzeFromFile", "filepath", "get the list of class/jar files from a designated file");
        addOption("-cloud", "id", "set cloud id");
        addOption("-cloudProperty", "key=value", "set cloud property");
        addOption("-bugReporters", "name,name2,-name3", "bug reporter decorators to explicitly enable/disable");

        addSwitch("-printConfiguration", "print configuration and exit, without running analysis");
        addSwitch("-version", "print version, check for updates and exit, without running analysis");
    }

    @Override
    public @Nonnull Project getProject() {
        return project;
    }

    public boolean getXargs() {
        return xargs;
    }

    public boolean setExitCode() {
        return setExitCode;
    }

    public boolean noClassOk() {
        return noClassOk;
    }

    public boolean quiet() {
        return quiet;
    }

    public boolean applySuppression() {
        return applySuppression;
    }

    public boolean justPrintConfiguration() {
        return printConfiguration;
    }

    public boolean justPrintVersion() {
        return printVersion;
    }

    Map<String, String> parsedOptions = new LinkedHashMap<String, String>();

    @SuppressFBWarnings("DM_EXIT")
    @Override
    protected void handleOption(String option, String optionExtraPart) {
        parsedOptions.put(option, optionExtraPart);
        if (DEBUG) {
            if (optionExtraPart != null) {
                System.out.println("option " + option + ":" + optionExtraPart);
            } else {
                System.out.println("option " + option);
            }
        }
        if (option.equals("-showPlugins")) {
            System.out.println("Available plugins:");
            int count = 0;
            for (Iterator<Plugin> i = DetectorFactoryCollection.instance().pluginIterator(); i.hasNext();) {
                Plugin plugin = i.next();
                System.out.println("  " + plugin.getPluginId() + " (default: " + (plugin.isEnabledByDefault() ? "enabled" : "disabled")
                        + ")");
                if (plugin.getShortDescription() != null) {
                    System.out.println("    Description: " + plugin.getShortDescription());
                }
                if (plugin.getProvider() != null) {
                    System.out.println("    Provider: " + plugin.getProvider());
                }
                if (plugin.getWebsite() != null) {
                    System.out.println("    Website: " + plugin.getWebsite());
                }
                ++count;
            }
            if (count == 0) {
                System.out.println("  No plugins are available (FindBugs installed incorrectly?)");
            }
            System.exit(0);
        } else if (option.equals("-experimental")) {
            priorityThreshold = Priorities.EXP_PRIORITY;
        } else if (option.equals("-longBugCodes")) {
            useLongBugCodes = true;
        } else if (option.equals("-progress")) {
            showProgress = true;
        } else if (option.equals("-timestampNow")) {
            project.setTimestamp(System.currentTimeMillis());
        } else if (option.equals("-low")) {
            priorityThreshold = Priorities.LOW_PRIORITY;
        } else if (option.equals("-medium")) {
            priorityThreshold = Priorities.NORMAL_PRIORITY;
        } else if (option.equals("-high")) {
            priorityThreshold = Priorities.HIGH_PRIORITY;
        } else if (option.equals("-dontCombineWarnings")) {
            mergeSimilarWarnings = false;
        } else if (option.equals("-sortByClass")) {
            bugReporterType = SORTING_REPORTER;
        } else if (option.equals("-xml")) {
            bugReporterType = XML_REPORTER;
            if (!optionExtraPart.equals("")) {
                if (optionExtraPart.equals("withMessages")) {
                    xmlWithMessages = true;
                } else if (optionExtraPart.equals("withAbridgedMessages")) {
                    xmlWithMessages = true;
                    xmlWithAbridgedMessages = true;
                } else if (optionExtraPart.equals("minimal")) {
                    xmlWithMessages = false;
                    xmlMinimal = true;
                } else {
                    throw new IllegalArgumentException("Unknown option: -xml:" + optionExtraPart);
                }
            }
        } else if (option.equals("-emacs")) {
            bugReporterType = EMACS_REPORTER;
        } else if (option.equals("-relaxed")) {
            relaxedReportingMode = true;
        } else if (option.equals("-train")) {
            trainingOutputDir = !optionExtraPart.equals("") ? optionExtraPart : ".";
        } else if (option.equals("-useTraining")) {
            trainingInputDir = !optionExtraPart.equals("") ? optionExtraPart : ".";
        } else if (option.equals("-html")) {
            bugReporterType = HTML_REPORTER;
            if (!optionExtraPart.equals("")) {
                stylesheet = optionExtraPart;
            } else {
                stylesheet = "default.xsl";
            }
        } else if (option.equals("-xdocs")) {
            bugReporterType = XDOCS_REPORTER;
        } else if (option.equals("-applySuppression")) {
            applySuppression = true;
        } else if (option.equals("-quiet")) {
            quiet = true;
        } else if (option.equals("-nested")) {
            scanNestedArchives = optionExtraPart.equals("") || Boolean.valueOf(optionExtraPart).booleanValue();
        } else if (option.equals("-exitcode")) {
            setExitCode = true;
        } else if (option.equals("-auxclasspathFromInput")) {
            try {
                BufferedReader in = UTF8.bufferedReader(System.in);
                while (true) {
                    String s = in.readLine();
                    if (s == null) {
                        break;
                    }
                    addAuxClassPathEntries(s);
                }
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (option.equals("-noClassOk")) {
            noClassOk = true;
        } else if (option.equals("-xargs")) {
            xargs = true;
        } else if (option.equals("-justListOptions")) {
            throw new RuntimeException("textui options are: " + parsedOptions);
        } else if (option.equals("-printConfiguration")) {
            printConfiguration = true;
        } else if (option.equals("-version")) {
            printVersion = true;
        } else {
            if(DEBUG) {
                System.out.println("XXX: " + option);
            }
            super.handleOption(option, optionExtraPart);
        }
    }

    protected @CheckForNull File outputFile;
    @SuppressFBWarnings("DM_EXIT")
    @Override
    protected void handleOptionWithArgument(String option, String argument) throws IOException {
        parsedOptions.put(option, argument);

        if (DEBUG) {
            System.out.println("option " + option + " is " + argument);
        }
        if (option.equals("-outputFile") || option.equals("-output")) {
            if (outputFile != null) {
                throw new IllegalArgumentException("output set twice; to " + outputFile + " and to " + argument);
            }
            outputFile = new File(argument);

            String fileName = outputFile.getName();
            String extension = Util.getFileExtensionIgnoringGz(outputFile);
            if (bugReporterType == PRINTING_REPORTER && (extension.equals("xml") || extension.equals("fba"))) {
                bugReporterType = XML_REPORTER;
            }

            try {
                OutputStream oStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                if (fileName.endsWith(".gz")) {
                    oStream = new GZIPOutputStream(oStream);
                }
                outputStream = UTF8.printStream(oStream);
            } catch (IOException e) {
                System.err.println("Couldn't open " + outputFile + " for output: " + e.toString());
                System.exit(1);
            }
        } else if (option.equals("-cloud")) {
            project.setCloudId(argument);
        } else if (option.equals("-cloudProperty")) {
            int e = argument.indexOf('=');
            if (e == -1) {
                throw new IllegalArgumentException("Bad cloud property: " + argument);
            }
            String key = argument.substring(0, e);
            String value = argument.substring(e + 1);
            project.getCloudProperties().setProperty(key, value);

        } else if (option.equals("-bugReporters")) {
            for (String s : argument.split(",")) {
                if (s.charAt(0) == '-') {
                    disabledBugReporterDecorators.add(s.substring(1));
                } else if (s.charAt(0) == '+') {
                    enabledBugReporterDecorators.add(s.substring(1));
                } else {
                    enabledBugReporterDecorators.add(s);
                }
            }

        } else if (option.equals("-maxRank")) {
            this.rankThreshold = Integer.parseInt(argument);
        } else if (option.equals("-projectName")) {
            this.projectName = argument;
        } else if (option.equals("-release")) {
            this.releaseName = argument;
        } else if (option.equals("-redoAnalysis")) {
            redoAnalysisFile = argument;
        } else if (option.equals("-sourceInfo")) {
            sourceInfoFile = argument;
        } else if (option.equals("-visitors") || option.equals("-omitVisitors")) {
            boolean omit = option.equals("-omitVisitors");

            if (!omit) {
                // Selecting detectors explicitly, so start out by
                // disabling all of them. The selected ones will
                // be re-enabled.
                getUserPreferences().enableAllDetectors(false);
            }

            // Explicitly enable or disable the selected detectors.
            StringTokenizer tok = new StringTokenizer(argument, ",");
            while (tok.hasMoreTokens()) {
                String visitorName = tok.nextToken().trim();
                DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(visitorName);
                if (factory == null) {
                    throw new IllegalArgumentException("Unknown detector: " + visitorName);
                }
                getUserPreferences().enableDetector(factory, !omit);
            }
        } else if (option.equals("-chooseVisitors")) {
            // This is like -visitors and -omitVisitors, but
            // you can selectively enable and disable detectors,
            // starting from the default set (or whatever set
            // happens to be in effect).
            choose(argument, "Detector choices", new Chooser() {
                @Override
                public void choose(boolean enabled, String what) {
                    DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(what);
                    if (factory == null) {
                        throw new IllegalArgumentException("Unknown detector: " + what);
                    }
                    if (FindBugs.DEBUG) {
                        System.err.println("Detector " + factory.getShortName() + " " + (enabled ? "enabled" : "disabled")
                                + ", userPreferences=" + System.identityHashCode(getUserPreferences()));
                    }
                    getUserPreferences().enableDetector(factory, enabled);
                }
            });
        } else if (option.equals("-choosePlugins")) {
            // Selectively enable/disable plugins
            choose(argument, "Plugin choices", new Chooser() {
                @Override
                public void choose(boolean enabled, String what) {
                    Plugin plugin = DetectorFactoryCollection.instance().getPluginById(what);
                    if (plugin == null) {
                        throw new IllegalArgumentException("Unknown plugin: " + what);
                    }
                    plugin.setGloballyEnabled(enabled);
                }
            });
        } else if (option.equals("-adjustPriority")) {
            // Selectively raise or lower the priority of warnings
            // produced by specified detectors.

            StringTokenizer tok = new StringTokenizer(argument, ",");
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken();
                int eq = token.indexOf('=');
                if (eq < 0) {
                    throw new IllegalArgumentException("Illegal priority adjustment: " + token);
                }

                String adjustmentTarget = token.substring(0, eq);
                String adjustment = token.substring(eq + 1);

                int adjustmentAmount;
                if (adjustment.equals("raise")) {
                    adjustmentAmount = -1;
                } else if (adjustment.equals("lower")) {
                    adjustmentAmount = +1;
                } else if (adjustment.equals("suppress")) {
                    adjustmentAmount = +100;
                } else {
                    throw new IllegalArgumentException("Illegal priority adjustment value: " + adjustment);
                }

                DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(adjustmentTarget);
                if (factory != null) {
                    factory.setPriorityAdjustment(adjustmentAmount);
                } else {
                    //
                    DetectorFactoryCollection i18n = DetectorFactoryCollection.instance();
                    BugPattern pattern = i18n.lookupBugPattern(adjustmentTarget);
                    if (pattern == null) {
                        throw new IllegalArgumentException("Unknown detector: " + adjustmentTarget);
                    }
                    pattern.adjustPriority(adjustmentAmount);
                }

            }
        } else if (option.equals("-bugCategories")) {
            this.bugCategorySet = FindBugs.handleBugCategories(argument);
        } else if (option.equals("-onlyAnalyze")) {
            // The argument is a comma-separated list of classes and packages
            // to select to analyze. (If a list item ends with ".*",
            // it specifies a package, otherwise it's a class.)
            StringTokenizer tok = new StringTokenizer(argument, ",");
            while (tok.hasMoreTokens()) {
                String item = tok.nextToken();
                if (item.endsWith(".-")) {
                    classScreener.addAllowedPrefix(item.substring(0, item.length() - 1));
                } else if (item.endsWith(".*")) {
                    classScreener.addAllowedPackage(item.substring(0, item.length() - 1));
                } else {
                    classScreener.addAllowedClass(item);
                }
            }
        } else if (option.equals("-exclude")) {
            project.getConfiguration().getExcludeFilterFiles().put(argument, true);
        } else if (option.equals("-excludeBugs")) {
            project.getConfiguration().getExcludeBugsFiles().put(argument, true);
        } else if (option.equals("-include")) {
            project.getConfiguration().getIncludeFilterFiles().put(argument, true);
        } else if (option.equals("-auxclasspathFromFile")) {
            handleAuxClassPathFromFile(argument);
        } else if (option.equals("-analyzeFromFile")) {
            handleAnalyzeFromFile(argument);
        } else if (option.equals("-auxclasspath")) {
            addAuxClassPathEntries(argument);
        } else if (option.equals("-sourcepath")) {
            StringTokenizer tok = new StringTokenizer(argument, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                project.addSourceDir(new File(tok.nextToken()).getAbsolutePath());
            }
        } else if(option.equals("-userPrefs")){
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.read(new FileInputStream(argument));
            project.setConfiguration(prefs);
        } else {
            super.handleOptionWithArgument(option, argument);
        }
    }

    /**
     * Parse the argument as auxclasspath entries and add them
     *
     * @param argument
     */
    private void addAuxClassPathEntries(String argument) {
        StringTokenizer tok = new StringTokenizer(argument, File.pathSeparator);
        while (tok.hasMoreTokens()) {
            project.addAuxClasspathEntry(tok.nextToken());
        }
    }

    /**
     * Common handling code for -chooseVisitors and -choosePlugins options.
     *
     * @param argument
     *            the list of visitors or plugins to be chosen
     * @param desc
     *            String describing what is being chosen
     * @param chooser
     *            callback object to selectively choose list members
     */
    private void choose(String argument, String desc, Chooser chooser) {
        StringTokenizer tok = new StringTokenizer(argument, ",");
        while (tok.hasMoreTokens()) {
            String what = tok.nextToken().trim();
            if (!what.startsWith("+") && !what.startsWith("-")) {
                throw new IllegalArgumentException(desc + " must start with " + "\"+\" or \"-\" (saw " + what + ")");
            }
            boolean enabled = what.startsWith("+");
            chooser.choose(enabled, what.substring(1));
        }
    }

    public void configureEngine(IFindBugsEngine findBugs) throws IOException, FilterException {
        // Load plugins

        // Set the DetectorFactoryCollection (that has been configured
        // by command line parsing)
        findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());


        if (redoAnalysisFile != null) {
            SortedBugCollection bugs = new SortedBugCollection();
            try {
                bugs.readXML(redoAnalysisFile);
            } catch (DocumentException e) {
                IOException ioe = new IOException("Unable to parse " + redoAnalysisFile);
                ioe.initCause(e);
                throw ioe;
            }
            project = bugs.getProject().duplicate();
        }
        TextUIBugReporter textuiBugReporter;
        switch (bugReporterType) {
        case PRINTING_REPORTER:
            textuiBugReporter = new PrintingBugReporter();
            break;
        case SORTING_REPORTER:
            textuiBugReporter = new SortingBugReporter();
            break;
        case XML_REPORTER: {
            XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
            xmlBugReporter.setAddMessages(xmlWithMessages);
            xmlBugReporter.setMinimalXML(xmlMinimal);

            textuiBugReporter = xmlBugReporter;
        }
        break;
        case EMACS_REPORTER:
            textuiBugReporter = new EmacsBugReporter();
            break;
        case HTML_REPORTER:
            textuiBugReporter = new HTMLBugReporter(project, stylesheet);
            break;
        case XDOCS_REPORTER:
            textuiBugReporter = new XDocsBugReporter(project);
            break;
        default:
            throw new IllegalStateException();
        }

        if (quiet) {
            textuiBugReporter.setErrorVerbosity(BugReporter.SILENT);
        }

        textuiBugReporter.setPriorityThreshold(priorityThreshold);
        textuiBugReporter.setRankThreshold(rankThreshold);
        textuiBugReporter.setUseLongBugCodes(useLongBugCodes);

        findBugs.setRankThreshold(rankThreshold);
        if (outputStream != null) {
            textuiBugReporter.setOutputStream(outputStream);
        }

        BugReporter bugReporter = textuiBugReporter;

        if (bugCategorySet != null) {
            bugReporter = new CategoryFilteringBugReporter(bugReporter, bugCategorySet);
        }

        findBugs.setBugReporter(bugReporter);
        findBugs.setProject(project);

        if (showProgress) {
            findBugs.setProgressCallback(new TextUIProgressCallback(System.out));
        }

        findBugs.setUserPreferences(getUserPreferences());
        findBugs.setClassScreener(classScreener);

        findBugs.setRelaxedReportingMode(relaxedReportingMode);
        findBugs.setAbridgedMessages(xmlWithAbridgedMessages);

        if (trainingOutputDir != null) {
            findBugs.enableTrainingOutput(trainingOutputDir);
        }
        if (trainingInputDir != null) {
            findBugs.enableTrainingInput(trainingInputDir);
        }

        if (sourceInfoFile != null) {
            findBugs.setSourceInfoFile(sourceInfoFile);
        }

        findBugs.setAnalysisFeatureSettings(settingList);

        findBugs.setMergeSimilarWarnings(mergeSimilarWarnings);
        findBugs.setReleaseName(releaseName);
        findBugs.setProjectName(projectName);

        findBugs.setScanNestedArchives(scanNestedArchives);
        findBugs.setNoClassOk(noClassOk);

        findBugs.setBugReporterDecorators(enabledBugReporterDecorators, disabledBugReporterDecorators);
        if (applySuppression) {
            findBugs.setApplySuppression(true);
        }

        findBugs.finishSettings();
    }

    /**
     * Handle -xargs command line option by reading jar file names from standard
     * input and adding them to the project.
     *
     * @throws IOException
     */
    public void handleXArgs() throws IOException {
        if (getXargs()) {
            BufferedReader in = UTF8.bufferedReader(System.in);
            try {
                while (true) {
                    String s = in.readLine();
                    if (s == null) {
                        break;
                    }
                    project.addFile(s);
                }
            } finally {
                Util.closeSilently(in);
            }
        }
    }

    /**
     * Handle -readAuxFromFile command line option by reading classpath entries
     * from a file and adding them to the project.
     *
     * @throws IOException
     */
    private void handleAuxClassPathFromFile(String filePath) throws IOException {
        BufferedReader in = new BufferedReader(UTF8.fileReader(filePath));
        try {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }
                project.addAuxClasspathEntry(s);
            }
        } finally {
            Util.closeSilently(in);
        }
    }

    /**
     * Handle -analyzeFromFile command line option by reading jar file names
     * from a file and adding them to the project.
     *
     * @throws IOException
     */
    private void handleAnalyzeFromFile(String filePath) throws IOException {
        BufferedReader in = new BufferedReader(UTF8.fileReader(filePath));
        try {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }
                project.addFile(s);
            }
        } finally {
            Util.closeSilently(in);
        }
    }

    /**
     * @return Returns the userPreferences.
     */
    private UserPreferences getUserPreferences() {
        return project.getConfiguration();
    }
}
