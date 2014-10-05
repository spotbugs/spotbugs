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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import edu.umd.cs.findbugs.ExitCodes;

/**
 * Ant task to generate HTML or plain text from a saved XML analysis results
 * file.
 *
 * @author David Hovemeyer
 */
public class ConvertXmlToTextTask extends AbstractFindBugsTask {

    private boolean longBugCodes;

    private boolean applySuppression;

    private boolean failIfBugFound;

    private String input;

    private String output;

    private String format = "html";

    public ConvertXmlToTextTask() {
        super("edu.umd.cs.findbugs.PrintingBugReporter");

        setFailOnError(true);
    }

    /**
     * @param longBugCodes
     *            The longBugCodes to set.
     */
    public void setLongBugCodes(boolean longBugCodes) {
        this.longBugCodes = longBugCodes;
    }

    /**
     * @param applySuppression
     *            The applySuppression to set.
     */
    public void setApplySuppression(boolean applySuppression) {
        this.applySuppression = applySuppression;
    }

    /**
     * @param input
     *            The input to set.
     */
    public void setInput(String input) {
        this.input = input;
    }

    /**
     * @param output
     *            The output to set.
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @param input
     *            The input to set.
     */
    public void setInputFile(String input) {
        this.input = input;
    }

    /**
     * @param output
     *            The output to set.
     */
    public void setOutputFile(String output) {
        this.output = output;
    }

    /**
     * @param format
     *            The format to set.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @param failIfBugFound true to 'fail' at the end if at least one bug is reported
     */
    public void setFailIfBugFound(boolean failIfBugFound) {
        this.failIfBugFound = failIfBugFound;
    }

    @Override
    protected void checkParameters() {
        if (input == null) {
            throw new BuildException("input attribute is required", getLocation());
        }
        if (!"text".equals(format) && !("html".equals(format) || format.startsWith("html:"))) {
            throw new BuildException("invalid value " + format + " for format attribute", getLocation());
        }

    }

    @Override
    protected void configureFindbugsEngine() {
        addArg("-exitcode");
        if (format.startsWith("html")) {
            addArg("-" + format);
        }
        if (longBugCodes) {
            addArg("-longBugCodes");
        }
        if (applySuppression) {
            addArg("-applySuppression");
        }
        addArg(input);
        if (output != null) {
            addArg(output);
        }
    }

    @Override
    protected void beforeExecuteJavaProcess() {
        if (output != null) {
            log("Converting " + input + " to " + output + " using format " + format);
        } else {
            log("Converting " + input + " using format " + format);
        }
    }

    @Override
    protected void afterExecuteJavaProcess(int rc) {
        if (rc == 0) {
            log("Success");
        } else {
            if (errorProperty != null) {
                getProject().setProperty(errorProperty, "true");
            }
            if ((rc & ExitCodes.ERROR_FLAG) != 0) {
                String message = "At least one error occured!";
                if (failIfBugFound) {
                    throw new BuildException(message);
                } else {
                    log(message, Project.MSG_ERR);
                }
            }
            if ((rc & ExitCodes.BUGS_FOUND_FLAG) != 0) {
                String message = "At least one unexpected bug is reported!";
                if (failIfBugFound) {
                    throw new BuildException(message);
                } else {
                    log(message, Project.MSG_ERR);
                }
            }
        }
    }

}
