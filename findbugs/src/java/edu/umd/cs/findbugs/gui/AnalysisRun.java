/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;

import javax.swing.tree.DefaultTreeModel;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.config.UserPreferences;



/**
 * Representation of a run of the FindBugs analysis on a Project.
 * This class has convenient methods which can be used to extract
 * bug reports in various interesting ways.
 *
 * @author David Hovemeyer
 */
public class AnalysisRun {
	private Project project;
	private FindBugsFrame frame;
	private String summary;
	private Logger logger;
	private FindBugs findBugs;
	private SwingGUIBugReporter reporter;
	private HashMap<String, DefaultTreeModel> treeModelMap;

	/**
	 * Creates a new instance of AnalysisRun.
	 */
	public AnalysisRun(Project project, FindBugsFrame frame) {
		this.project = project;
		this.frame = frame;
		this.logger = frame.getLogger();
		this.reporter = new SwingGUIBugReporter(this);
		this.reporter.setPriorityThreshold(Detector.EXP_PRIORITY);
		this.findBugs = new FindBugs(reporter, project);
		this.treeModelMap = new HashMap<String, DefaultTreeModel>();
	}
	
	/**
	 * Get the FindBugsFrame which created this analysis run.
	 * 
	 * @return the FindBugsFrame
	 */
	public FindBugsFrame getFrame() {
		return frame;
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
	 * @throws IOException          if an I/O error occurs during the analysis
	 * @throws InterruptedException if the analysis thread is interrupted
	 */
	public void execute(FindBugsProgress progressCallback) throws IOException, InterruptedException {
		findBugs.setProgressCallback(progressCallback);
		
		// Honor current UserPreferences
		findBugs.setUserPreferences(UserPreferences.getUserPreferences());
		
		// Set analysis feature settings
		findBugs.setAnalysisFeatureSettings(frame.getSettingList());

		// Run the analysis!
		findBugs.execute();

		if (!SystemProperties.getBoolean("findbugs.noSummary")) {
			// Get the summary!
			createSummary(reporter.getProjectStats());
		}

	}

	private static final String MISSING_SUMMARY_MESSAGE =
	        "<html><head><title>Could not format summary</title></head>" +
	        "<body><h1>Could not format summary</h1>" +
	        "<p> Please report this failure to <a href=\"findbugs-discuss@cs.umd.edu\">" +
	        "findbugs-discuss@cs.umd.edu</a>.</body></html>";

	private void createSummary(ProjectStats stats) throws IOException {
		StringWriter html = new StringWriter();
		try {
			stats.transformSummaryToHTML(html);
			summary = html.toString();
		} catch (Exception e) {
			logger.logMessage(ConsoleLogger.WARNING, MessageFormat.format(L10N.getLocalString("msg.failedtotransform_txt", "Failed to transform summary: {0}"), new Object[]{e.toString()}));
			summary = MISSING_SUMMARY_MESSAGE;
		}
	}

	private static final boolean CREATE_SUMMARY = !SystemProperties.getBoolean("findbugs.noSummary");

	/**
	 * Load bugs from a file.
	 */
	public void loadBugsFromFile(File file) throws IOException, org.dom4j.DocumentException {
		reporter.getBugCollection().readXML(file, project);

		// Update summary stats
		summary = reporter.getBugCollection().getSummaryHTML();
	}
	
	/**
	 * Load bugs from an InputStream.
	 * 
	 * @param in the InputStream
	 * @throws IOException
	 * @throws DocumentException
	 */
	public void loadBugsFromInputStream(InputStream in) throws IOException, DocumentException {
		reporter.getBugCollection().readXML(in, project);
		
		// Update summary stats
		summary = reporter.getBugCollection().getSummaryHTML();
	}

	/**
	 * Save bugs to a file.
	 */
	public void saveBugsToFile(File file) throws IOException {
		reporter.getBugCollection().writeXML(file, project);
	}

	/**
	 * Report any errors that may have occurred during analysis.
	 */
	public void reportAnalysisErrors() {
		if (reporter.errorsOccurred()) {
			reporter.getErrorDialog().setSize(750, 520);
			reporter.getErrorDialog().setLocationRelativeTo(null); // center the dialog
			reporter.getErrorDialog().setVisible(true);
		}
	}

	/**
	 * Return the collection of BugInstances.
	 */
	public java.util.Collection<BugInstance> getBugInstances() {
		return reporter.getBugCollection().getCollection();
	}

	/**
	 * Set the tree model to be used in the BugTree.
	 *
	 * @param groupByOrder the grouping order that the tree model will conform to
	 * @param treeModel    the tree model
	 */
	public void setTreeModel(String groupByOrder, DefaultTreeModel treeModel) {
		treeModelMap.put(groupByOrder, treeModel);
	}

	/**
	 * Get the tree model to be used in the BugTree.
	 *
	 * @param groupByOrder the grouping order that the tree model conforms to
	 * @return the tree model
	 */
	public DefaultTreeModel getTreeModel(String groupByOrder) {
		return treeModelMap.get(groupByOrder);
	}

	public String getSummary() {
		return summary;
	}
}
