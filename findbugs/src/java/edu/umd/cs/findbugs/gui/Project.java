/*
 * Project.java
 *
 * Created on March 30, 2003, 2:22 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;

/**
 * A project in the GUI.
 * This consists of some number of Jar files, and (optionally)
 * some number of source directories.
 *
 * @author David Hovemeyer
 */
public class Project {
    /** The list of jar files. */
    private List jarList;
    
    /** The list of source directories. */
    private List srcDirList;
    
    /** Creates a new instance of Project */
    public Project() {
	jarList = new LinkedList();
	srcDirList = new LinkedList();
    }
    
    /**
     * Add a Jar file to the project.
     * @param fileName the jar file to add
     */
    public void addJar(String fileName) {
	if (!jarList.contains(fileName))
	    jarList.add(fileName);
    }
    
    /*
     * Add a source directory to the project.
     * @param dirName the directory to add
     */
    public void addSourceDir(String dirName) {
	if (!srcDirList.contains(dirName))
	    srcDirList.add(dirName);
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
}
