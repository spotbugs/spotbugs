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
 * AnalysisRun.java
 *
 * Created on April 1, 2003, 2:24 PM
 */

package edu.umd.cs.findbugs.gui;

import java.io.File;
import java.util.*;
import javax.swing.tree.DefaultTreeModel;
import edu.umd.cs.findbugs.*;
import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

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
    private class Reporter extends AbstractBugReporter {
        private HashSet<BugInstance> bugSet = new HashSet<BugInstance>();
        
        public void finish() { }
        
        public void reportBug(edu.umd.cs.findbugs.BugInstance bugInstance) {
            bugSet.add(bugInstance);
        }
        
        public void beginReport() {
            errorDialog = new AnalysisErrorDialog(frame, true);
        }
        
	public void reportLine(String msg) {
            errorDialog.addLine(msg);
        }
        
	public void endReport() {
            errorDialog.finish();
        }
    }

    private Project project;
    private FindBugsFrame frame;
    private ConsoleLogger logger;
    private FindBugs findBugs;
    private Reporter reporter;
    private HashMap<String, DefaultTreeModel> treeModelMap;
    private AnalysisErrorDialog errorDialog;
    
    /** Creates a new instance of AnalysisRun. */
    public AnalysisRun(Project project, FindBugsFrame frame) {
        this.project = project;
        this.frame = frame;
        this.logger = frame.getLogger();
        reporter = new Reporter();
        findBugs = new FindBugs(reporter);
        treeModelMap = new HashMap<String, DefaultTreeModel>();
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

	// Create a ClassPath and SyntheticRepository to reflect the exact classpath
	// that should be used for the analysis.

	// Add aux class path entries specified in project
	StringBuffer buf = new StringBuffer();
	List auxClasspathEntryList = project.getAuxClasspathEntryList();
	Iterator i = auxClasspathEntryList.iterator();
	while (i.hasNext()) {
	    String entry = (String) i.next();
	    buf.append(entry);
	    buf.append(File.pathSeparatorChar);
	}

	// Add the system classpath entries
	buf.append(ClassPath.getClassPath());

	// Set up the Repository to use the combined classpath
	ClassPath classPath = new ClassPath(buf.toString());
	SyntheticRepository repository = SyntheticRepository.getInstance(classPath);
	Repository.setRepository(repository);
        
        // Run the analysis!
        findBugs.execute(project.getJarFileArray());
    }

    /**
     * Report any errors that may have occurred during analysis.
     */
    public void reportAnalysisErrors() {
	if (errorDialog != null) {
            errorDialog.setSize(750, 520);
            errorDialog.setLocationRelativeTo(null); // center the dialog
            errorDialog.show();
	}
    }
    
    /**
     * Return the collection of BugInstances.
     */
    public java.util.Collection<BugInstance> getBugInstances() {
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
    
}
