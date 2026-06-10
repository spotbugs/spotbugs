/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.anttask;

import java.io.File;

/**
 * FindBugsViewerTask.java -- Ant Task to launch the FindBugsFrame
 *
 * To use, create a new task that references the ant task (such as
 * "findbugs-viewer"). Then call this task while passing in parameters to modify
 * it's behaviour. It supports several options that are the same as the findbugs
 * task:
 *
 * -projectFile -debug -jvmargs -home -classpath -pluginList -timeout
 *
 * It also adds some new options:
 *
 * -look: string name representing look and feel. Can be "native", "plastic" or
 * "gtk" -loadbugs: file name of bug report to load
 *
 * The below is an example of how this could be done in an ant script:
 *
 * {@literal <taskdef name="findbugs" classname="edu.umd.cs.findbugs.anttask.FindBugsTask" classpath="C:\dev\cvs.sourceforge.net\findbugs\lib\findbugs-ant.jar" />}
 * {@literal <taskdef name="findbugs-viewer"
 * classname="edu.umd.cs.findbugs.anttask.FindBugsViewerTask"
 * classpath="C:\dev\cvs.sourceforge.net\findbugs\lib\findbugs-ant.jar" />}
 *
 * {@literal <property name="findbugs.home" location="C:\dev\cvs.sourceforge.net\findbugs"
 * /> <property name="findbugs.bugReport" location="bcel-fb.xml" />}
 *
 * {@literal <target name="findbugs-viewer" depends="jar"> <findbugs-viewer
 * home="${findbugs.home}" look="native" loadbugs="${findbugs.bugReport}"/>
 * </target>}
 *
 * Created on March 21, 2006, 12:57 PM
 *
 * @author Mark McKay, mark@kitfox.com
 */
public class FindBugsViewerTask extends AbstractFindBugsTask {

    private static final long DEFAULT_TIMEOUT = -1; // no timeout

    // location to load bug report from
    private boolean debug;

    private File projectFile;

    private File loadbugs;

    private String look;

    /** Creates a new instance of FindBugsViewerTask */
    public FindBugsViewerTask() {
        super("edu.umd.cs.findbugs.LaunchAppropriateUI");
        setTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * Sets the file that contains the XML output of a findbugs report.
     *
     * @param loadbugs
     *            XML output from a findbugs session
     */
    public void setLoadbugs(File loadbugs) {
        this.loadbugs = loadbugs;
    }

    /**
     * Set the project file
     *
     * @param projectFile
     *            project file
     */
    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    /**
     * Set look. One of "native", "gtk" or "plastic"
     *
     * @param look
     *            One of "native", "gtk" or "plastic
     */
    public void setLook(String look) {
        this.look = look;
    }

    @Override
    protected void configureFindbugsEngine() {
        if (projectFile != null) {
            addArg("-project");
            addArg(projectFile.getPath());
        }

        if (loadbugs != null) {
            addArg("-loadbugs");
            addArg(loadbugs.getPath());
        }

        if (look != null) {
            addArg("-look:" + look);
            // addArg("-look");
            // addArg(look);
        }
    }

    @Override
    protected void beforeExecuteJavaProcess() {
        log("Launching FindBugs Viewer...");
    }

}
