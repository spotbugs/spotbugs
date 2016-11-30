/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.util.Bag;

/**
 * A simple BugReporter which simply prints the formatted message to the output
 * stream.
 */
public class PrintingBugReporter extends TextUIBugReporter {
    private final HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

    @Override
    public void observeClass(ClassDescriptor classDescriptor) {
        // Don't need to do anything special, since we won't be
        // reporting statistics.
    }

    @Override
    protected void doReportBug(BugInstance bugInstance) {
        if (seenAlready.add(bugInstance)) {
            printBug(bugInstance);
            notifyObservers(bugInstance);
        }
    }

    @Override
    public void finish() {
        outputStream.close();
    }

    class PrintingCommandLine extends CommandLine {
        private String stylesheet = null;

        private boolean annotationUploadFormat = false;

        private int maxRank = BugRanker.VISIBLE_RANK_MAX;

        private int summarizeMaxRank = maxRank;

        private final Project project;

        private boolean setExitCode;

        public PrintingCommandLine() {
            project = new Project();
            addSwitch("-longBugCodes", "use long bug codes when generating text");
            addSwitch("-rank", "list rank when generating text");
            addOption("-maxRank", "max rank", "only list bugs of this rank or less");
            addOption("-summarizeMaxRank", "max rank", "summary bugs with of this rank or less");
            addSwitch("-designations", "report user designations for each bug");
            addSwitch("-history", "report first and last versions for each bug");
            addSwitch("-applySuppression", "exclude any bugs that match suppression filters");
            addSwitch("-annotationUpload", "generate annotations in upload format");
            addSwitchWithOptionalExtraPart("-html", "stylesheet", "Generate HTML output (default stylesheet is default.xsl)");
            addOption("-pluginList", "jar1[" + File.pathSeparator + "jar2...]", "specify list of plugin Jar files to load");
            addSwitch("-exitcode", "set exit code of process");
        }

        public @Nonnull
        Project getProject() {
            return project;
        }

        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            if ("-longBugCodes".equals(option)) {
                setUseLongBugCodes(true);
            } else if ("-rank".equals(option)) {
                setShowRank(true);
            } else if ("-designations".equals(option)) {
                setReportUserDesignations(true);
            } else if ("-applySuppression".equals(option)) {
                setApplySuppressions(true);
            } else if ("-history".equals(option)) {
                setReportHistory(true);
            } else if ("-annotationUpload".equals(option)) {
                annotationUploadFormat = true;
            } else if ("-html".equals(option)) {
                if (!"".equals(optionExtraPart)) {
                    stylesheet = optionExtraPart;
                } else {
                    stylesheet = "default.xsl";
                }
            } else if ("-exitcode".equals(option)) {
                setExitCode = true;
            } else {
                throw new IllegalArgumentException("Unknown option '" + option + "'");
            }
        }

        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
            if ("-pluginList".equals(option)) {
                String pluginListStr = argument;
                Map<String, Boolean> customPlugins = getProject().getConfiguration().getCustomPlugins();
                StringTokenizer tok = new StringTokenizer(pluginListStr, File.pathSeparator);
                while (tok.hasMoreTokens()) {
                    File file = new File(tok.nextToken());
                    Boolean enabled = Boolean.valueOf(file.isFile());
                    customPlugins.put(file.getAbsolutePath(), enabled);
                    if(enabled.booleanValue()) {
                        try {
                            Plugin.loadCustomPlugin(file, getProject());
                        } catch (PluginException e) {
                            throw new IllegalStateException("Failed to load plugin " +
                                    "specified by the '-pluginList', file: " + file, e);
                        }
                    }
                }
            } else if ("-maxRank".equals(option)) {
                maxRank = Integer.parseInt(argument);
            } else if ("-summarizeMaxRank".equals(option)) {
                summarizeMaxRank = Integer.parseInt(argument);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        FindBugs.setNoAnalysis();
        PrintingBugReporter reporter = new PrintingBugReporter();
        PrintingCommandLine commandLine = reporter.new PrintingCommandLine();

        int argCount = commandLine.parse(args, 0, 2, "Usage: " + PrintingCommandLine.class.getName()
                + " [options] [<xml results> [<test results]] ");


        if (commandLine.stylesheet != null) {
            // actually do xsl via HTMLBugReporter instead of
            // PrintingBugReporter
            xslt(commandLine.stylesheet, reporter.isApplySuppressions(), args, argCount);
            return;
        }

        SortedBugCollection bugCollection = new SortedBugCollection(commandLine.getProject());
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }

        if (argCount < args.length) {
            reporter.setOutputStream(UTF8.printStream(new FileOutputStream(args[argCount++]), true));
        }
        boolean bugsReported = false;
        RuntimeException storedException = null;
        if (commandLine.annotationUploadFormat) {
            bugCollection.computeBugHashes();
            for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
                BugInstance warning = i.next();
                try {
                    String fHash = "fb-" + warning.getInstanceHash() + "-" + warning.getInstanceOccurrenceNum() + "-"
                            + warning.getInstanceOccurrenceMax();

                    System.out.print("#" + fHash);
                    String key = warning.getUserDesignationKey();
                    if (key.equals(BugDesignation.UNCLASSIFIED) || "NEEDS_FURTHER_STUDY".equals(key)) {
                        System.out.print("#-1#" + key);
                    } else if ("MUST_FIX".equals(key) || "SHOULD_FIX".equals(key) || "I_WILL_FIX".equals(key)) {
                        System.out.print("#7#" + key);
                    } else {
                        System.out.print("#0#" + key);
                    }
                    SourceLineAnnotation sourceLine = warning.getPrimarySourceLineAnnotation();
                    System.out.println("#" + sourceLine.getSourceFile() + "#" + sourceLine.getStartLine());
                    System.out.println(warning.getAnnotationText());
                } catch (RuntimeException e) {
                    if (storedException == null) {
                        storedException = e;
                    }
                }
            }
        } else {

            Bag<String> lowRank = new Bag<String>(new TreeMap<String, Integer>());
            for (BugInstance warning : bugCollection.getCollection()) {
                if (!reporter.isApplySuppressions() || !bugCollection.getProject().getSuppressionFilter().match(warning)) {
                    int rank = warning.getBugRank();
                    BugPattern pattern = warning.getBugPattern();
                    if (rank <= commandLine.maxRank) {
                        bugsReported = true;
                        try {
                            reporter.printBug(warning);
                        } catch (RuntimeException e) {
                            if (storedException == null) {
                                storedException = e;
                            }
                        }
                    } else if (rank <= commandLine.summarizeMaxRank) {
                        bugsReported = true;
                        lowRank.add(pattern.getCategory());
                    }

                }
            }

            reporter.finish();
            for (Map.Entry<String, Integer> e : lowRank.entrySet()) {
                System.out.printf("%4d low ranked %s issues%n", e.getValue(),
                        I18N.instance().getBugCategoryDescription(e.getKey()));
            }

        }
        if(commandLine.setExitCode){
            int exitCode = 0;
            System.err.println("Calculating exit code...");
            if (storedException != null) {
                exitCode |= ExitCodes.ERROR_FLAG;
                System.err.println("Setting 'errors encountered' flag (" + ExitCodes.ERROR_FLAG + ")");
                storedException.printStackTrace(System.err);
            }
            if (bugsReported) {
                exitCode |= ExitCodes.BUGS_FOUND_FLAG;
                System.err.println("Setting 'bugs found' flag (" + ExitCodes.BUGS_FOUND_FLAG + ")");
            }
            System.err.println("Exit code set to: " + exitCode);
            System.exit(exitCode);
        } else
            if (storedException != null) {
                throw storedException;
            }

    }

    public static void xslt(String stylesheet, boolean applySuppression, String[] args, int argCount) throws Exception {
        Project proj = new Project();
        HTMLBugReporter reporter = new HTMLBugReporter(proj, stylesheet);
        BugCollection bugCollection = reporter.getBugCollection();

        bugCollection.setApplySuppressions(applySuppression);
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }

        if (argCount < args.length) {
            reporter.setOutputStream(UTF8.printStream(new FileOutputStream(args[argCount++]), true));
        }

        reporter.finish();
        Exception e = reporter.getFatalException();
        if (e != null) {
            throw e;
        }
    }

    @Override
    public @CheckForNull
    BugCollection getBugCollection() {
        return null;
    }
}

