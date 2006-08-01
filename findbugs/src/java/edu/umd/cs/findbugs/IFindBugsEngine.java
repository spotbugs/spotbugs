/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.IOException;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IClassObserver;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;

/**
 * Interface for a FindBugs engine class.
 * An instance of this interface takes a project, user configuration
 * options, orchestrates the analysis of the classes
 * in the project, and reports the results to the
 * configured BugReporter.
 * 
 * @author David Hovemeyer
 */
public interface IFindBugsEngine {

	/**
	 * Get the BugReporter.
	 * 
	 * @return the BugReporter
	 */
	public BugReporter getBugReporter();

	/**
	 * Set the BugReporter.
	 * 
	 * @param bugReporter The BugReporter to set
	 */
	public void setBugReporter(BugReporter bugReporter);

	/**
	 * Set the Project.
	 * 
	 * @param project The Project to set
	 */
	public void setProject(Project project);
	
	/**
	 * Get the Project.
	 * 
	 * @return the Project
	 */
	public Project getProject();

	/**
	 * Set the progress callback that will be used to keep track
	 * of the progress of the analysis.
	 *
	 * @param progressCallback the progress callback
	 */
	public void setProgressCallback(FindBugsProgress progressCallback);

	/**
	 * Set filter of bug instances to include or exclude.
	 *
	 * @param filterFileName the name of the filter file
	 * @param include        true if the filter specifies bug instances to include,
	 *                       false if it specifies bug instances to exclude
	 */
	public void addFilter(String filterFileName, boolean include)
			throws IOException, FilterException;

	/**
	 * Set the UserPreferences representing which Detectors
	 * should be used.  If UserPreferences are not set explicitly,
	 * the default set of Detectors will be used.
	 * 
	 * @param userPreferences the UserPreferences
	 */
	public void setUserPreferences(UserPreferences userPreferences);

	/**
	 * Add an IClassObserver.
	 *
	 * @param classObserver the IClassObserver
	 */
	public void addClassObserver(IClassObserver classObserver);

	/**
	 * Set the ClassScreener.
	 * This object chooses which individual classes to analyze.
	 * By default, all classes are analyzed.
	 *
	 * @param classScreener the ClassScreener to use
	 */
	public void setClassScreener(ClassScreener classScreener);

	/**
	 * Set relaxed reporting mode.
	 * 
	 * @param relaxedReportingMode true if relaxed reporting mode should be enabled,
	 *                             false if not
	 */
	public void setRelaxedReportingMode(boolean relaxedReportingMode);

	/**
	 * Set whether or not training output should be emitted.
	 * 
	 * @param trainingOutputDir  directory to save training output in
	 */
	public void enableTrainingOutput(String trainingOutputDir);

	/**
	 * Set whether or not training input should be used to
	 * make the analysis more precise.
	 * 
	 * @param trainingInputDir directory to load training input from
	 */
	public void enableTrainingInput(String trainingInputDir);

	/**
	 * Set analysis feature settings.
	 * 
	 * @param settingList list of analysis feature settings
	 */
	public void setAnalysisFeatureSettings(AnalysisFeatureSetting[] settingList);

	/**
	 * @return Returns the releaseName.
	 */
	public String getReleaseName();

	/**
	 * @param releaseName The releaseName to set.
	 */
	public void setReleaseName(String releaseName);

	/**
	 * Set the filename of the source info file containing line numbers for fields
	 * and classes.
	 * 
	 * @param sourceInfoFile the source info filename
	 */
	public void setSourceInfoFile(String sourceInfoFile);

	/**
	 * Execute FindBugs on the Project.
	 * All bugs found are reported to the BugReporter object which was set
	 * when this object was constructed.
	 *
	 * @throws java.io.IOException  if an I/O exception occurs analyzing one of the files
	 * @throws InterruptedException if the thread is interrupted while conducting the analysis
	 * @throws CheckedAnalysisException if a fatal exception occurs
	 */
	public void execute() throws java.io.IOException, InterruptedException/*, CheckedAnalysisException*/;

	/**
	 * Get the name of the most recent class to be analyzed.
	 * This is useful for diagnosing an unexpected exception.
	 * Returns null if no class has been analyzed.
	 */
	public String getCurrentClass();

	/**
	 * Get the number of bug instances that were reported during analysis.
	 */
	public int getBugCount();

	/**
	 * Get the number of errors that occurred during analysis.
	 */
	public int getErrorCount();

	/**
	 * Get the number of time missing classes were reported during analysis.
	 */
	public int getMissingClassCount();

}
