/*
 * AnalysisRun.java
 *
 * Created on April 1, 2003, 2:24 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.*;
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
    private static class Reporter implements BugReporter {
        HashSet bugSet = new HashSet();
        
        public void finish() { }
        
        public void reportBug(edu.umd.cs.findbugs.BugInstance bugInstance) {
            bugSet.add(bugInstance);
        }
    }

    private Project project;
    private FindBugs findBugs;
    private Reporter reporter;
    
    /** Creates a new instance of AnalysisRun. */
    public AnalysisRun(Project project) {
        this.project = project;
        reporter = new Reporter();
        findBugs = new FindBugs(reporter);
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
    
}
