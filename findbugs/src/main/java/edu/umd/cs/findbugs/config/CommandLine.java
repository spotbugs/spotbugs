/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.util.Util;

/**
 * Helper class for parsing command line arguments.
 */
public abstract class CommandLine {

    private static final String SPACES = "                    ";

    private final List<String> optionList;

    private final Set<String> unlistedOptions;

    private final Map<Integer, String> optionGroups;

    private final Set<String> requiresArgumentSet;

    private final Map<String, String> optionDescriptionMap;

    private final Map<String, String> optionExtraPartSynopsisMap;

    private final Map<String, String> argumentDescriptionMap;

    int maxWidth;

    public CommandLine() {
        this.unlistedOptions = new HashSet<String>();
        this.optionList = new LinkedList<String>();
        this.optionGroups = new HashMap<Integer, String>();
        this.requiresArgumentSet = new HashSet<String>();
        this.optionDescriptionMap = new HashMap<String, String>();
        this.optionExtraPartSynopsisMap = new HashMap<String, String>();
        this.argumentDescriptionMap = new HashMap<String, String>();
        this.maxWidth = 0;
    }

    /**
     * Start a new group of related command-line options.
     *
     * @param description
     *            description of the group
     */
    public void startOptionGroup(String description) {
        optionGroups.put(optionList.size(), description);
    }

    /**
     * Add a command line switch. This method is for adding options that do not
     * require an argument.
     *
     * @param option
     *            the option, must start with "-"
     * @param description
     *            single line description of the option
     */
    public void addSwitch(String option, String description) {
        optionList.add(option);
        optionDescriptionMap.put(option, description);

        if (option.length() > maxWidth) {
            maxWidth = option.length();
        }
    }

    /**
     * Add a command line switch that allows optional extra information to be
     * specified as part of it.
     *
     * @param option
     *            the option, must start with "-"
     * @param optionExtraPartSynopsis
     *            synopsis of the optional extra information
     * @param description
     *            single-line description of the option
     */
    public void addSwitchWithOptionalExtraPart(String option, String optionExtraPartSynopsis, String description) {
        optionList.add(option);
        optionExtraPartSynopsisMap.put(option, optionExtraPartSynopsis);
        optionDescriptionMap.put(option, description);

        // Option will display as -foo[:extraPartSynopsis]
        int length = option.length() + optionExtraPartSynopsis.length() + 3;
        if (length > maxWidth) {
            maxWidth = length;
        }
    }

    /**
     * Add an option requiring an argument.
     *
     * @param option
     *            the option, must start with "-"
     * @param argumentDesc
     *            brief (one or two word) description of the argument
     * @param description
     *            single line description of the option
     */
    public void addOption(String option, String argumentDesc, String description) {
        optionList.add(option);
        optionDescriptionMap.put(option, description);
        requiresArgumentSet.add(option);
        argumentDescriptionMap.put(option, argumentDesc);

        int width = option.length() + 3 + argumentDesc.length();
        if (width > maxWidth) {
            maxWidth = width;
        }
    }

    /**
     * Don't list this option when printing Usage information
     *
     * @param option
     */
    public void makeOptionUnlisted(String option) {
        unlistedOptions.add(option);
    }

    /**
     * Expand option files in given command line. Any token beginning with "@"
     * is assumed to be an option file. Option files contain one command line
     * option per line.
     *
     * @param argv
     *            the original command line
     * @param ignoreComments
     *            ignore comments (lines starting with "#")
     * @param ignoreBlankLines
     *            ignore blank lines
     * @return the expanded command line
     */

    public String[] expandOptionFiles(String[] argv, boolean ignoreComments, boolean ignoreBlankLines) throws IOException,
    HelpRequestedException {
        // Add all expanded options at the end of the options list, before the
        // list of
        // jar/zip/class files and directories.
        // At the end of the options to preserve the order of the options (e.g.
        // -adjustPriority
        // must always come after -pluginList).
        int lastOptionIndex = parse(argv, true);
        ArrayList<String> resultList = new ArrayList<String>();
        ArrayList<String> expandedOptionsList = getAnalysisOptionProperties(ignoreComments, ignoreBlankLines);
        for (int i = 0; i < lastOptionIndex; i++) {
            String arg = argv[i];
            if (!arg.startsWith("@")) {
                resultList.add(arg);
                continue;
            }

            BufferedReader reader = null;
            try {
                reader = UTF8.bufferedReader(new FileInputStream(arg.substring(1)));
                addCommandLineOptions(expandedOptionsList, reader, ignoreComments, ignoreBlankLines);
            } finally {
                Util.closeSilently(reader);
            }
        }
        resultList.addAll(expandedOptionsList);
        for (int i = lastOptionIndex; i < argv.length; i++) {
            resultList.add(argv[i]);
        }

        return resultList.toArray(new String[resultList.size()]);
    }

    public static ArrayList<String> getAnalysisOptionProperties(boolean ignoreComments, boolean ignoreBlankLines) {
        ArrayList<String> resultList = new ArrayList<String>();
        URL u = DetectorFactoryCollection.getCoreResource("analysisOptions.properties");
        if (u != null) {
            BufferedReader reader = null;
            try {
                reader = UTF8.bufferedReader(u.openStream());
                addCommandLineOptions(resultList, reader, ignoreComments, ignoreBlankLines);
            } catch (IOException e) {
                AnalysisContext.logError("unable to load analysisOptions.properties", e);
            } finally {
                Util.closeSilently(reader);
            }
        }
        return resultList;
    }

    private static void addCommandLineOptions(ArrayList<String> resultList, BufferedReader reader, boolean ignoreComments,
            boolean ignoreBlankLines) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (ignoreComments && line.startsWith("#")) {
                continue;
            }

            if (ignoreBlankLines && "".equals(line)) {
                continue;
            }
            if (line.length() >= 2 && line.charAt(0) == '"' && line.charAt(line.length() - 1) == '"') {
                resultList.add(line.substring(0, line.length() - 1));
            } else {
                for (String segment : line.split(" ")) {
                    resultList.add(segment);
                }
            }
        }
    }

    public static class HelpRequestedException extends Exception {

    }

    /**
     * Parse switches/options, showing usage information if they can't be
     * parsed, or if we have the wrong number of remaining arguments after
     * parsing. Calls parse(String[]).
     *
     * @param argv
     *            command line arguments
     * @param minArgs
     *            allowed minimum number of arguments remaining after
     *            switches/options are parsed
     * @param maxArgs
     *            allowed maximum number of arguments remaining after
     *            switches/options are parsed
     * @param usage
     *            usage synopsis
     * @return number of arguments parsed
     */
    @SuppressFBWarnings("DM_EXIT")
    public int parse(String argv[], int minArgs, int maxArgs, String usage) {
        try {
            int count = parse(argv);
            int remaining = argv.length - count;
            if (remaining < minArgs || remaining > maxArgs) {
                System.out.println(usage);
                System.out.println("Expected " + minArgs + "..." + maxArgs + " file arguments, found " + remaining);
                System.out.println("Options:");
                printUsage(System.out);
                System.exit(1);
            }
            return count;
        } catch (HelpRequestedException e) {
            // fall through
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(usage);
        System.out.println("Options:");
        printUsage(System.out);
        System.exit(1);
        return -1;
    }

    /**
     * Parse a command line. Calls down to handleOption() and
     * handleOptionWithArgument() methods. Stops parsing when it reaches the end
     * of the command line, or when a command line argument not starting with
     * "-" is seen.
     *
     * @param argv
     *            the arguments
     * @return the number of arguments parsed; if equal to argv.length, then the
     *         entire command line was parsed
     * @throws HelpRequestedException
     */
    public int parse(String argv[]) throws IOException, HelpRequestedException {
        return parse(argv, false);
    }

    private int parse(String argv[], boolean dryRun) throws IOException, HelpRequestedException {
        int arg = 0;

        while (arg < argv.length) {
            String option = argv[arg];
            if ("-help".equals(option) || "-h".equals(option)) {
                throw new HelpRequestedException();
            }
            if (!option.startsWith("-")) {
                break;
            }
            if (dryRun && option.startsWith("@")) {
                ++arg;
                continue;
            }
            String optionExtraPart = "";
            int colon = option.indexOf(':');
            if (colon >= 0) {
                optionExtraPart = option.substring(colon + 1);
                option = option.substring(0, colon);
            }

            if (optionDescriptionMap.get(option) == null) {
                throw new IllegalArgumentException("Unknown option: " + option);
            }

            if (requiresArgumentSet.contains(option)) {
                ++arg;
                if (arg >= argv.length) {
                    throw new IllegalArgumentException("Option " + option + " requires an argument");
                }
                String argument = argv[arg];
                if (!dryRun) {
                    handleOptionWithArgument(option, argument);
                }
                ++arg;
            } else {
                if (!dryRun) {
                    handleOption(option, optionExtraPart);
                }
                ++arg;
            }
        }

        return arg;
    }

    /**
     * Callback method for handling an option.
     *
     * @param option
     *            the option
     * @param optionExtraPart
     *            the "extra" part of the option (everything after the colon:
     *            e.g., "withMessages" in "-xml:withMessages"); the empty string
     *            if there was no extra part
     */
    protected abstract void handleOption(String option, String optionExtraPart) throws IOException;

    /**
     * Callback method for handling an option with an argument.
     *
     * @param option
     *            the option
     * @param argument
     *            the argument
     */
    protected abstract void handleOptionWithArgument(String option, String argument) throws IOException;

    /**
     * Print command line usage information to given stream.
     *
     * @param os
     *            the output stream
     */
    public void printUsage(OutputStream os) {
        int count = 0;
        PrintStream out = UTF8.printStream(os);
        for (String option : optionList) {

            if (optionGroups.containsKey(count)) {
                out.println("  " + optionGroups.get(count));
            }
            count++;

            if (unlistedOptions.contains(option)) {
                continue;
            }
            out.print("    ");

            StringBuilder buf = new StringBuilder();
            buf.append(option);
            if (optionExtraPartSynopsisMap.get(option) != null) {
                String optionExtraPartSynopsis = optionExtraPartSynopsisMap.get(option);
                buf.append("[:");
                buf.append(optionExtraPartSynopsis);
                buf.append("]");
            }
            if (requiresArgumentSet.contains(option)) {
                buf.append(" <");
                buf.append(argumentDescriptionMap.get(option));
                buf.append(">");
            }
            printField(out, buf.toString(), maxWidth + 1);

            out.println(optionDescriptionMap.get(option));
        }
        out.flush();
    }

    private static void printField(PrintStream out, String s, int width) {
        if (s.length() > width) {
            throw new IllegalArgumentException();
        }
        int nSpaces = width - s.length();
        out.print(s);
        while (nSpaces > 0) {
            int n = Math.min(SPACES.length(), nSpaces);
            out.print(SPACES.substring(0, n));
            nSpaces -= n;
        }
    }
}
