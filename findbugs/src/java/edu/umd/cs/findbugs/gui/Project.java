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
    /** Project filename. */
    private String fileName;
    
    /** The list of jar files. */
    private ArrayList jarList;
    
    /** The list of source directories. */
    private ArrayList srcDirList;
    
    /** Creates a new instance of Project */
    public Project(String fileName) {
        this.fileName = fileName;
	jarList = new ArrayList();
	srcDirList = new ArrayList();
    }
    
    /** Get the project filename. */
    public String getFileName() { return fileName; }
    
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
    
    public String toString() { return fileName; }
}
