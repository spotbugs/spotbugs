/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2010, University of Maryland
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

import java.io.IOException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Obfuscate;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;

public class ObfuscateBugs {
    BugCollection bugCollection;

    public ObfuscateBugs() {
    }

    public ObfuscateBugs(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public void setBugCollection(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public ObfuscateBugs execute() {
        ProjectPackagePrefixes foo = new ProjectPackagePrefixes();

        for (BugInstance b : bugCollection.getCollection()) {
            foo.countBug(b);
        }
        foo.report();

        return this;
    }

    static class CommandLine extends edu.umd.cs.findbugs.config.CommandLine {

        @Override
        public void handleOption(String option, String optionalExtraPart) {
            throw new IllegalArgumentException("unknown option: " + option);
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
            throw new IllegalArgumentException("Unknown option : " + option);
        }

    }

    public static void main(String[] args) throws Exception {
        FindBugs.setNoAnalysis();
        CommandLine commandLine = new CommandLine();

        int argCount = commandLine.parse(args, 0, 2, "Usage: " + ObfuscateBugs.class.getName() + " [options] [<xml results>] ");

        SortedBugCollection bugCollection = new SortedBugCollection();
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }

        SortedBugCollection results = bugCollection.createEmptyCollectionWithMetadata();
        Project project = results.getProject();
        project.getSourceDirList().clear();
        project.getFileList().clear();
        project.getAuxClasspathEntryList().clear();

        results.getProjectStats().getPackageStats().clear();
        results.clearMissingClasses();
        results.clearErrors();

        for (BugInstance bug : bugCollection) {
            results.add(Obfuscate.obfuscate(bug), false);
        }

        if (argCount == args.length) {
            results.writeXML(System.out);
        } else {
            results.writeXML(args[argCount++]);

        }

    }
}
