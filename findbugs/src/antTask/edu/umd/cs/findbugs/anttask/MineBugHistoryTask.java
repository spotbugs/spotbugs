/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.anttask;

import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * Ant task to invoke the MineBugHistory program in the workflow package
 *
 * @author David Hovemeyer
 * @author Ben Langmead
 */
public class MineBugHistoryTask extends AbstractFindBugsTask {

    private File outputFile;

    private String formatDates;

    private String noTabs;

    private String summary;

    private DataFile inputFile;

    public MineBugHistoryTask() {
        super("edu.umd.cs.findbugs.workflow.MineBugHistory");
        setFailOnError(true);
    }

    public DataFile createDataFile() {
        if (inputFile != null) {
            throw new BuildException("only one dataFile element is allowed", getLocation());
        }
        inputFile = new DataFile();
        return inputFile;
    }

    public void setOutput(File output) {
        this.outputFile = output;
    }

    public void setInput(String input) {
        this.inputFile = new DataFile();
        this.inputFile.name = input;
    }

    public void setFormatDates(String arg) {
        this.formatDates = arg;
    }

    public void setNoTabs(String arg) {
        this.noTabs = arg;
    }

    public void setSummary(String arg) {
        this.summary = arg;
    }

    private void checkBoolean(String attrVal, String attrName) {
        if (attrVal == null) {
            return;
        }
        attrVal = attrVal.toLowerCase();
        if (!"true".equals(attrVal) && !"false".equals(attrVal)) {
            throw new BuildException("attribute " + attrName + " requires boolean value", getLocation());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#checkParameters()
     */
    @Override
    protected void checkParameters() {
        super.checkParameters();

        if (inputFile == null) {
            throw new BuildException("inputFile element is required");
        }

        checkBoolean(formatDates, "formatDates");
        checkBoolean(noTabs, "noTabs");
        checkBoolean(summary, "summary");
    }

    public void addBoolOption(String option, String value) {
        if (value != null) {
            addArg(option + ":" + value);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#configureFindbugsEngine
     * ()
     */
    @Override
    protected void configureFindbugsEngine() {
        addBoolOption("-formatDates", formatDates);
        addBoolOption("-noTabs", noTabs);
        addBoolOption("-summary", summary);
        addArg(inputFile.getName());
        if (outputFile != null) {
            // Don't use .getName() because it discards path
            addArg(outputFile.getAbsolutePath());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess
     * ()
     */
    @Override
    protected void beforeExecuteJavaProcess() {
        log("running mineBugHistory...");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#afterExecuteJavaProcess
     * (int)
     */
    @Override
    protected void afterExecuteJavaProcess(int rc) {
        if (rc != 0) {
            throw new BuildException("execution of " + getTaskName() + " failed");
        }
    }

}
