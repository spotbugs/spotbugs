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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import edu.umd.cs.findbugs.workflow.UnionResults;

/**
 * An ant task that is wraps the behavior of the UnionResults executable into an
 * ant task.
 *
 * <taskdef name="UnionBugs" classname="edu.umd.cs.findbugs.anttask.UnionBugs"
 * classpath="...">
 *
 * <UnionBugs to="${basedir}/findbugs.xml" > <fileset dir="plugins"> <include
 * name="*_findbugs_partial.xml" /> </fileset> </UnionBugs>
 *
 * @author Peter Franza <a href="mailto:pfranza@gmail.com">pfranza@gmail.com</a>
 * @version 1.0
 *
 * @ant.task category="utility"
 *
 */
@Deprecated
public class UnionBugs extends Task {

    private final List<FileSet> fileSets = new ArrayList<FileSet>();

    private String into;

    /**
     * The fileset containing all the findbugs xml files that need to be merged
     *
     * @param arg
     */
    public void addFileset(FileSet arg) {
        fileSets.add(arg);
    }

    /**
     * The File everything should get merged into
     *
     * @param file
     */
    public void setTo(String file) {
        into = file;
    }

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {

            List<File> fileList = createListOfAllFilesToMerge();

            // If there is nothing to merge, don't
            if (fileList.isEmpty()) {
                return;
            }

            // if the merge target doesn't exist yet, then the first file
            // becomes
            // the merge target
            File to = new File(into);
            if (!to.exists()) {
                File from = fileList.remove(0);
                copyFile(from, to);
            }

            // If there was only one file, and it has been copied as
            // the merge target then you're done
            if (fileList.isEmpty()) {
                return;
            }

            UnionResults.main(createCommandArgumentsArray(fileList));
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private List<File> createListOfAllFilesToMerge() {
        List<File> fileList = new ArrayList<File>();
        for (FileSet s : fileSets) {
            File fromDir = s.getDir(getProject());
            for (String file : s.getDirectoryScanner(getProject()).getIncludedFiles()) {
                fileList.add(new File(fromDir, file));
            }
        }
        return fileList;
    }

    private String[] createCommandArgumentsArray(List<File> fileList) {
        List<String> parts = new ArrayList<String>();
        parts.add("-withMessages");
        parts.add("-output");
        parts.add(into);

        parts.add(into);

        for (File f : fileList) {
            parts.add(f.getAbsolutePath());
        }

        String[] args = parts.toArray(new String[parts.size()]);
        return args;
    }

    /**
     * Copy a File
     *
     * @param in
     *            to Copy From
     * @param out
     *            to Copy To
     * @throws IOException
     */
    private static void copyFile(File in, File out) throws IOException {
        try (FileInputStream inStream = new FileInputStream(in);
                FileOutputStream outStream = new FileOutputStream(out);) {
            FileChannel inChannel = inStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outStream.getChannel());
        }
    }

}
