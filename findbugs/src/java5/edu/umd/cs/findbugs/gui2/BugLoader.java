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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.config.UserPreferences;
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
	 */
	public static BugCollection doAnalysis(@NonNull Project p, FindBugsProgress progressCallback)
	{
		BugCollectionBugReporter  pcb=new BugCollectionBugReporter(p);
		pcb.setPriorityThreshold(Priorities.NORMAL_PRIORITY);
		IFindBugsEngine fb=createEngine(p, pcb);
		fb.setUserPreferences(UserPreferences.getUserPreferences());
		fb.setProgressCallback(progressCallback);
		try {
			fb.execute();
			List<String> possibleDirectories=p.getSourceDirList();
			MainFrame instance = MainFrame.getInstance();

			//			System.out.println("List of directories: "+p.getSourceDirList());
			instance.setSourceFinder(new SourceFinder());
			instance.getSourceFinder().setSourceBaseList(possibleDirectories);

			return pcb.getBugCollection();
		} catch (IOException e) {
			Debug.println(e);
		} catch (InterruptedException e) {
			// Do nothing
		}


		return null;

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
		if (false) {
			// Old driver - don't use
			return new FindBugs(pcb, p);
		} else {
			FindBugs2 engine = new FindBugs2();
			engine.setBugReporter(pcb);
			engine.setProject(p);

			engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

			return engine;
		}
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

	public static SortedBugCollection loadBugs(MainFrame mainFrame, Project project, URL url){
		try {
			return loadBugs(mainFrame, project, url.openConnection().getInputStream());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,"This file contains no bug data");
		}	
		return null;
	}
	public static SortedBugCollection loadBugs(MainFrame mainFrame, Project project, File file){
		try {
			return loadBugs(mainFrame, project, new FileInputStream(file));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,"This file contains no bug data");
		}	
		return null;
	}
	public static SortedBugCollection loadBugs(MainFrame mainFrame, Project project, InputStream in)
	{
			try 
			{
				SortedBugCollection col=new SortedBugCollection();
				col.readXML(in, project);
				return col;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(mainFrame,"This file contains no bug data");
			} catch (DocumentException e) {
				JOptionPane.showMessageDialog(mainFrame,"This file does not have the correct format; it may be corrupt.");
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
	 * @param project TODO
	 * @return the merged collecction of bugs
	 */
	public static BugCollection combineBugHistories(Project project)
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
			conglomeration.readXML(chooser.getSelectedFiles()[0],project);
			Update update = new Update();
			for (int x=1; x<chooser.getSelectedFiles().length;x++)
			{
				File f=chooser.getSelectedFiles()[x];
				Project p=new Project();
				SortedBugCollection col=new SortedBugCollection();
				col.readXML(f,p);
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
	public static BugCollection redoAnalysisKeepComments(@NonNull Project p)
	{
		if (p == null) throw new NullPointerException("null project");

		BugSet oldSet=BugSet.getMainBugSet();
		BugCollection current=MainFrame.getInstance().bugCollection;//Now we should no longer get this December 31st 1969 business.
		for (BugLeafNode node: oldSet)
		{
			BugInstance bug=node.getBug();
			current.add(bug);
		}
		Update update = new Update();

		RedoAnalysisCallback ac= new RedoAnalysisCallback();

		new AnalyzingDialog(p,ac,true);

		if (ac.finished)
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
