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

package edu.umd.cs.findbugs.workflow;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.CommandLine;
import org.dom4j.DocumentException;

/**
 * Compute the union of two sets of bug results, preserving annotations.
 */
public class UnionResults {

    static class UnionResultsCommandLine extends CommandLine {
        public String outputFile;

        boolean withMessages;

        UnionResultsCommandLine() {
            addSwitch("-withMessages", "Generated XML should contain msgs for external processing");
            addOption("-output", "outputFile", "File in which to store combined results");
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String,
         * java.lang.String)
         */
        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            if ("-withMessages".equals(option)) {
                withMessages = true;
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java
         * .lang.String, java.lang.String)
         */
        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
            if ("-output".equals(option)) {
                outputFile = argument;
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }

    }

    static {
        DetectorFactoryCollection.instance(); // as a side effect, loads
        // detector plugins
    }

    static public SortedBugCollection union(SortedBugCollection origCollection, SortedBugCollection newCollection) {
        SortedBugCollection result = origCollection.duplicate();
        merge(null, result, newCollection);
        return result;
    }

    static public void merge(HashSet<String> hashes, SortedBugCollection into, SortedBugCollection from) {

        for (BugInstance bugInstance : from.getCollection()) {
            if (hashes == null || hashes.add(bugInstance.getInstanceHash())) {
                into.add(bugInstance);
            }
        }
        ProjectStats stats = into.getProjectStats();
        ProjectStats stats2 = from.getProjectStats();
        stats.addStats(stats2);

        Project project = into.getProject();
        Project project2 = from.getProject();
        project.add(project2);

        for (AnalysisError error : from.getErrors()) {
            into.addError(error);
        }

        return;
    }

    public static void main(String[] argv) throws IOException {

        FindBugs.setNoAnalysis();
        final UnionResultsCommandLine commandLine = new UnionResultsCommandLine();

        int argCount = commandLine.parse(argv, 1, Integer.MAX_VALUE, "Usage: " + UnionResults.class.getName()
                + " [options] [<results1> <results2> ... <resultsn>] ");

        SortedBugCollection results = null;
        HashSet<String> hashes = new HashSet<>();

        results = iterateArguments(Stream.of(argv).collect(Collectors.toList()), argCount, results, hashes);

        if (results == null) {
            System.err.println("No files successfully read");
            System.exit(1);
            return;
        }
        results.setWithMessages(commandLine.withMessages);
        if (commandLine.outputFile == null) {
            results.writeXML(System.out);
        } else {
            results.writeXML(commandLine.outputFile);
        }
    }

    private static SortedBugCollection iterateArguments(List<String> arguments, int argCount, SortedBugCollection results, HashSet<String> hashes) {
        for (int i = argCount; i < arguments.size(); i++) {
            String fileName = arguments.get(i);
            try {
                SortedBugCollection more = new SortedBugCollection();
                if (fileName.endsWith(".txt")) {
                    List<String> wrappedArguments = readWrappedArguments(fileName);
                    if (results == null) {
                        results = more.createEmptyCollectionWithMetadata();
                    }
                    merge(hashes, results, iterateArguments(wrappedArguments, 0, results, hashes));
                    continue;
                }
                more.readXML(fileName);
                if (results == null) {
                    results = more.createEmptyCollectionWithMetadata();
                }
                merge(hashes, results, more);
            } catch (IOException e) {
                System.err.println("Trouble reading/parsing " + fileName);
            } catch (DocumentException e) {
                System.err.println("Trouble reading/parsing " + fileName);
            }
        }
        return results;
    }

    private static List<String> readWrappedArguments(String fileName) throws IOException {
        List<String> wrappedArguments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), Charset.forName("UTF-8")))) {
            String next;
            while ((next = reader.readLine()) != null) {
                wrappedArguments.add(next);
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
        return wrappedArguments;
    }

}
