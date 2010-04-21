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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.workflow.Update;

/**
 * Everything having to do with loading bugs should end up here.
 * @author Dan
 *
 */
public class BugLoader {

	/**
	 * Performs an analysis and returns the BugSet created
	 * 
	 * @param p The Project to run the analysis on
	 * @param progressCallback the progressCallBack is supposed to be supplied by analyzing dialog, FindBugs supplies progress information while it runs the analysis
	 * @return the bugs found 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static BugCollection doAnalysis(@NonNull Project p, FindBugsProgress progressCallback) throws IOException, InterruptedException
	{
		BugCollectionBugReporter  pcb=new BugCollectionBugReporter(p);
		pcb.setPriorityThreshold(Priorities.NORMAL_PRIORITY);
		IFindBugsEngine fb=createEngine(p, pcb);
		fb.setUserPreferences(UserPreferences.getUserPreferences());
		fb.setProgressCallback(progressCallback);
		fb.setProjectName(p.getProjectName());

		fb.execute();
		
		return pcb.getBugCollection();
	}

	/**
	 * Create the IFindBugsEngine that will be used to
	 * analyze the application.
	 * 
	 * @param p   the Project
	 * @param pcb the PrintCallBack
	 * @return the IFindBugsEngine
	 */
	private static IFindBugsEngine createEngine(@NonNull Project p, BugReporter pcb)
	{
		FindBugs2 engine = new FindBugs2();
		engine.setBugReporter(pcb);
		engine.setProject(p);

		engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
		
		//
		// Honor -effort option if one was given on the command line.
		//
		engine.setAnalysisFeatureSettings(Driver.getAnalysisSettingList());

		return engine;
	}

	public static BugSet loadBugsHelper(BugCollection collection)
	{
		ArrayList<BugLeafNode> bugList=new ArrayList<BugLeafNode>();
		Iterator<BugInstance> i=collection.iterator();
		while (i.hasNext())
		{
			bugList.add(new BugLeafNode(i.next()));
		}

		return new BugSet(bugList);

	}



	public static @CheckForNull SortedBugCollection loadBugs(MainFrame mainFrame, Project project, File source) {
		if (!source.isFile() || !source.canRead()) {
			JOptionPane.showMessageDialog(mainFrame,"Unable to read " + source);
			return null;
		}
		SortedBugCollection col=new SortedBugCollection(project);
		col.setRequestDatabaseCloud(true);
		try {
	        col.readXML(source);
	        initiateCommunication(col);
        } catch (Exception e) {
        	e.printStackTrace();
        	JOptionPane.showMessageDialog(mainFrame,"Could not read " +  source+ "; " + e.getMessage());
        }
		addDeadBugMatcher(project);
		return col;
	}

	/**
     * @param col
     */
    private static void initiateCommunication(SortedBugCollection col) {
	    Cloud cloud = col.getCloud();
	    if (cloud != null)
	    	cloud.initiateCommunication();
    }
	
	public static @CheckForNull SortedBugCollection loadBugs(MainFrame mainFrame, Project project, URL url) {
		
		SortedBugCollection col=new SortedBugCollection(project);
		col.setRequestDatabaseCloud(true);
		try {
	        col.readXML(url);
	        initiateCommunication(col);
        } catch (Exception e) {
        	String msg = SystemProperties.getOSDependentProperty("findbugs.unableToLoadViaURL");
        	if (msg == null)
        		msg = e.getMessage();
        	else 
        		msg = String.format(msg);
        	JOptionPane.showMessageDialog(mainFrame,"Could not read " +  url + "\n" +  msg);
        	if (SystemProperties.getBoolean("findbugs.failIfUnableToLoadViaURL"))
        		System.exit(1);
        }
		addDeadBugMatcher(project);
		return col;
	}
		
	static void addDeadBugMatcher(Project p) {
		Filter suppressionMatcher = p.getSuppressionFilter();
		if (suppressionMatcher != null) {
			suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
		}
	}
	

	public static @CheckForNull Project loadProject(MainFrame mainFrame,  File f)
	{
		try 
		{
			Project project = Project.readXML(f); 

			Filter suppressionMatcher = project.getSuppressionFilter();
			if (suppressionMatcher != null) {
				suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
			}
			project.setGuiCallback(mainFrame);
			return project;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(mainFrame,"Could not read " + f + "; " + e.getMessage());
		} catch (DocumentException e) {
			JOptionPane.showMessageDialog(mainFrame, "Could not read project from " + f + "; " + e.getMessage());
		} catch (SAXException e) {
			JOptionPane.showMessageDialog(mainFrame, "Could not read  project from " + f + "; " + e.getMessage());
		}
		return null;
	}

	private BugLoader()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * TODO: This really needs to be rewritten such that they don't have to choose ALL xmls in one fel swoop.  I'm thinking something more like new project wizard's functionality. -Dan
	 * 
	 * Merges bug collection histories from xmls selected by the user.  Right now all xmls must be in the same folder and he must select all of them at once
	 * Makes use of FindBugs's mergeCollection method in the Update class of the workflow package
	 * @return the merged collecction of bugs
	 */
	public static BugCollection combineBugHistories()
	{
		try
		{
			FBFileChooser chooser=new FBFileChooser();
			chooser.setFileFilter(new FindBugsAnalysisFileFilter());
//			chooser.setCurrentDirectory(GUISaveState.getInstance().getStarterDirectoryForLoadBugs());  This is done by FBFileChooser.
			chooser.setMultiSelectionEnabled(true);
			chooser.setDialogTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.choose_xmls_ttl", "Choose All XML's To Combine"));
			if (chooser.showOpenDialog(MainFrame.getInstance())==JFileChooser.CANCEL_OPTION)
				return null;

			SortedBugCollection conglomeration=new SortedBugCollection();
			conglomeration.readXML(chooser.getSelectedFiles()[0]);
			Update update = new Update();
			for (int x=1; x<chooser.getSelectedFiles().length;x++)
			{
				File f=chooser.getSelectedFiles()[x];
				SortedBugCollection col=new SortedBugCollection();
				col.readXML(f);
				conglomeration=(SortedBugCollection) update.mergeCollections(conglomeration, col, false, false);//False means dont show dead bugs
			}

			return conglomeration;
		} catch (IOException e) {
			Debug.println(e);
			return null;
		} catch (DocumentException e) {
			Debug.println(e);
			return null;
		}

	}

	/**
	 * Does what it says it does, hit apple r (control r on pc) and the analysis is redone using the current project
	 * @param p
	 * @return the bugs from the reanalysis, or null if cancelled
	 */
	public static @CheckForNull BugCollection doAnalysis(@NonNull Project p)
	{
		if (p == null) 
			throw new NullPointerException("null project");

		RedoAnalysisCallback ac= new RedoAnalysisCallback();

		new AnalyzingDialog(p,ac,true);

		if (ac.finished)
			return  ac.getBugCollection();
		else
			return null;

	}

	/**
	 * Does what it says it does, hit apple r (control r on pc) and the analysis is redone using the current project
	 * @param p
	 * @return the bugs from the reanalysis, or null if canceled
	 */
	public static @CheckForNull  BugCollection redoAnalysisKeepComments(@NonNull Project p)
	{
		if (p == null) 
			throw new NullPointerException("null project");

		BugSet oldSet=BugSet.getMainBugSet();
		BugCollection current=MainFrame.getInstance().bugCollection;//Now we should no longer get this December 31st 1969 business.
		// Sourceforge bug 1800962 indicates 'current' can be null here
		if(current != null) for (BugLeafNode node: oldSet)
		{
			BugInstance bug=node.getBug();
			current.add(bug);
		}
		Update update = new Update();

		RedoAnalysisCallback ac= new RedoAnalysisCallback();

		new AnalyzingDialog(p,ac,true);

		if(current == null)
			return ac.getBugCollection();
		else if (ac.finished)
			return update.mergeCollections(current, ac.getBugCollection(), true, false);
		else
			return null;

	}

	/** just used to know how the new analysis went*/
	private static class RedoAnalysisCallback implements AnalysisCallback
	{

		BugCollection getBugCollection() {
			return justAnalyzed;      
		}
		BugCollection justAnalyzed;
		volatile boolean finished;
		public void analysisFinished(BugCollection b)
		{
			justAnalyzed = b;
			finished = true;
		}

		public void analysisInterrupted() 
		{
			finished=false;
		}
	}
}
