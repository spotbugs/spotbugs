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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * Saves all the stuff that should be saved for each run,
 * like recent projects, previous comments, the current docking layout
 * and the sort order
 * 
 * For project related things, look in ProjectSettings
 * @author Dan
 *
 */
//GUISaveState uses the Preferences API, dont look for a file anywhere, there isn't one, well... there might be, but its all system dependent where it is and how its stored
public class GUISaveState{

	private static GUISaveState instance;
//	private static final String PREVCOMMENTS="Previous Comments";
	private static final String SORTERTABLELENGTH="Sorter Length";
	private static final String PREVCOMMENTSSIZE="Previous Comments Size";
	private static final String PREFERENCESDIRECTORY="Preference Directory";
	private static final String DOCKINGLAYOUT="Docking Layout";
	private static final int MAXNUMRECENTPROJECTS= 5;
	private static final Sortables[] DEFAULT_COLUMN_HEADERS = new Sortables[] {
		Sortables.PRIORITY, Sortables.CATEGORY, Sortables.CLASS, Sortables.DESIGNATION, Sortables.DIVIDER };

	private static final String[] RECENTPROJECTKEYS=new String[MAXNUMRECENTPROJECTS];//{"Project1","Project2","Project3","Project4","Project5"};//Make MAXNUMRECENTPROJECTS of these
	static
	{
		for (int x=0; x<RECENTPROJECTKEYS.length;x++)
		{
			RECENTPROJECTKEYS[x]="Project"+x;
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
	private static final String STARTERDIRECTORY= "Starter Directory";
	private File starterDirectoryForLoadBugs;
	/**
	 * List of previous comments by the user.
	 */
	private LinkedList<String> previousComments;
	private boolean useDefault=false;
	private SorterTableColumnModel starterTable;
	private ArrayList<File> recentProjects;
	private byte[] dockingLayout;
	
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
		if (useDefault)
			starterTable=new SorterTableColumnModel(GUISaveState.DEFAULT_COLUMN_HEADERS);

		return starterTable;
	}
	
	private GUISaveState()
	{
		recentProjects=new ArrayList<File>();
//		projectsToLocations=new HashMap<String,String>();
		previousComments=new LinkedList();
	}
	
	public static GUISaveState getInstance()
	{
		if (instance==null)
			instance=new GUISaveState();
		return instance;
	}
	
	public ArrayList<File> getRecentProjects()
	{
		return recentProjects;
	}
		
	public void addRecentProject(File f)
	{
		recentProjects.add(f);
	}
	
	public void projectReused(File f)
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
	
	public void projectNotFound(File f)
	{
		if (!recentProjects.contains(f))
		{
			throw new IllegalStateException("Well no wonder it wasn't found, its not in the list.");
		}
		else
			recentProjects.remove(f);
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
		Preferences p=Preferences.userNodeForPackage(GUISaveState.class);
		
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
			newInstance.starterTable=new SorterTableColumnModel(sortColumns);
		}
		else
			newInstance.useDefault=true;

		newInstance.dockingLayout = p.getByteArray(DOCKINGLAYOUT, new byte[0]);
		
		instance=newInstance;
	}
	
	public void save()
	{	
		Preferences p=Preferences.userNodeForPackage(GUISaveState.class);
		
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
		while (recentProjects.size()>MAXNUMRECENTPROJECTS)
		{
			recentProjects.remove(0);
		}
		p.putInt(GUISaveState.NUMPROJECTS,Math.min(size,MAXNUMRECENTPROJECTS));
		for (int x=0; x<Math.min(size,MAXNUMRECENTPROJECTS);x++)
		{
			File file=recentProjects.get(x);
			p.put(GUISaveState.RECENTPROJECTKEYS[x],file.getAbsolutePath());
		}
		
		p.putByteArray(DOCKINGLAYOUT, dockingLayout);
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
}