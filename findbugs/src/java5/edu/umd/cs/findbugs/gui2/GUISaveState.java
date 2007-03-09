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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.gui2.MainFrame.SaveType;

/**
 * Saves all the stuff that should be saved for each run,
 * like recent projects, previous comments, the current docking layout
 * and the sort order
 * 
 * For project related things, look in ProjectSettings
 * @author Dan
 *
 */
/*GUISaveState uses the Preferences API, dont look for a file anywhere,
  there isn't one, well... there might be, but its all system dependent where it is and how its stored*/
public class GUISaveState{

	private static GUISaveState instance;
//	private static final String PREVCOMMENTS="Previous Comments";
	private static final String SORTERTABLELENGTH="Sorter Length";
	private static final String PREVCOMMENTSSIZE="Previous Comments Size";
	private static final String PREFERENCESDIRECTORY="Preference Directory";
	private static final String DOCKINGLAYOUT="Docking Layout";
	private static final String FRAME_BOUNDS="Frame Bounds";

	private static final int MAXNUMRECENTPROJECTS= 5;
	private static final int MAXNUMRECENTANALYSES= MAXNUMRECENTPROJECTS;
	private static final Sortables[] DEFAULT_COLUMN_HEADERS = new Sortables[] {
		Sortables.CATEGORY, Sortables.BUGCODE, Sortables.TYPE, Sortables.DIVIDER, Sortables.PRIORITY };

	private static final String[] RECENTPROJECTKEYS=new String[MAXNUMRECENTPROJECTS];//{"Project1","Project2","Project3","Project4","Project5"};//Make MAXNUMRECENTPROJECTS of these
	private static final String[] RECENTANALYSISKEYS=new String[MAXNUMRECENTPROJECTS];
	static
	{
		for (int x=0; x<RECENTPROJECTKEYS.length;x++)
		{
			RECENTPROJECTKEYS[x]="Project"+x;
			RECENTANALYSISKEYS[x]="Analysis"+x;
		}
	}
	private static final int MAXNUMPREVCOMMENTS= 10;
	private static final String[] COMMENTKEYS= new String[MAXNUMPREVCOMMENTS];
	static
	{
		for (int x=0; x<COMMENTKEYS.length;x++)
		{
			COMMENTKEYS[x]="Comment"+x;
		}
	}
	private static final String NUMPROJECTS= "NumberOfProjectsToLoad";
	private static final String NUMANALYSES= "NumberOfAnalysesToLoad";
	private static final String STARTERDIRECTORY= "Starter Directory";
	
	private static final String SPLIT_MAIN = "MainSplit";
	private static final String SPLIT_TREE_COMMENTS = "TreeCommentsSplit";
	private static final String SPLIT_TOP = "TopSplit";
	private static final String SPLIT_SUMMARY = "SummarySplit";
	
	private int splitMain;
	private int splitTreeComments;
	private int splitTop;
	private int splitSummary;
	
	private File starterDirectoryForLoadBugs;
	/**
	 * List of previous comments by the user.
	 */
	private LinkedList<String> previousComments;
	private boolean useDefault=false;
	private SorterTableColumnModel starterTable;
	private ArrayList<File> recentProjects;
	private ArrayList<File> recentAnalyses;
	private byte[] dockingLayout;
	private Rectangle frameBounds;
	
	private static final String TAB_SIZE = "TabSize";
	private int tabSize; //Tab size in the source code display.
	private static final String FONT_SIZE = "FontSize";
	private float fontSize; //Font size of entire GUI.
	
	public int getTabSize() {
		return tabSize;
	}

	public void setTabSize(int tabSize) {
		this.tabSize = tabSize;
	}
	
	public byte[] getDockingLayout()
	{
		return dockingLayout;
	}

	public void setDockingLayout(byte[] dockingLayout)
	{
		this.dockingLayout = dockingLayout;
	}

	private static String[] generateSorterKeys(int numSorters)
	{
		String[] result= new String[numSorters];
		for (int x=0; x< result.length;x++)
		{
			result[x]="Sorter"+x;
		}
		return result;
	}
	
	SorterTableColumnModel getStarterTable()
	{
		if (useDefault || (starterTable == null))
			starterTable=new SorterTableColumnModel(GUISaveState.DEFAULT_COLUMN_HEADERS);

		return starterTable;
	}
	
	private GUISaveState()
	{
		recentProjects=new ArrayList<File>();
		recentAnalyses=new ArrayList<File>();
//		projectsToLocations=new HashMap<String,String>();
		previousComments=new LinkedList<String>();
	}
	
	public static GUISaveState getInstance()
	{
		if (instance==null)
			instance=new GUISaveState();
		return instance;
	}
	
	public ArrayList<File> getRecent(SaveType s)
	{
		if (s==SaveType.PROJECT)
			return getRecentProjects();
		if (s==SaveType.XML_ANALYSIS)
			return getRecentAnalyses();
		else
			throw new IllegalStateException("Your file is not of any valid save type--GetRecent");
	}
	public ArrayList<File> getRecentProjects()
	{
		return recentProjects;
	}
	
	public ArrayList<File> getRecentAnalyses()
	{
		return recentAnalyses;
	}

	public void addRecentProject(File f)
	{
		addRecentProject(f, SaveType.PROJECT);
	}
	
	public void addRecentProject(File f, SaveType s)
	{
		if (s==SaveType.PROJECT)
			recentProjects.add(f);
		else if (s==SaveType.XML_ANALYSIS)
			recentAnalyses.add(f);
	}
	
	public void projectReused(File f)
	{
		projectReused(f, SaveType.PROJECT);
	}
	
	public void projectReused(File f, SaveType s)
	{
		if (s==SaveType.PROJECT)
		{
			if (!recentProjects.contains(f))
			{
				throw new IllegalStateException("Selected a recent project that doesn't exist?");
			}
			else
			{
				recentProjects.remove(f);
				recentProjects.add(f);
			}
		}
		else if (s==SaveType.XML_ANALYSIS)
		{
			if (!recentAnalyses.contains(f))
			{
				throw new IllegalStateException("Selected a recent project that doesn't exist?");
			}
			else
			{
				recentAnalyses.remove(f);
				recentAnalyses.add(f);
			}					}
	}
	
	public void projectNotFound(File f)
	{
		projectNotFound(f, SaveType.PROJECT);
	}
	
	public void projectNotFound(File f, SaveType s)
	{
		if (s==SaveType.PROJECT)
		{
			if (!recentProjects.contains(f))
			{
				throw new IllegalStateException("Well no wonder it wasn't found, its not in the list.");
			}
			else
				recentProjects.remove(f);
		}
		else if (s==SaveType.XML_ANALYSIS)
		{
			if (!recentAnalyses.contains(f))
			{
				throw new IllegalStateException("Well no wonder it wasn't found, its not in the list.");
			}
			else
				recentAnalyses.remove(f);				
		}
	}

	/**
	 * The file to start the loading of Bugs from.
	 * @return Returns the starterDirectoryForLoadBugs.
	 */
	public File getStarterDirectoryForLoadBugs() {
		return starterDirectoryForLoadBugs;
	}

	/**
	 * @param f The starterDirectoryForLoadBugs to set.
	 */
	public void setStarterDirectoryForLoadBugs(File f) {
		this.starterDirectoryForLoadBugs = f;
	}

	
	public static void loadInstance()
	{
		GUISaveState newInstance=new GUISaveState();
		newInstance.recentProjects=new ArrayList<File>();
		newInstance.recentAnalyses=new ArrayList<File>();
		Preferences p=Preferences.userNodeForPackage(GUISaveState.class);
		
		newInstance.tabSize = p.getInt(TAB_SIZE, 4);
		
		newInstance.fontSize = p.getFloat(FONT_SIZE, 12.0f);
		
		newInstance.starterDirectoryForLoadBugs=new File(p.get(GUISaveState.STARTERDIRECTORY, SystemProperties.getProperty("user.dir")));
		
		int prevCommentsSize=p.getInt(GUISaveState.PREVCOMMENTSSIZE, 0);
		
		for (int x=0;x<prevCommentsSize;x++)
		{
			String comment=p.get(GUISaveState.COMMENTKEYS[x], "");
			newInstance.previousComments.add(comment);
		}
		
		int size=Math.min(MAXNUMRECENTPROJECTS,p.getInt(GUISaveState.NUMPROJECTS,0));
		for (int x=0;x<size;x++)
		{
			newInstance.recentProjects.add(new File(p.get(GUISaveState.RECENTPROJECTKEYS[x],"")));
		}

		int size2=Math.min(MAXNUMRECENTANALYSES,p.getInt(GUISaveState.NUMANALYSES,0));
		for (int x=0;x<size2;x++)
		{
			newInstance.recentAnalyses.add(new File(p.get(GUISaveState.RECENTANALYSISKEYS[x],"")));
		}

		int sorterSize=p.getInt(GUISaveState.SORTERTABLELENGTH,-1);
		if (sorterSize!=-1)
		{
			Sortables[] sortColumns=new Sortables[sorterSize];
			String[] sortKeys=GUISaveState.generateSorterKeys(sorterSize);
			for (int x=0;x<sorterSize;x++)
			{
				sortColumns[x]=Sortables.getSortableByPrettyName(p.get(sortKeys[x], "*none*"));
				if (sortColumns[x]==null)
				{
					if (MainFrame.DEBUG) System.err.println("Sort order was corrupted, using default sort order");
					newInstance.useDefault=true;
				}
			}
			if(!newInstance.useDefault)
				newInstance.starterTable=new SorterTableColumnModel(sortColumns);
		}
		else
			newInstance.useDefault=true;

		newInstance.dockingLayout = p.getByteArray(DOCKINGLAYOUT, new byte[0]);
		
		String boundsString = p.get(FRAME_BOUNDS, null);
		Rectangle r = new Rectangle(0, 0, 800, 650);
		if (boundsString != null) {
			String[] a = boundsString.split(",", 4);
			if (a.length > 0) try {
				r.x = Math.max(0, Integer.parseInt(a[0]));
			} catch (NumberFormatException nfe) { assert true; }
			if (a.length > 1) try {
				r.y = Math.max(0, Integer.parseInt(a[1]));
			} catch (NumberFormatException nfe) { assert true; }
			if (a.length > 2) try {
				r.width = Math.max(40, Integer.parseInt(a[2]));
			} catch (NumberFormatException nfe) { assert true; }
			if (a.length > 3) try {
				r.height = Math.max(40, Integer.parseInt(a[3]));
			} catch (NumberFormatException nfe) { assert true; }
		}
		newInstance.frameBounds = r;
		
		newInstance.splitMain = p.getInt(SPLIT_MAIN, 400);
		newInstance.splitSummary = p.getInt(SPLIT_SUMMARY, 85);
		newInstance.splitTop = p.getInt(SPLIT_TOP, -1);
		newInstance.splitTreeComments = p.getInt(SPLIT_TREE_COMMENTS, 250);
		
		instance=newInstance;
	}
	
	public void save()
	{	
		Preferences p=Preferences.userNodeForPackage(GUISaveState.class);
		
		p.putInt(TAB_SIZE, tabSize);
		
		p.putFloat(FONT_SIZE, fontSize);
		
		int sorterLength=MainFrame.getInstance().getSorter().getColumnCount();
		ArrayList<Sortables> sortables=MainFrame.getInstance().getSorter().getOrder();
		p.putInt(GUISaveState.SORTERTABLELENGTH, sorterLength);
		
		String[] sorterKeys=GUISaveState.generateSorterKeys(sorterLength);
		for (int x=0; x<sorterKeys.length;x++)
		{
			p.put(sorterKeys[x], sortables.get(x).prettyName);
		}
		
		p.putInt(GUISaveState.PREVCOMMENTSSIZE, previousComments.size());
		
		for (int x=0; x<previousComments.size();x++)
		{
			String comment=previousComments.get(x);
			p.put(GUISaveState.COMMENTKEYS[x], comment);
		}
		
		int size=recentProjects.size();
		int size2=recentAnalyses.size();
		while (recentProjects.size()>MAXNUMRECENTPROJECTS)
		{
			recentProjects.remove(0);
		}
		while (recentAnalyses.size()>MAXNUMRECENTANALYSES)
		{
			recentAnalyses.remove(0);
		}
		
		p.putInt(GUISaveState.NUMPROJECTS,Math.min(size,MAXNUMRECENTPROJECTS));
		p.putInt(GUISaveState.NUMANALYSES,Math.min(size2,MAXNUMRECENTANALYSES));
		for (int x=0; x<Math.min(size,MAXNUMRECENTPROJECTS);x++)
		{
			File file=recentProjects.get(x);
			p.put(GUISaveState.RECENTPROJECTKEYS[x],file.getAbsolutePath());
		}
		for (int x=0; x<Math.min(size2,MAXNUMRECENTANALYSES);x++)
		{
			File file=recentAnalyses.get(x);
			p.put(GUISaveState.RECENTANALYSISKEYS[x],file.getAbsolutePath());			
		}
		
		p.putByteArray(DOCKINGLAYOUT, dockingLayout);
		
		p.put(FRAME_BOUNDS, frameBounds.x+","+frameBounds.y+","+frameBounds.width+","+frameBounds.height);
	
		p.putInt(SPLIT_MAIN, splitMain);
		p.putInt(SPLIT_SUMMARY, splitSummary);
		p.putInt(SPLIT_TOP, splitTop);
		p.putInt(SPLIT_TREE_COMMENTS, splitTreeComments);
	}
		
	static void clear()
	{
		Preferences p=Preferences.userNodeForPackage(GUISaveState.class);
		try {
			p.clear();
		} catch (BackingStoreException e) {
			Debug.println(e);
		}
		instance=new GUISaveState();
	}
	
	/**
	 * @return Returns the previousComments.
	 */
	public LinkedList<String> getPreviousComments() {
		return previousComments;
	}

	/**
	 * @param previousComments The previousComments to set.
	 */
	public void setPreviousComments(LinkedList<String> previousComments) {
		this.previousComments = previousComments;
	}

	/**
	 * @return Returns the frame bounds Rectangle.
	 */
	public Rectangle getFrameBounds() {
		return frameBounds;
	}

	/**
	 * @param frameBounds The frame bourds Rectangle to set.
	 */
	public void setFrameBounds(Rectangle frameBounds) {
		this.frameBounds = frameBounds;
	}

	/**
	 * @return Returns the fontSize.
	 */
	public float getFontSize() {
		return fontSize;
	}

	/**
	 * @param fontSize The fontSize to set.
	 */
	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}

	/**
	 * @return Returns the location of the main divider.
	 */
	public int getSplitMain() {
		return splitMain;
	}

	/**
	 * @param splitMain The location of the main divider to set.
	 */
	public void setSplitMain(int splitMain) {
		this.splitMain = splitMain;
	}

	/**
	 * @return Returns the location of the summary divider.
	 */
	public int getSplitSummary() {
		return splitSummary;
	}

	/**
	 * @param splitSummary The location of the summar divider to set.
	 */
	public void setSplitSummary(int splitSummary) {
		this.splitSummary = splitSummary;
	}

	/**
	 * @return Returns the location of the top divider.
	 */
	public int getSplitTop() {
		return splitTop;
	}

	/**
	 * @param splitTop The location of the top divider to set.
	 */
	public void setSplitTop(int splitTop) {
		this.splitTop = splitTop;
	}

	/**
	 * @return Returns the location of the tree-comments divider.
	 */
	public int getSplitTreeComments() {
		return splitTreeComments;
	}

	/**
	 * @param splitTreeComments The location of the tree-comments divider to set.
	 */
	public void setSplitTreeComments(int splitTreeComments) {
		this.splitTreeComments = splitTreeComments;
	}
}