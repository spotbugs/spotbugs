/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

/*
 * Project.java
 *
 * Created on March 30, 2003, 2:22 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;
import java.io.*;

/**
 * A project in the GUI.
 * This consists of some number of Jar files, and (optionally)
 * some number of source directories.
 *
 * @author David Hovemeyer
 */
public class Project {
    /** Project filename. */
    private String fileName;
    
    /** The list of jar files. */
    private ArrayList jarList;
    
    /** The list of source directories. */
    private ArrayList srcDirList;
    
    /** Number of analysis runs done so far on this project. */
    private int numAnalysisRuns;
    
    /** Creates a new instance of Project */
    public Project(String fileName) {
        this.fileName = fileName;
	jarList = new ArrayList();
	srcDirList = new ArrayList();
	numAnalysisRuns = 0;
    }
    
    /** Get the project filename. */
    public String getFileName() { return fileName; }
    
    /**
     * Set the project filename.
     * @param fileName the new filename
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Add a Jar file to the project.
     * @param fileName the jar file to add
     * @return true if the jar file was added, or false if the jar
     *   file was already present
     */
    public boolean addJar(String fileName) {
	if (!jarList.contains(fileName)) {
	    jarList.add(fileName);
            return true;
        } else
            return false;
    }
    
    /*
     * Add a source directory to the project.
     * @param dirName the directory to add
     * @return true if the source directory was added, or false if the
     *   source directory was already present
     */
    public boolean addSourceDir(String dirName) {
	if (!srcDirList.contains(dirName)) {
	    srcDirList.add(dirName);
            return true;
        } else
            return false;
    }
    
    /**
     * Get the number of jar files in the project.
     * @return the number of jar files in the project
     */
    public int getNumJarFiles() { return jarList.size(); }
    
    /**
     * Get the given Jar file.
     * @param num the number of the jar file
     * @return the name of the jar file
     */
    public String getJarFile(int num) { return (String) jarList.get(num); }
    
    /**
     * Remove jar file at given index.
     * @param num index of the jar file to remove
     */
    public void removeJarFile(int num) {
        jarList.remove(num);
    }
    
    /**
     * Get the number of source directories in the project.
     * @return the number of source directories in the project
     */
    public int getNumSourceDirs() { return srcDirList.size(); }
    
    /**
     * Get the given source directory.
     * @param num the number of the source directory
     * @return the source directory
     */
    public String getSourceDir(int num) { return (String) srcDirList.get(num); }
    
    /**
     * Remove source directory at given index.
     * @param num index of the source directory to remove
     */
    public void removeSourceDir(int num) {
        srcDirList.remove(num);
    }
    
    /**
     * Get Jar files as an array of Strings.
     */
    public String[] getJarFileArray() {
	return (String[]) jarList.toArray(new String[0]);
    }
    
    /**
     * Get source dirs as an array of Strings.
     */
    public String[] getSourceDirArray() {
	return (String[]) srcDirList.toArray(new String[0]);
    }
    
    /**
     * Get the source dir list.
     */
    public List getSourceDirList() {
	return srcDirList;
    }
    
    /**
     * Get the number of the next analysis run.
     * The runs are numbered starting at 1.
     */
    public int getNextAnalysisRun() {
	return ++numAnalysisRuns;
    }

    private static final String JAR_FILES_KEY = "[Jar files]";
    private static final String SRC_DIRS_KEY = "[Source dirs]";
    
    /**
     * Save the project to an output stream.
     * @param out the OutputStream to write the project to
     * @throws IOException if an error occurs while writing
     */
    public void write(OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.println(JAR_FILES_KEY);
        for (Iterator i = jarList.iterator(); i.hasNext(); ) {
            String jarFile = (String) i.next();
            writer.println(jarFile);
        }
        writer.println(SRC_DIRS_KEY);
        for (Iterator i = srcDirList.iterator(); i.hasNext(); ) {
            String srcDir = (String) i.next();
            writer.println(srcDir);
        }
        writer.close();
    }
    
    /**
     * Read the project from an input stream.
     * @param in the InputStream to read the project from
     * @throws IOException if an error occurs while reading
     */
    public void read(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        line = reader.readLine();
        if (line == null || !line.equals(JAR_FILES_KEY)) throw new IOException("Bad format: missing jar files key");
        while ((line = reader.readLine()) != null && !line.equals(SRC_DIRS_KEY)) {
            jarList.add(line);
        }
        if (line == null) throw new IOException("Bad format: missing source dirs key");
        while ((line = reader.readLine()) != null) {
            srcDirList.add(line);
        }
        
        reader.close();
    }
    
    public String toString() {
        String name = fileName;
        int lastSep = name.lastIndexOf(File.separatorChar);
        if (lastSep >= 0)
            name = name.substring(lastSep + 1);
        return name;
    }
}
