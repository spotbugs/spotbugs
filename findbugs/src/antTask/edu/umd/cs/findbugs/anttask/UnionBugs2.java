/*
 * UnionBugs - Ant Task to Merge Findbugs Bug Reports
 * Copyright (C) 2008 peterfranza.com
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
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

/**
 * An ant task that is wraps the behavior of the UnionResults executable into an
 * ant task.
 *
 * <taskdef name="UnionBugs2" classname="edu.umd.cs.findbugs.anttask.UnionBugs2"
 * classpath="...">
 *
 * <UnionBugs2 to="${basedir}/findbugs.xml" > <fileset dir="plugins"> <include
 * name="*_findbugs_partial.xml" /> </fileset> </UnionBugs>
 *
 *
 * @ant.task category="utility"
 *
 */

public class UnionBugs2 extends AbstractFindBugsTask {

    private String to;

    private final ArrayList<FileSet> fileSets = new ArrayList<FileSet>();

    public UnionBugs2() {
        super("edu.umd.cs.findbugs.workflow.UnionResults");
        setFailOnError(true);
    }

    /**
     * The fileset containing all the findbugs xml files that need to be merged
     *
     * @param arg
     */
    public void addFileset(FileSet arg) {
        this.fileSets.add(arg);
    }

    /**
     * The File everything should get merged into
     *
     * @param arg
     */
    public void setTo(String arg) {
        this.to = arg;
    }

    @Override
    protected void checkParameters() {
        super.checkParameters();

        if (to == null) {
            throw new BuildException("to attribute is required", getLocation());
        }

        if (fileSets.size() < 1) {
            throw new BuildException("fileset is required");
        }
    }


    @Override
    protected void beforeExecuteJavaProcess() {
        log("unioning bugs...");
    }

    @Override
    protected void configureFindbugsEngine() {
        addArg("-withMessages");
        addArg("-output");
        addArg(to);
        for (FileSet s : fileSets) {
            File fromDir = s.getDir(getProject());
            for (String file : s.getDirectoryScanner(getProject()).getIncludedFiles()) {
                addArg(new File(fromDir, file).toString());
            }
        }

    }

}
