/*
 * AnalysisRun.java
 *
 * Created on April 1, 2003, 2:24 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;
import javax.swing.tree.DefaultTreeModel;
import edu.umd.cs.findbugs.*;

/**
 * Representation of a run of the FindBugs analysis on a Project.
 * This class has convenient methods which can be used to extract
 * bug reports in various interesting ways.
 *
 * @author David Hovemeyer
 */
public class AnalysisRun {
    /**
     * Our BugReporter just puts the reported BugInstances into a HashSet.
     */
    private class Reporter implements BugReporter {
        private HashSet bugSet = new HashSet();
        
        public void finish() { }
        
        public void reportBug(edu.umd.cs.findbugs.BugInstance bugInstance) {
            bugSet.add(bugInstance);
        }
        
        public void logError(String message) {
            logger.logMessage(ConsoleLogger.ERROR, message);
        }
    }

    private Project project;
    private ConsoleLogger logger;
    private FindBugs findBugs;
    private Reporter reporter;
    private int runNumber;
    private HashMap treeModelMap;
    
    /** Creates a new instance of AnalysisRun. */
    public AnalysisRun(Project project, ConsoleLogger logger) {
        this.project = project;
        this.logger = logger;
        reporter = new Reporter();
        findBugs = new FindBugs(reporter);
	runNumber = project.getNextAnalysisRun();
        treeModelMap = new HashMap();
    }
    
    /**
     * Run the analysis.
     * This should be done in a separate thread (not the GUI event thread).
     * The progress callback can be used to update the user interface to
     * reflect the progress of the analysis.  The GUI may cancel the analysis
     * by interrupting the analysis thread, in which case InterruptedException
     * will be thrown by this method.
     *
     * @param progressCallback the progress callback
     * @throws java.io.IOException if an I/O error occurs during the analysis
     * @throws InterruptedException if the analysis thread is interrupted
     */
    public void execute(FindBugsProgress progressCallback) throws java.io.IOException, InterruptedException {
        findBugs.setProgressCallback(progressCallback);
        
        // Run the analysis!
        findBugs.execute(project.getJarFileArray());
    }
    
    /**
     * Return the collection of BugInstances.
     */
    public java.util.Collection getBugInstances() {
        return reporter.bugSet;
    }
    
    /**
     * Set the tree model to be used in the BugTree.
     * @param groupByOrder the grouping order that the tree model will conform to
     * @param treeModel the tree model
     */
    public void setTreeModel(String groupByOrder, DefaultTreeModel treeModel) {
        treeModelMap.put(groupByOrder, treeModel);
    }
    
    /**
     * Get the tree model to be used in the BugTree.
     * @param groupByOrder the grouping order that the tree model conforms to
     * @return the tree model
     */
    public DefaultTreeModel getTreeModel(String groupByOrder) {
        return (DefaultTreeModel) treeModelMap.get(groupByOrder);
    }
    
    /**
     * Look up the source file for given class.
     * @return the source file name, or null if we don't have a source filename
     *   for the class
     */
    public String getSourceFile(String className) {
	return findBugs.getSourceFile(className);
    }
    
    public String toString() {
	return "Run " + runNumber;
    }
    
}
