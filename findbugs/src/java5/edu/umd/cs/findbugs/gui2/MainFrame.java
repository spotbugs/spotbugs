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

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugsDisplayFeatures;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.CloudListener;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.BugTreeModel.TreeModification;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import edu.umd.cs.findbugs.sourceViewer.NavigableTextPane;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Multiset;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;             

@SuppressWarnings("serial")

/*
 * This is where it all happens... seriously... all of it...
 * All the menus are set up, all the listeners, all the frames, dockable window functionality
 * There is no one style used, no one naming convention, its all just kinda here.  This is another one of those 
 * classes where no one knows quite why it works.
 * <p>
 * The MainFrame is just that, the main application window where just about everything happens.
 */
public class MainFrame extends FBFrame implements LogSync, IGuiCallback
{
	 private AbstractExecutorService bugUpdateExecutor = new EventQueueExecutor();

    static JButton newButton(String key, String name) {
		JButton b = new JButton();
		edu.umd.cs.findbugs.L10N.localiseButton(b, key, name, false);
		return b;
	}
	static JMenuItem newJMenuItem(String key, String string, int vkF) {
		JMenuItem m = new JMenuItem();
		edu.umd.cs.findbugs.L10N.localiseButton(m, key, string, false);
		m.setMnemonic(vkF);
		return m;

	}
	static JMenuItem newJMenuItem(String key, String string) {
		JMenuItem m = new JMenuItem();
		edu.umd.cs.findbugs.L10N.localiseButton(m, key, string, true);
		return m;

	}
	static JMenu newJMenu(String key, String string) {
		JMenu m = new JMenu();
		edu.umd.cs.findbugs.L10N.localiseButton(m, key, string, true);
		return m;
	}
	
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(MainFrame.class.getName());

	JTree tree;
	private BasicTreeUI treeUI;
	boolean userInputEnabled;

	static boolean isMacLookAndFeel() {
		return UIManager.getLookAndFeel().getClass().getName().startsWith("apple");
	}

	static final String DEFAULT_SOURCE_CODE_MSG = edu.umd.cs.findbugs.L10N.getLocalString("msg.nosource_txt", "No available source");

	static final int COMMENTS_TAB_STRUT_SIZE = 5;
	static final int COMMENTS_MARGIN = 5;
	static final int SEARCH_TEXT_FIELD_SIZE = 32;

	static final String TITLE_START_TXT = "FindBugs: ";

	private JTextField sourceSearchTextField = new JTextField(SEARCH_TEXT_FIELD_SIZE);
	private JButton findButton = newButton("button.find", "First");
	private JButton findNextButton = newButton("button.findNext", "Next");
	private JButton findPreviousButton = newButton("button.findPrev", "Previous");

	public static final boolean DEBUG = SystemProperties.getBoolean("gui2.debug");

	static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");
	final static String WINDOW_MODIFIED = "windowModified";

	NavigableTextPane sourceCodeTextPane = new NavigableTextPane();
	private JScrollPane sourceCodeScrollPane;

	final CommentsArea comments;
	private SorterTableColumnModel sorter;
	private JTableHeader tableheader;
    
	private JLabel statusBarLabel = new JLabel();
    private JLabel signedInLabel;
    private ImageIcon signedInIcon;
    private ImageIcon warningIcon;

	private JPanel summaryTopPanel;
	private final HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
	private final JEditorPane summaryHtmlArea = new JEditorPane();
	private JScrollPane summaryHtmlScrollPane = new JScrollPane(summaryHtmlArea);

	final private FindBugsLayoutManagerFactory findBugsLayoutManagerFactory;
	final private FindBugsLayoutManager guiLayout;

	/* To change this value must use setProjectChanged(boolean b).
	 * This is because saveProjectItemMenu is dependent on it for when
	 * saveProjectMenuItem should be enabled.
	 */
	boolean projectChanged = false;
	final private JMenuItem reconfigMenuItem = newJMenuItem("menu.reconfig", "Reconfigure...", KeyEvent.VK_F);
	private JMenuItem redoAnalysis;
	BugLeafNode currentSelectedBugLeaf;
	BugAspects currentSelectedBugAspects;
	private JPopupMenu bugPopupMenu;
	private JPopupMenu branchPopupMenu;
	private static MainFrame instance;
	private RecentMenu recentMenuCache;
	private JMenu recentMenu;
	private JMenuItem preferencesMenuItem;
	private Project curProject = new Project();
	private JScrollPane treeScrollPane;
	private JPanel treePanel;
	private Object lock = new Object();
	private boolean newProject = false;
	final ProjectPackagePrefixes projectPackagePrefixes = new ProjectPackagePrefixes();
	
	final CountDownLatch mainFrameInitialized = new CountDownLatch(1);
	
	
	private Class<?> osxAdapter;
	private Method osxPrefsEnableMethod;

	private Logger logger = new ConsoleLogger(this);
	SourceCodeDisplay displayer = new SourceCodeDisplay(this);

	private SaveType saveType = SaveType.NOT_KNOWN;
	FBFileChooser saveOpenFileChooser;
	FBFileChooser filterOpenFileChooser;
	
	@CheckForNull private File saveFile = null;

    enum SaveReturn {SAVE_SUCCESSFUL, SAVE_IO_EXCEPTION, SAVE_ERROR};
	JMenuItem saveMenuItem = newJMenuItem("menu.save_item", "Save", KeyEvent.VK_S);

	public static void makeInstance(FindBugsLayoutManagerFactory factory) {
		if (instance != null) 
			throw new IllegalStateException();
		instance=new MainFrame(factory);
		instance.initializeGUI();
	}

	public static MainFrame getInstance() {
		if (instance==null) throw new IllegalStateException();
		return instance;
	}
	
	public static boolean isAvailable() {
		return instance != null;
	}


	private void initializeGUI() {
		SwingUtilities.invokeLater(new InitializeGUI());
	}
	private MainFrame(FindBugsLayoutManagerFactory factory)
	{
		this.findBugsLayoutManagerFactory = factory;
		this.guiLayout = factory.getInstance(this);
		this.comments = new CommentsArea(this);
		FindBugsDisplayFeatures.setAbridgedMessages(true);
		
	}

	/**
	 * Show About
	 */
	void about() {
		AboutDialog dialog = new AboutDialog(this, logger, true);
		dialog.setSize(600, 554);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	/**
	 * Show Preferences
	 */
	void preferences() {
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
		PreferencesFrame.getInstance().setLocationRelativeTo(MainFrame.this);
		PreferencesFrame.getInstance().setVisible(true);
	}

	/**
	 * enable/disable preferences menu
	 */
	void enablePreferences(boolean b) {
		preferencesMenuItem.setEnabled(b);
		if (MAC_OS_X) {
			if (osxPrefsEnableMethod != null) {
				Object args[] = {Boolean.valueOf(b)};
				try {
					osxPrefsEnableMethod.invoke(osxAdapter, args);
				}
				catch (Exception e) {
					System.err.println("Exception while enabling Preferences menu: " + e);
				}
			} 
		}
	}


	/**
	 * This method is called when the application is closing. This is either by
	 * the exit menuItem or by clicking on the window's system menu.
	 */
	void callOnClose(){
		comments.saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
		
		if(projectChanged && !SystemProperties.getBoolean("findbugs.skipSaveChangesWarning")){
			int value = JOptionPane.showConfirmDialog(MainFrame.this, getActionWithoutSavingMsg("closing"), 
					edu.umd.cs.findbugs.L10N.getLocalString("msg.confirm_save_txt", "Do you want to save?"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if(value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION)
				return ;
			else if(value == JOptionPane.YES_OPTION){

				if(saveFile == null){
					if(!saveAs())
						return;
				}
				else
					save();
			}				
		}

		GUISaveState.getInstance().setPreviousComments(comments.prevCommentsList);
		guiLayout.saveState();
		GUISaveState.getInstance().setFrameBounds( getBounds() );
		GUISaveState.getInstance().save();
		if (this.bugCollection != null) {
			Cloud cloud = this.bugCollection.getCloud();
			if (cloud != null)
				cloud.shutdown();
		}
		System.exit(0);
	}
	/**
     * @return
     */
    private String getActionWithoutSavingMsg(String action) {
    	String msg = edu.umd.cs.findbugs.L10N.getLocalString("msg.you_are_"+action+"_without_saving_txt", null);
    	if (msg != null) return msg;
	    return edu.umd.cs.findbugs.L10N.getLocalString("msg.you_are_"+action+"_txt", "You are "+action) + " " +
	    		edu.umd.cs.findbugs.L10N.getLocalString("msg.without_saving_txt", "without saving. Do you want to save?");
    }

	/*
	 * A lot of if(false) here is for switching from special cases based on localSaveType
	 * to depending on the SaveType.forFile(f) method. Can delete when sure works.
	 */
	JMenuItem createRecentItem(final File f, final SaveType localSaveType)
	{
		if (DEBUG) System.out.println("createRecentItem("+f+", "+localSaveType +")");
		String name = f.getName();

		final JMenuItem item=new JMenuItem(name);
		item.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					setCursor(new Cursor(Cursor.WAIT_CURSOR));

					if (!f.exists())
					{
						JOptionPane.showMessageDialog(null,edu.umd.cs.findbugs.L10N.getLocalString("msg.proj_not_found", "This project can no longer be found"));
						GUISaveState.getInstance().fileNotFound(f);
						return;
					}
					GUISaveState.getInstance().fileReused(f);//Move to front in GUISaveState, so it will be last thing to be removed from the list

					MainFrame.this.recentMenuCache.addRecentFile(f);

					if (!f.exists())
						throw new IllegalStateException ("User used a recent projects menu item that didn't exist.");

					//Moved this outside of the thread, and above the line saveFile=f.getParentFile()
					//Since if this save goes on in the thread below, there is no way to stop the save from
					//overwriting the files we are about to load.
					if (curProject != null && projectChanged)
					{
						int response = JOptionPane.showConfirmDialog(MainFrame.this, 
								edu.umd.cs.findbugs.L10N.getLocalString("dlg.save_current_changes", "The current project has been changed, Save current changes?")
								,edu.umd.cs.findbugs.L10N.getLocalString("dlg.save_changes", "Save Changes?"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

						if (response == JOptionPane.YES_OPTION)
						{
							if(saveFile != null)
								save();
							else
								saveAs();
						}
						else if (response == JOptionPane.CANCEL_OPTION)
							return;
						//IF no, do nothing.
					}

					SaveType st = SaveType.forFile(f);
					boolean result = true;
					switch(st){
					case PROJECT:
						openProject(f);
						break;
					case XML_ANALYSIS:
						result = openAnalysis(f, st);
						break;
					case FBP_FILE:
						result = openFBPFile(f);
						break;
					case FBA_FILE:
						result = openFBAFile(f);
						break;
					default:
						error("Wrong file type in recent menu item.");
					}
					
					if(!result){
						JOptionPane.showMessageDialog(MainFrame.getInstance(),
								"There was an error in opening the file", "Recent Menu Opening Error",
								JOptionPane.WARNING_MESSAGE);
					}
				}
				finally
				{
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					setSaveType(localSaveType);
				}
			}
		});
		item.setFont(item.getFont().deriveFont(Driver.getFontSize()));
		return item;
	}

	BugCollection bugCollection;
	
	CloudListener userAnnotationListener = new CloudListener() {

		public void issueUpdated(BugInstance bug) {
			if (currentSelectedBugLeaf != null
					&& currentSelectedBugLeaf.getBug() == bug)
				comments.updateCommentsFromLeafInformation(currentSelectedBugLeaf);
		}

		public void statusUpdated() {
			SwingUtilities.invokeLater(updateStatusBarRunner);
		}
	};
    private Cloud.CloudStatusListener cloudStatusListener = new Cloud.CloudStatusListener() {
        public void handleIssueDataDownloadedEvent() {
            rebuildBugTreeIfSortablesDependOnCloud();
        }

        public void handleStateChange(SigninState oldState, SigninState state) {
            rebuildBugTreeIfSortablesDependOnCloud();
        }
    };
	
	public void registerCloud(Project project, BugCollection collection, Cloud plugin) {
		assert collection.getCloud() == plugin;
		if (this.bugCollection == collection) {
			plugin.addListener(userAnnotationListener);
            plugin.addStatusListener(cloudStatusListener);
		}
		// Don't think we need to do this
        setProjectAndBugCollectionInSwingThread(project, collection);
    }
	public void unregisterCloud(Project project, BugCollection collection, Cloud plugin) {
		assert collection.getCloud() == plugin;
		if (this.bugCollection == collection) {
			plugin.removeListener(userAnnotationListener);
            plugin.removeStatusListener(cloudStatusListener);
		}
		// Don't think we need to do this
        setProjectAndBugCollectionInSwingThread(project, collection);
    }

    private void rebuildBugTreeIfSortablesDependOnCloud() {
        BugTreeModel bt=(BugTreeModel) (this.getTree().getModel());
        List<Sortables> sortables = sorter.getOrderBeforeDivider();
        if (sortables.contains(Sortables.DESIGNATION)
            || sortables.contains(Sortables.FIRST_SEEN)
            || sortables.contains(Sortables.FIRSTVERSION)
            || sortables.contains(Sortables.LASTVERSION)) {

            bt.rebuild();
        }
    }

    public ExecutorService getBugUpdateExecutor() {
        return bugUpdateExecutor;
    }

    @SwingThread
	void setProjectWithNoBugCollection(Project project) {
		setProjectAndBugCollection(project, null);
	}

	@SwingThread
	void setBugCollection(BugCollection bugCollection) {
		setProjectAndBugCollection(bugCollection.getProject(), bugCollection);
	}

	@SwingThread
	private void setProjectAndBugCollection(Project project, @CheckForNull BugCollection bugCollection) {
		Filter suppressionMatcher = project.getSuppressionFilter();
		if (suppressionMatcher != null) {
			suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
		}
		if (this.bugCollection != bugCollection && this.bugCollection != null) {
        	
        	Cloud plugin = this.bugCollection.getCloud();
        	if (plugin != null)  {
        		plugin.removeListener(userAnnotationListener);
                plugin.removeStatusListener(cloudStatusListener);
        		plugin.shutdown();
        	}
        	
        }
		// setRebuilding(false);
		if (bugCollection != null) {
			showTreeCard();
			setProject(project);
			this.bugCollection = bugCollection;
			bugCollection.setRequestDatabaseCloud(true);
			displayer.clearCache();
			Cloud plugin = bugCollection.getCloud();
			if (plugin != null) {
				plugin.addListener(userAnnotationListener);
                plugin.addStatusListener(cloudStatusListener);
			}
			updateBugTree();
		}
		setProjectChanged(false);
		Runnable runnable = new Runnable() {
	    	public void run() {
	    		PreferencesFrame.getInstance().updateFilterPanel();
	    		reconfigMenuItem.setEnabled(true);
	    		comments.configureForCurrentCloud();
	    		setViewMenu();
	    		newProject();
	    		clearSourcePane();
	    		clearSummaryTab();

	    		/* This is here due to a threading issue. It can only be called after
	    		 * curProject has been changed. Since this method is called by both open methods
	    		 * it is put here.*/
	    		changeTitle();
	    	}};
    	if (SwingUtilities.isEventDispatchThread()) 
    		runnable.run();
    	else
    		SwingUtilities.invokeLater(runnable);
		
		
	}
	private void updateBugTree() {
	    BugTreeModel model = (BugTreeModel) getTree().getModel();
        if (bugCollection != null) {
            BugSet bs = new BugSet(bugCollection);
            model.getOffListenerList();
            model.changeSet(bs);
            if (bs.size() == 0 && bs.sizeUnfiltered() > 0) {
                warnUserOfFilters();
            }
        }

        updateStatusBar();
        changeTitle();
    }

	void resetViewCache() {
		 ((BugTreeModel) getTree().getModel()).clearViewCache();
	}
	final Runnable updateStatusBarRunner = new Runnable() {

		public void run() {
	        updateStatusBar();
	        
        }};
	void updateProjectAndBugCollection(Project project, BugCollection bugCollection, BugTreeModel previousModel) {
		setRebuilding(false);
		if (bugCollection != null) {
			displayer.clearCache();
			BugSet bs = new BugSet(bugCollection);
			//Dont clear data, the data's correct, just get the tree off the listener lists.
			BugTreeModel model = (BugTreeModel) tree.getModel();
			model.getOffListenerList();
			model.changeSet(bs);
			//curProject=BugLoader.getLoadedProject();
			setProjectChanged(true);
		}
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Changes the title based on curProject and saveFile.
	 *
	 */	
	public void changeTitle(){
		Project project = getProject();
		String name = project == null ? null : project.getProjectName();
		if(name == null && saveFile != null)
			name = saveFile.getAbsolutePath();
		if(name == null)
			name = Project.UNNAMED_PROJECT;
		String oldTitle = MainFrame.this.getTitle();
		String newTitle = TITLE_START_TXT + name;
		if (oldTitle.equals(newTitle))
			return;
		MainFrame.this.setTitle(newTitle);
	}

	/**
	 * Creates popup menu for bugs on tree.
	 * @return
	 */
	private JPopupMenu createBugPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();

		
		JMenuItem filterMenuItem = newJMenuItem("menu.filterBugsLikeThis", "Filter bugs like this");

		filterMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
				new NewFilterFromBug(currentSelectedBugLeaf.getBug());

				setProjectChanged(true);
				MainFrame.getInstance().getTree().setSelectionRow(0);//Selects the top of the Jtree so the CommentsArea syncs up.
			}
		});

		popupMenu.add(filterMenuItem);

		JMenu changeDesignationMenu = newJMenu("menu.changeDesignation", "Change bug designation");

		int i = 0;
		int keyEvents [] = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
		for(String key :  I18N.instance().getUserDesignationKeys(true)) {
			String name = I18N.instance().getUserDesignation(key);
			comments.addDesignationItem(changeDesignationMenu, name, keyEvents[i++]);
		}

		popupMenu.add(changeDesignationMenu);

		return popupMenu;
	}

	boolean shouldDisplayIssueIgnoringPackagePrefixes(BugInstance b) {
		Project project = getProject();
		Filter suppressionFilter = project.getSuppressionFilter();
		if (null == bugCollection || suppressionFilter.match(b))
			return false;
		return viewFilter.showIgnoringPackagePrefixes(b);
		}

	boolean shouldDisplayIssue(BugInstance b) {
		Project project = getProject();
		Filter suppressionFilter = project.getSuppressionFilter();
		if (null == bugCollection || suppressionFilter.match(b))
			return false;
		return viewFilter.show(b);
		}
	/**
	 * Creates the branch pop up menu that ask if the user wants 
	 * to hide all the bugs in that branch.
	 * @return
	 */
	private JPopupMenu createBranchPopUpMenu(){
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem filterMenuItem = newJMenuItem("menu.filterTheseBugs", "Filter these bugs");

		filterMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				//TODO This code does a smarter version of filtering that is only possible for branches, and does so correctly
				//However, it is still somewhat of a hack, because if we ever add more tree listeners than simply the bugtreemodel,
				//They will not be called by this code.  Using FilterActivity to notify all listeners will however destroy any
				//benefit of using the smarter deletion method.
				
				try {
				saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
				int startCount;
				TreePath path=MainFrame.getInstance().getTree().getSelectionPath();
				TreePath deletePath=path;
				startCount=((BugAspects)(path.getLastPathComponent())).getCount();
				int count=((BugAspects)(path.getParentPath().getLastPathComponent())).getCount();
				while(count==startCount)
				{
					deletePath=deletePath.getParentPath();
					if (deletePath.getParentPath()==null)//We are at the top of the tree, don't let this be removed, rebuild tree from root.
					{
						Matcher m = currentSelectedBugAspects.getMatcher();
						Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
						suppressionFilter.addChild(m);
						PreferencesFrame.getInstance().updateFilterPanel();
						FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
						return;
					}
					count=((BugAspects)(deletePath.getParentPath().getLastPathComponent())).getCount();
				} 
/*				 
				deletePath should now be a path to the highest 
				ancestor branch with the same number of elements
				as the branch to be deleted
				in other words, the branch that we actually have 
				to remove in order to correctly remove the selected branch.
*/
				BugTreeModel model=MainFrame.getInstance().getBugTreeModel();
				TreeModelEvent event=new TreeModelEvent(this,deletePath.getParentPath(),
					new int[]{model.getIndexOfChild(deletePath.getParentPath().getLastPathComponent(),deletePath.getLastPathComponent())},
					new Object[]{deletePath.getLastPathComponent()});
				Matcher m = currentSelectedBugAspects.getMatcher();
				Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
				suppressionFilter.addChild(m);
				PreferencesFrame.getInstance().updateFilterPanel();
				model.sendEvent(event, TreeModification.REMOVE);
//				FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
				
				setProjectChanged(true);
				
				MainFrame.getInstance().getTree().setSelectionRow(0);//Selects the top of the Jtree so the CommentsArea syncs up.
				} catch (RuntimeException e) {
					MainFrame.getInstance().showMessageDialog("Unable to create filter: " + e.getMessage());
				}
			}
		});

		popupMenu.add(filterMenuItem);

		JMenu changeDesignationMenu = newJMenu("menu.changeDesignation", "Change bug designation");

		int i = 0;
		int keyEvents [] = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
		for(String key :  I18N.instance().getUserDesignationKeys(true)) {
			String name = I18N.instance().getUserDesignation(key);
			addDesignationItem(changeDesignationMenu, name, keyEvents[i++]);
		}

		popupMenu.add(changeDesignationMenu);

		return popupMenu;
	}

	/**
	 * Creates the MainFrame's menu bar.
	 * @return the menu bar for the MainFrame
	 */
	protected JMenuBar createMainMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		//Create JMenus for menuBar.
		JMenu fileMenu = newJMenu("menu.file_menu", "File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenu editMenu = newJMenu("menu.edit_menu", "Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		//Edit fileMenu JMenu object.
		JMenuItem openMenuItem = newJMenuItem("menu.open_item", "Open...", KeyEvent.VK_O);
		recentMenu = newJMenu("menu.recent", "Recent");
		recentMenuCache=new RecentMenu(recentMenu);
		JMenuItem saveAsMenuItem = newJMenuItem("menu.saveas_item", "Save As...", KeyEvent.VK_A);
		JMenuItem importFilter = newJMenuItem("menu.importFilter_item", "Import filter...");
		JMenuItem exportFilter = newJMenuItem("menu.exportFilter_item", "Export filter...");
		
		JMenuItem exitMenuItem = null;
		if (!MAC_OS_X) {
			exitMenuItem = newJMenuItem("menu.exit", "Exit", KeyEvent.VK_X);
			exitMenuItem.addActionListener(new ActionListener(){			
			public void actionPerformed(ActionEvent evt){
				callOnClose();
			}
			});
		}
		JMenu windowMenu = guiLayout.createWindowMenu();

		JMenuItem newProjectMenuItem  = null;
		if (!FindBugs.noAnalysis) {
			newProjectMenuItem = newJMenuItem("menu.new_item", "New Project", KeyEvent.VK_N);

			attachAcceleratorKey(newProjectMenuItem, KeyEvent.VK_N);

			newProjectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					newProjectMenu();
				}
			});
		}

		reconfigMenuItem.setEnabled(false);
		attachAcceleratorKey(reconfigMenuItem, KeyEvent.VK_F);
		reconfigMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
				new NewProjectWizard(curProject);
			}
		});

		JMenuItem mergeMenuItem = newJMenuItem("menu.mergeAnalysis", "Merge Analysis...");

		mergeMenuItem.setEnabled(true);
		mergeMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				mergeAnalysis();
			}
		});

		if (!FindBugs.noAnalysis) {
		redoAnalysis = newJMenuItem("menu.rerunAnalysis", "Redo Analysis", KeyEvent.VK_R);
		
		redoAnalysis.setEnabled(false);
		attachAcceleratorKey(redoAnalysis, KeyEvent.VK_R);
		redoAnalysis.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				redoAnalysis();
			}
		});
		}

		openMenuItem.setEnabled(true);
		attachAcceleratorKey(openMenuItem, KeyEvent.VK_O);
		openMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				open();
			}
		});

		saveAsMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				saveAs();
			}
		});
		exportFilter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				exportFilter();
			}
		});
		importFilter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				importFilter();
			}
		});
		saveMenuItem.setEnabled(false);
		attachAcceleratorKey(saveMenuItem, KeyEvent.VK_S);
		saveMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				save();
			}
		});

		if (!FindBugs.noAnalysis)
			fileMenu.add(newProjectMenuItem);
		fileMenu.add(reconfigMenuItem);
		fileMenu.addSeparator();

		fileMenu.add(openMenuItem);
		fileMenu.add(recentMenu);
		fileMenu.addSeparator();
		fileMenu.add(importFilter);
		fileMenu.add(exportFilter);
		fileMenu.addSeparator();
		fileMenu.add(saveAsMenuItem);
		fileMenu.add(saveMenuItem);

		if (!FindBugs.noAnalysis) {	
			fileMenu.addSeparator();
			fileMenu.add(redoAnalysis);
			}
		// fileMenu.add(mergeMenuItem);

		if (exitMenuItem != null) {
			fileMenu.addSeparator();
			fileMenu.add(exitMenuItem);
		}

		menuBar.add(fileMenu);

		//Edit editMenu Menu object.
		JMenuItem cutMenuItem = new JMenuItem(new CutAction());
		JMenuItem copyMenuItem = new JMenuItem(new CopyAction());
		JMenuItem pasteMenuItem = new JMenuItem(new PasteAction());
		preferencesMenuItem = newJMenuItem("menu.preferences_menu", "Filters/Suppressions...");
		JMenuItem sortMenuItem = newJMenuItem("menu.sortConfiguration", "Sort Configuration...");
		JMenuItem goToLineMenuItem = newJMenuItem("menu.gotoLine", "Go to line...");

		attachAcceleratorKey(cutMenuItem, KeyEvent.VK_X);
		attachAcceleratorKey(copyMenuItem, KeyEvent.VK_C);
		attachAcceleratorKey(pasteMenuItem, KeyEvent.VK_V);

		preferencesMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				preferences();
			}
		});

		sortMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
				SorterDialog.getInstance().setLocationRelativeTo(MainFrame.this);
				SorterDialog.getInstance().setVisible(true);
			}
		});

		attachAcceleratorKey(goToLineMenuItem, KeyEvent.VK_L);
		goToLineMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){				
				guiLayout.makeSourceVisible();
				try{
					int num = Integer.parseInt(JOptionPane.showInputDialog(MainFrame.this, "", edu.umd.cs.findbugs.L10N.getLocalString("dlg.go_to_line_lbl", "Go To Line") + ":", JOptionPane.QUESTION_MESSAGE));
					displayer.showLine(num);
				}
				catch(NumberFormatException e){}
			}});

		editMenu.add(cutMenuItem);
		editMenu.add(copyMenuItem);
		editMenu.add(pasteMenuItem);
		editMenu.addSeparator();
		editMenu.add(goToLineMenuItem);
		editMenu.addSeparator();
		//editMenu.add(selectAllMenuItem);
//		editMenu.addSeparator();
		if (!MAC_OS_X) {
			// Preferences goes in Findbugs menu and is handled by OSXAdapter
			editMenu.add(preferencesMenuItem);
		}
		editMenu.add(sortMenuItem);

		menuBar.add(editMenu);

		if (windowMenu != null)
			menuBar.add(windowMenu);
		
		viewMenu = newJMenu("menu.view", "View");
		setViewMenu();
		menuBar.add(viewMenu);

		final ActionMap map = tree.getActionMap();

		JMenu navMenu = newJMenu("menu.navigation", "Navigation");

		addNavItem(map, navMenu, "menu.expand", "Expand", "expand", KeyEvent.VK_RIGHT );
		addNavItem(map, navMenu, "menu.collapse", "Collapse", "collapse", KeyEvent.VK_LEFT);
		addNavItem(map, navMenu, "menu.up", "Up", "selectPrevious", KeyEvent.VK_UP );
		addNavItem(map, navMenu, "menu.down", "Down", "selectNext", KeyEvent.VK_DOWN);

		menuBar.add(navMenu);

				
		JMenu designationMenu = newJMenu("menu.designation", "Designation");
		int i = 0;
		int keyEvents [] = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
		for(String key :  I18N.instance().getUserDesignationKeys(true)) {
			String name = I18N.instance().getUserDesignation(key);
			addDesignationItem(designationMenu, name, keyEvents[i++]);
		}
		menuBar.add(designationMenu);

		if (!MAC_OS_X) {		
			// On Mac, 'About' appears under Findbugs menu, so no need for it here
			JMenu helpMenu = newJMenu("menu.help_menu", "Help");
			JMenuItem aboutItem = newJMenuItem("menu.about_item", "About FindBugs");
			helpMenu.add(aboutItem);

				aboutItem.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent evt) {
							about();
						}
					});
				menuBar.add(helpMenu);
		}
		return menuBar;
	}
	
	static class ProjectSelector {
        public ProjectSelector(String projectName, String filter, int count) {
	        this.projectName = projectName;
	        this.filter = filter;
	        this.count = count;
        }
		final  String projectName;
		final String filter;
		final int count;
		@Override
        public String toString() {
			return String.format("%s -- [%d issues]", projectName, count);
		}
	}
	public void selectPackagePrefixByProject() {
		TreeSet<String> projects = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Multiset<String> count = new Multiset<String>();
		int total = 0;
		for (BugInstance b : bugCollection.getCollection())
			if (shouldDisplayIssueIgnoringPackagePrefixes(b)){
			TreeSet<String> projectsForThisBug = projectPackagePrefixes.getProjects(b.getPrimaryClass().getClassName());
			projects.addAll(projectsForThisBug);
			count.addAll(projectsForThisBug);
			total++;
		}
		if (projects.size() == 0) {
			JOptionPane.showMessageDialog(this, "No issues in current view");
			return;
		}
		ArrayList<ProjectSelector> selectors = new ArrayList<ProjectSelector>(projects.size() + 1);
		ProjectSelector everything = new ProjectSelector("all projects", "", total);
		selectors.add(everything);
		for (String projectName : projects) {
			ProjectPackagePrefixes.PrefixFilter filter = projectPackagePrefixes.getFilter(projectName);
			selectors.add(new ProjectSelector(projectName, filter.toString(), count.getCount(projectName)));
		}
		ProjectSelector choice = (ProjectSelector) JOptionPane.showInputDialog(null, "Choose a project to set appropriate package prefix(es)", "Select package prefixes by package",
		        JOptionPane.QUESTION_MESSAGE, null, selectors.toArray(), everything);
		if (choice == null)
			return;

		textFieldForPackagesToDisplay.setText(choice.filter);
		viewFilter.setPackagesToDisplay(choice.filter);
		resetViewCache();

	}
	JMenu viewMenu ;
	public void setViewMenu() {

		Cloud cloud = this.bugCollection == null ? null : this.bugCollection.getCloud();
			
		viewMenu.removeAll();
		if (cloud != null && cloud.supportsCloudSummaries()) {
			JMenuItem cloudReport = new JMenuItem("Cloud summary");
			cloudReport.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					displayCloudReport();

				}
			});
			viewMenu.add(cloudReport);
		}
		if (projectPackagePrefixes.size() > 0 && this.bugCollection != null) {
			JMenuItem selectPackagePrefixMenu = new JMenuItem("Select class search strings by project...");
			selectPackagePrefixMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectPackagePrefixByProject();

				}
			});
			viewMenu.add(selectPackagePrefixMenu);
			
			
		}
		if (viewMenu.getItemCount() > 0)
			viewMenu.addSeparator();
		
		ButtonGroup rankButtonGroup = new ButtonGroup();
		for(final ViewFilter.RankFilter r : ViewFilter.RankFilter.values()) {
			JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
			rankButtonGroup.add(rbMenuItem);
			if (r == ViewFilter.RankFilter.ALL) 
				rbMenuItem.setSelected(true);
			rbMenuItem.addActionListener(new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					viewFilter.setRank(r);
					resetViewCache();
				}});   
			viewMenu.add(rbMenuItem);
		}
		
		viewMenu.addSeparator();

		if (cloud != null && cloud.getMode() == Cloud.Mode.COMMUNAL) {
			ButtonGroup overallClassificationButtonGroup = new ButtonGroup();
			for (final ViewFilter.OverallClassificationFilter r : ViewFilter.OverallClassificationFilter.values()) {
				if (cloud != null && !r.supported(cloud))
					continue;
				JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
				overallClassificationButtonGroup.add(rbMenuItem);
				if (r == ViewFilter.OverallClassificationFilter.ALL)
					rbMenuItem.setSelected(true);
				rbMenuItem.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						viewFilter.setClassification(r);
						resetViewCache();
					}
				});
				viewMenu.add(rbMenuItem);
			}
			viewMenu.addSeparator();
		}
		
		ButtonGroup evalButtonGroup = new ButtonGroup();
		for(final ViewFilter.CloudFilter r : ViewFilter.CloudFilter.values()) {
			if (cloud != null && !r.supported(cloud)) 
				continue;
			JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
			evalButtonGroup.add(rbMenuItem);
			if (r == ViewFilter.CloudFilter.ALL) 
				rbMenuItem.setSelected(true);
			rbMenuItem.addActionListener(new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					viewFilter.setEvaluation(r);
					resetViewCache();
				}});   
			viewMenu.add(rbMenuItem);
		}
		viewMenu.addSeparator();
		ButtonGroup ageButtonGroup = new ButtonGroup();
		for(final ViewFilter.FirstSeenFilter r : ViewFilter.FirstSeenFilter.values()) {
			JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
			ageButtonGroup.add(rbMenuItem);
			if (r == ViewFilter.FirstSeenFilter.ALL) 
				rbMenuItem.setSelected(true);
			rbMenuItem.addActionListener(new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					viewFilter.setFirstSeen(r);
					resetViewCache();
				}});   
			viewMenu.add(rbMenuItem);
		}

	}
	
	public void resetCommentsInputPane() {
		guiLayout.resetCommentsInputPane();
	}
	ViewFilter viewFilter = new ViewFilter(this);
	/**
	 * @param map
	 * @param navMenu
	 */
	private void addNavItem(final ActionMap map, JMenu navMenu, String menuNameKey, String menuNameDefault, String actionName, int keyEvent) {
		JMenuItem toggleItem = newJMenuItem(menuNameKey, menuNameDefault);
		toggleItem.addActionListener(treeActionAdapter(map, actionName));	
		attachAcceleratorKey(toggleItem, keyEvent);
		navMenu.add(toggleItem);
	}
	ActionListener treeActionAdapter(ActionMap map, String actionName) {
		final Action selectPrevious = map.get(actionName);
		return new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				e.setSource(tree);	
				selectPrevious.actionPerformed(e);	
			}};
	}
	static void attachAcceleratorKey(JMenuItem item, int keystroke) {
		attachAcceleratorKey(item, keystroke, 0);
	}
	static void attachAcceleratorKey(JMenuItem item, int keystroke,
																	 int additionalMask) {
		// As far as I know, Mac is the only platform on which it is normal
		// practice to use accelerator masks such as Shift and Alt, so
		// if we're not running on Mac, just ignore them
		if (!MAC_OS_X && additionalMask != 0) {
			return;
		}

		item.setAccelerator(KeyStroke.getKeyStroke(keystroke, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | additionalMask));
	}
	
	@SwingThread
	void expandTree() {
		expandTree(Integer.MAX_VALUE);
	}
	@SwingThread
	void expandTree(int max) {
		Debug.printf("expandTree(%d)\n", max);
		JTree jTree = getTree();
		int i = 0;
		while (true) {
			int rows = jTree.getRowCount();
			if (i >=  rows || rows >= max)
				break;
			jTree.expandRow(i++);
		}
	}
	
	
	@SwingThread
	boolean leavesShown() {
		JTree jTree = getTree();

		int rows = jTree.getRowCount();
		for(int i = 0; i < rows; i++) {
			TreePath treePath = jTree.getPathForRow(i);
			Object lastPathComponent = treePath.getLastPathComponent();
			if (lastPathComponent instanceof BugLeafNode) 
				return true;
		}
		return false;
	}
	@SwingThread
	void expandToFirstLeaf(int max) {
		Debug.println("expand to first leaf");
		if (leavesShown())
			return;
		JTree jTree = getTree();
		int i = 0;
		while (true) {
			int rows = jTree.getRowCount();
			if (i >=  rows || rows >= max)
				break;
			TreePath treePath = jTree.getPathForRow(i);
			Object lastPathComponent = treePath.getLastPathComponent();
			if (lastPathComponent instanceof BugLeafNode) return;
			jTree.expandRow(i++);
		}
	}
	void newProject(){
		clearSourcePane();
		if (curProject == null)
		  redoAnalysis.setEnabled(false);
		else 
			redoAnalysis.setEnabled(!curProject.getFileList().isEmpty());

		if(newProject){
			setProjectChanged(true);
//			setTitle(TITLE_START_TXT + Project.UNNAMED_PROJECT);
			saveFile = null;
			saveMenuItem.setEnabled(false);
			reconfigMenuItem.setEnabled(true);
			newProject=false;
		}		
	}
	/**
	 * This method is for when the user wants to open a file.
	 */
	private void importFilter() {
		filterOpenFileChooser.setDialogTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.importFilter_ttl",
		        "Import and merge filter..."));

		boolean retry = true;
		File f = null;
		while (retry) {
			retry = false;

			int value = filterOpenFileChooser.showOpenDialog(MainFrame.this);

			if (value != JFileChooser.APPROVE_OPTION)
				return;

			f = filterOpenFileChooser.getSelectedFile();

			if (!f.exists()) {
				JOptionPane.showMessageDialog(filterOpenFileChooser, "No such file", "Invalid File", JOptionPane.WARNING_MESSAGE);
				retry = true;
				continue;
			}
			Filter filter;
			try {
				filter = Filter.parseFilter(f.getPath());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(filterOpenFileChooser, "Could not load filter.");
				retry = true;
				continue;
			}
			projectChanged = true;
			if (getProject().getSuppressionFilter() == null) {
				getProject().setSuppressionFilter(filter);
			} else {
				for (Matcher m : filter.getChildren())
					getProject().getSuppressionFilter().addChild(m);
			}
			PreferencesFrame.getInstance().updateFilterPanel();
		}

	}

	/**
	 * This method is for when the user wants to open a file.
	 */
	private void open(){
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		if (projectChanged)
		{
			int response = JOptionPane.showConfirmDialog(MainFrame.this, 
					edu.umd.cs.findbugs.L10N.getLocalString("dlg.save_current_changes", "The current project has been changed, Save current changes?")
					,edu.umd.cs.findbugs.L10N.getLocalString("dlg.save_changes", "Save Changes?"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (response == JOptionPane.YES_OPTION)
			{
				if (saveFile!=null)
					save();
				else
					saveAs();
			}
			else if (response == JOptionPane.CANCEL_OPTION)
				return;
			//IF no, do nothing.
		}

		boolean loading = true;
		SaveType fileType = SaveType.NOT_KNOWN;
		tryAgain: while (loading)
		{
			int value=saveOpenFileChooser.showOpenDialog(MainFrame.this);
			if(value!=JFileChooser.APPROVE_OPTION) return;

			loading = false;
			fileType = convertFilterToType(saveOpenFileChooser.getFileFilter());
			final File f = saveOpenFileChooser.getSelectedFile();

			if(!fileType.isValid(f)){
				JOptionPane.showMessageDialog(saveOpenFileChooser,
						"That file is not compatible with the choosen file type", "Invalid File",
						JOptionPane.WARNING_MESSAGE);
				loading = true;
				continue;
			}
			
			switch (fileType) {
			case PROJECT:
				File xmlFile = OriginalGUI2ProjectFile.fileContainingXMLData(f);
				
				if (!xmlFile.exists())
				{
					JOptionPane.showMessageDialog(saveOpenFileChooser, edu.umd.cs.findbugs.L10N.getLocalString("dlg.no_xml_data_lbl", "This directory does not contain saved bug XML data, please choose a different directory."));
					loading=true;
					continue tryAgain;
				}

				openProject(f);
				break;
			case XML_ANALYSIS:
				if(!f.getName().endsWith(".xml")){
					JOptionPane.showMessageDialog(saveOpenFileChooser, edu.umd.cs.findbugs.L10N.getLocalString("dlg.not_xml_data_lbl", "This is not a saved bug XML data file."));
					loading=true;
					continue tryAgain;
				}

				if(!openAnalysis(f, fileType)){
					//TODO: Deal if something happens when loading analysis
					JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
					loading=true;
					continue tryAgain;
				}
				break;
			case FBP_FILE:
				if(!openFBPFile(f)){
					//TODO: Deal if something happens when loading analysis
					JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
					loading=true;
					continue tryAgain;
				}
				break;
			case FBA_FILE:
				if(!openFBAFile(f)){
					//TODO: Deal if something happens when loading analysis
					JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
					loading=true;
					continue tryAgain;
				}
				break;
			}
		}
	}


	/**
	 * Method called to open FBA file.
     * @param f
     * @return
     */
    private boolean openFBAFile(File f) {
	   return openAnalysis(f, SaveType.FBA_FILE);
    }
	/**
	 * Method called to open FBP file.
     * @param f
     * @return
     */
    private boolean openFBPFile(File f) {
    		if (!f.exists() || !f.canRead()) {
    			return false;
    		}

    		prepareForFileLoad(f, SaveType.FBP_FILE);
  
    		loadProjectFromFile(f);
    		

    		return true;
    		
    	
    }
    
	private boolean exportFilter() {
		if (getProject().getSuppressionFilter() == null)
		{
			JOptionPane.showMessageDialog(MainFrame.this,edu.umd.cs.findbugs.L10N.getLocalString("dlg.no_filter", "There is no filter"));
			return false;
		}
		
		filterOpenFileChooser.setDialogTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.exportFilter_ttl", "Export filter..."));

		boolean retry = true;
		boolean alreadyExists = true;
		File f = null;
		while(retry){
			retry = false;

			int value=filterOpenFileChooser.showSaveDialog(MainFrame.this);

			if (value!=JFileChooser.APPROVE_OPTION) return false;


			f = filterOpenFileChooser.getSelectedFile();

			alreadyExists = f.exists();
			if(alreadyExists){
				int response = JOptionPane.showConfirmDialog(filterOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.file_exists_lbl", "This file already exists.\nReplace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				
				if(response == JOptionPane.OK_OPTION)
					retry = false;
				if(response == JOptionPane.CANCEL_OPTION){
					retry = true;
					continue;
				}

			}

			Filter suppressionFilter = getProject().getSuppressionFilter();
			try {
			suppressionFilter.writeEnabledMatchersAsXML(new FileOutputStream(f));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
				return false;
			}
		}

		return true;
	}

	
	private boolean saveAs(){
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		
		saveOpenFileChooser.setDialogTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.saveas_ttl", "Save as..."));

		if (curProject==null)
		{
			JOptionPane.showMessageDialog(MainFrame.this,edu.umd.cs.findbugs.L10N.getLocalString("dlg.no_proj_save_lbl", "There is no project to save"));
			return false;
		}

		boolean retry = true;
		SaveType fileType = SaveType.NOT_KNOWN;
		boolean alreadyExists = true;
		File f = null;
		while(retry){
			retry = false;

			int value=saveOpenFileChooser.showSaveDialog(MainFrame.this);

			if (value!=JFileChooser.APPROVE_OPTION) return false;

			fileType = convertFilterToType(saveOpenFileChooser.getFileFilter());
			if(fileType == SaveType.NOT_KNOWN){
				Debug.println("Error! fileType == SaveType.NOT_KNOWN");
				//This should never happen b/c saveOpenFileChooser can only display filters
				//given it.
				retry = true;
				continue;
			}

			f = saveOpenFileChooser.getSelectedFile();
			
			f = convertFile(f, fileType);

			if(!fileType.isValid(f)){
				JOptionPane.showMessageDialog(saveOpenFileChooser,
						"That file is not compatible with the chosen file type", "Invalid File",
						JOptionPane.WARNING_MESSAGE);
				retry = true;
				continue;
			}

			alreadyExists = fileAlreadyExists(f, fileType);
			if(alreadyExists){
				int response = -1;

				switch(fileType){
				case XML_ANALYSIS:
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.analysis_exists_lbl", "This analysis already exists.\nReplace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					break;
				case PROJECT:
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.proj_already_exists_lbl", "This project already exists.\nDo you want to replace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					break;
				case FBP_FILE:
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("FB Project File already exists", "This FB project file already exists.\nDo you want to replace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					break;
				case FBA_FILE:
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("FB Analysis File already exists", "This FB analysis file already exists.\nDo you want to replace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					break;
				}
				
				if(response == JOptionPane.OK_OPTION)
					retry = false;
				if(response == JOptionPane.CANCEL_OPTION){
					retry = true;
					continue;
				}

			}

			SaveReturn successful = SaveReturn.SAVE_ERROR;

			switch(fileType){
			case PROJECT:
				successful = saveProject(f);
				break;
			case XML_ANALYSIS:
				successful = saveAnalysis(f);
				break;
			case FBA_FILE:
				successful = saveFBAFile(f);
				break;
			case FBP_FILE:
				successful = saveFBPFile(f);
				break;
			}
			
			if (successful != SaveReturn.SAVE_SUCCESSFUL)
			{
				JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
				return false;
			}
		}
		assert f != null;

//		saveProjectMenuItem.setEnabled(false);
		saveMenuItem.setEnabled(false);
		setSaveType(fileType);
		saveFile = f;
		File xmlFile;
		if (getSaveType()==SaveType.PROJECT)
			xmlFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".xml");
		else
			xmlFile=f;

		addFileToRecent(xmlFile, getSaveType());

		return true;
	}

	/*
	 * Returns SaveType equivalent depending on what kind of FileFilter passed.
	 */
	private	SaveType convertFilterToType(FileFilter f){
		if (f instanceof FindBugsFileFilter)
			return ((FindBugsFileFilter)f).getSaveType();

		return SaveType.NOT_KNOWN;
	}

	/*
	 * Depending on File and SaveType determines if f already exists. Returns false if not
	 * exist, true otherwise. For a project calls
	 * OriginalGUI2ProjectFile.fileContainingXMLData(f).exists
	 */
	private boolean fileAlreadyExists(File f, SaveType fileType){
		if(fileType == SaveType.PROJECT){
			/*File xmlFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".xml");
			File fasFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".fas");
			return xmlFile.exists() || fasFile.exists();*/
			return (OriginalGUI2ProjectFile.fileContainingXMLData(f).exists());
		}

		return f.exists();
	}

	/*
	 * If f is not of type FileType will convert file so it is.
	 */
	private File convertFile(File f, SaveType fileType){
		//WARNING: This assumes the filefilter for project is working so f can only be a directory.
		if(fileType == SaveType.PROJECT)
			return f;

		//Checks that it has the correct file extension, makes a new file if it doesn't.
		if(!f.getName().endsWith(fileType.getFileExtension()))
			f = new File(f.getAbsolutePath() + fileType.getFileExtension());

		return f;
	}

	private void save(){
		File sFile = saveFile;
		assert sFile != null;
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		
		SaveReturn result = SaveReturn.SAVE_ERROR;
		
		switch(getSaveType()){
		case PROJECT:
			result = saveProject(sFile);
			break;
		case XML_ANALYSIS:
			result = saveAnalysis(sFile);
			break;
		case FBA_FILE:
			result = saveFBAFile(sFile);
			break;
		case FBP_FILE:
			result = saveFBPFile(sFile);
			break;
		}


		if(result != SaveReturn.SAVE_SUCCESSFUL){
			JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString(
					"dlg.saving_error_lbl", "An error occurred in saving."));
		}
	}

	/**
     * @param saveFile2
     * @return
     */
    private SaveReturn saveFBAFile(File saveFile2) {
	    return saveAnalysis(saveFile2);
    }
	/**
     * @param saveFile2
     * @return
     */
    private SaveReturn saveFBPFile(File saveFile2) {
    		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
    		try {
    			getProject().writeXML(saveFile2);
            } catch (IOException e) {
	            AnalysisContext.logError("Couldn't save FBP file to " + saveFile2, e);
	            return SaveReturn.SAVE_IO_EXCEPTION;
            }

    		setProjectChanged(false);

    		return SaveReturn.SAVE_SUCCESSFUL;
    }
    
    
	static final String TREECARD = "Tree";
	static final String WAITCARD = "Wait";


	public void showWaitCard() {
		showCard(WAITCARD, new Cursor(Cursor.WAIT_CURSOR));
	}

	public void showTreeCard() {
		showCard(TREECARD, new Cursor(Cursor.DEFAULT_CURSOR));
	}
	private void showCard(final String c, final Cursor cursor) {
		if (SwingUtilities.isEventDispatchThread()) {
			setCursor(cursor);
			CardLayout layout = (CardLayout) cardPanel.getLayout();
			layout.show(cardPanel, c);
		} else
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					setCursor(cursor);
					CardLayout layout = (CardLayout) cardPanel.getLayout();
					layout.show(cardPanel, c);
				}
			});
	}

	JPanel waitPanel, cardPanel;
	
	JPanel makeNavigationPanel(String packageSelectorLabel, 
			JComponent packageSelector, JComponent treeHeader, JComponent tree) {
		JPanel topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(150,150));
		
		topPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = c.ipady = 3;
		c.insets = new Insets(6,6,6,6);
		c.gridx = 0;
		c.gridy = 0;
		c.fill=GridBagConstraints.NONE;
		JLabel label = new JLabel(packageSelectorLabel);
		topPanel.add(label, c);
		
		c.gridx = 1;
		c.fill=GridBagConstraints.HORIZONTAL;
		c.weightx = 1;		
		topPanel.add(packageSelector, c);
		
		c.gridx = 0;
		c.gridwidth=2;
		c.gridy++;
		c.ipadx = c.ipady = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(treeHeader, c);
		
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		c.weighty = 1;
		c.ipadx = c.ipady = 0;
		c.insets = new Insets(0,0,0,0);
		topPanel.add(tree, c);
		return topPanel;
	}
	/**
     * 
     * @return
     */
    JPanel bugListPanel()
    {
    	tableheader = new JTableHeader();
        tableheader.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
    	//Listener put here for when user double clicks on sorting
    	//column header SorterDialog appears.
    	tableheader.addMouseListener(new MouseAdapter(){
    
    		@Override
    		public void mouseClicked(MouseEvent e) {
    			Debug.println("tableheader.getReorderingAllowed() = " + tableheader.getReorderingAllowed());
    			if (!tableheader.getReorderingAllowed())
    				return;
    			if (e.getClickCount()==2)
    				SorterDialog.getInstance().setVisible(true);
    		}
    
    		@Override
    		public void mouseReleased(MouseEvent arg0) {
    			if (!tableheader.getReorderingAllowed())
    				return;
    			BugTreeModel bt=(BugTreeModel) (MainFrame.this.getTree().getModel());
    			bt.checkSorter();
    		}
    	});
    	sorter = GUISaveState.getInstance().getStarterTable();
    	tableheader.setColumnModel(sorter);
    	tableheader.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.reorder_message", "Drag to reorder tree folder and sort order"));
    
    	tree = new JTree();
    	treeUI = (BasicTreeUI) tree.getUI();
    	tree.setLargeModel(true);
    	tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    	tree.setCellRenderer(new BugRenderer());
    	tree.setRowHeight((int)(Driver.getFontSize() + 7));
    	if (false) {
    
    	System.out.println("Left indent had been " + treeUI.getLeftChildIndent());
    	System.out.println("Right indent had been " + treeUI.getRightChildIndent());
    	treeUI.setLeftChildIndent(30 );
    	treeUI.setRightChildIndent(30 );
    	}
    	tree.setModel(new BugTreeModel(tree, sorter, new BugSet(new ArrayList<BugLeafNode>())));
    	setupTreeListeners();
    	setProject(new Project());
    
    
    	treeScrollPane = new JScrollPane(tree);
    	
    	treePanel = new JPanel(new BorderLayout());
    	treePanel.add(treeScrollPane, BorderLayout.CENTER);
    	//New code to fix problem in Windows
    	JTable t = new JTable(new DefaultTableModel(0, sortables().length));
    	t.setTableHeader(tableheader);
    	
    	if (false) {
    		JScrollPane tableHeaderScrollPane = new JScrollPane(t);
        	//This sets the height of the scrollpane so it is dependent on the fontsize.
        	
        	
    		int num = (int) (Driver.getFontSize());
    		tableHeaderScrollPane.setPreferredSize(new Dimension(0, 0));
    		tableHeaderScrollPane.setMaximumSize(new Dimension(200, 5));
    	}
    
    	//End of new code.
    	//Changed code.
    	textFieldForPackagesToDisplay = new JTextField();
        ActionListener filterAction = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    viewFilter.setPackagesToDisplay(textFieldForPackagesToDisplay.getText());
                    resetViewCache();
                } catch (IllegalArgumentException err) {
                    JOptionPane.showMessageDialog(MainFrame.this, err.getMessage(), "Bad class search string", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        textFieldForPackagesToDisplay.addActionListener(filterAction);
        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(filterAction);
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        filterPanel.add(textFieldForPackagesToDisplay, gbc);

        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        filterPanel.add(filterButton, gbc);
    
    	filterPanel.setToolTipText("Only show classes containing the word(s) you specify");

        JPanel sortablePanel = new JPanel(new GridBagLayout());
        JLabel sortableLabel = new JLabel("Group bugs by:");
        sortableLabel.setLabelFor(tableheader);
        gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(3,3,3,3);
        gbc.fill = GridBagConstraints.BOTH;
        sortablePanel.add(sortableLabel, gbc);
        gbc.weightx = 1;
        sortablePanel.add(tableheader, gbc);

        tableheader.setBorder(new LineBorder(Color.BLACK));

    	JPanel topPanel = makeNavigationPanel("Class name filter:", filterPanel, sortablePanel, treePanel);
    	cardPanel = new JPanel(new CardLayout());
    	waitPanel = new JPanel();
    	waitPanel.setLayout(new BoxLayout(waitPanel, BoxLayout.Y_AXIS));
    	waitPanel.add(new JLabel("Please wait..."));
    	waitStatusLabel = new JLabel();
		waitPanel.add(waitStatusLabel);
    	cardPanel.add(topPanel, TREECARD);
    	cardPanel.add(waitPanel, WAITCARD);
    	return cardPanel;
    }
	JTextField textFieldForPackagesToDisplay;
	public void newTree(final JTree newTree, final BugTreeModel newModel)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				tree = newTree;
				tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
				tree.setLargeModel(true);
				tree.setCellRenderer(new BugRenderer());
				showTreeCard();
				treePanel.remove(treeScrollPane);
				treeScrollPane = new JScrollPane(newTree);
				treePanel.add(treeScrollPane);
				setFontSizeHelper(Driver.getFontSize(), treeScrollPane);
				tree.setRowHeight((int)(Driver.getFontSize() + 7));
				MainFrame.this.getContentPane().validate();
				MainFrame.this.getContentPane().repaint();

				setupTreeListeners();
				newModel.openPreviouslySelected(((BugTreeModel)(tree.getModel())).getOldSelectedBugs());
				MainFrame.this.expandTree(10);
				MainFrame.this.expandToFirstLeaf(14);
				MainFrame.this.getSorter().addColumnModelListener(newModel);
				FilterActivity.addFilterListener(newModel.bugTreeFilterListener);
				MainFrame.this.setSorting(true);

			}
		});
	}

	private void setupTreeListeners() {
		if (false) 
			tree.addTreeExpansionListener(new TreeExpansionListener(){

			public void treeExpanded(TreeExpansionEvent event) {
				System.out.println("Tree expanded");
	            TreePath path = event.getPath();
	            Object lastPathComponent = path.getLastPathComponent();
	            int children  = tree.getModel().getChildCount(lastPathComponent);
	            if (children == 1) {
	            	Object o = tree.getModel().getChild(lastPathComponent, 0);
	            	if (o instanceof BugAspects) {
	            		final TreePath p = path.pathByAddingChild(o);
	            		SwingUtilities.invokeLater(new Runnable() {

							public void run() {try {
								System.out.println("auto expanding " + p);
								tree.expandPath(p);
							} catch (Exception e) {
								e.printStackTrace();
							}
	                            
                            }});
	            	}
	            	
	            }
            }

			public void treeCollapsed(TreeExpansionEvent event) {
	           // do nothing
	            
            }});
		tree.addTreeSelectionListener(new TreeSelectionListener(){
			public void valueChanged(TreeSelectionEvent selectionEvent) {

				TreePath path = selectionEvent.getNewLeadSelectionPath();
				if (path != null)
				{
					saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

					Object lastPathComponent = path.getLastPathComponent();
					if (lastPathComponent instanceof BugLeafNode)
					{	
						boolean beforeProjectChanged = projectChanged;
						currentSelectedBugLeaf = (BugLeafNode)lastPathComponent;
						currentSelectedBugAspects = null;
						syncBugInformation();
						setProjectChanged(beforeProjectChanged);
					}
					else
					{
						boolean beforeProjectChanged = projectChanged;
						updateDesignationDisplay();
						currentSelectedBugLeaf = null;
						currentSelectedBugAspects = (BugAspects)lastPathComponent;
						syncBugInformation();
						setProjectChanged(beforeProjectChanged);
					}
				}


//				Debug.println("Tree selection count:" + tree.getSelectionCount());
				if (tree.getSelectionCount() !=1)
				{
					Debug.println("Tree selection count not equal to 1, disabling comments tab" + selectionEvent);

					MainFrame.this.setUserCommentInputEnable(false);
				}
			}						
		});

		tree.addMouseListener(new MouseListener(){

			public void mouseClicked(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());

				if(path == null)
					return;

				if ((e.getButton() == MouseEvent.BUTTON3) || 
						(e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())){

					if (tree.getModel().isLeaf(path.getLastPathComponent())){
						tree.setSelectionPath(path);
						bugPopupMenu.show(tree, e.getX(), e.getY());
					}
					else{
						tree.setSelectionPath(path);
						if (!(path.getParentPath()==null))//If the path's parent path is null, the root was selected, dont allow them to filter out the root.
							branchPopupMenu.show(tree, e.getX(), e.getY());
					}
				}		
			}			

			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}			
		});
	}


	void syncBugInformation (){
		boolean prevProjectChanged = projectChanged;
		if (currentSelectedBugLeaf != null)  {
			BugInstance bug  = currentSelectedBugLeaf.getBug();
			displayer.displaySource(bug, bug.getPrimarySourceLineAnnotation());
			updateDesignationDisplay();
			comments.updateCommentsFromLeafInformation(currentSelectedBugLeaf);
			updateSummaryTab(currentSelectedBugLeaf);
		} else if (currentSelectedBugAspects != null) {
			updateDesignationDisplay();
			comments.updateCommentsFromNonLeafInformation(currentSelectedBugAspects);
			displayer.displaySource(null, null);
			clearSummaryTab();
		} else {
			displayer.displaySource(null, null);
			clearSummaryTab();
		}
		setProjectChanged(prevProjectChanged);
	}

	/**
	 * Clears the source code text pane.
	 *
	 */
	 void clearSourcePane(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				setSourceTab("", null);				
				sourceCodeTextPane.setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
			}
		});	
	}



	/**
	 * @param b
	 */
	private void setUserCommentInputEnable(boolean b) {
		comments.setUserCommentInputEnable(b);

	}
	/**
	 * Creates the status bar of the GUI.
	 * @return
	 */
	JPanel statusBar()
	{
		JPanel statusBar = new JPanel();
		// statusBar.setBackground(Color.WHITE);

		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusBar.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 0;
        constraints.weightx = 1;
        statusBar.add(statusBarLabel, constraints.clone());

        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;

        try {
            signedInIcon = loadImageResource("greencircle.png", 16, 16);
            warningIcon = loadImageResource("warningicon.png", 16, 16);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load status icons", e);
            signedInIcon = null;
            warningIcon = null;
        }
        signedInLabel = new JLabel();
        signedInLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signedInLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu menu = new JPopupMenu();
                SigninState signinState = bugCollection.getCloud().getSigninState();
                boolean isSignedIn = signinState == Cloud.SigninState.SIGNED_IN;
                final JCheckBoxMenuItem signInAuto = new JCheckBoxMenuItem("Sign in automatically");
                signInAuto.setToolTipText("Saves your Cloud session for the next time you run FindBugs. " +
                                          "No personal information or passwords are saved.");
                signInAuto.setSelected(bugCollection.getCloud().isSavingSignInInformationEnabled());
                signInAuto.setEnabled(isSignedIn);
                signInAuto.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        boolean checked = signInAuto.isSelected();
                        if (checked != bugCollection.getCloud().isSavingSignInInformationEnabled()) {
                            System.out.println("checked: " + checked);
                            bugCollection.getCloud().setSaveSignInInformation(checked);
                        }
                    }
                });
                menu.add(signInAuto);
                if (signinState == Cloud.SigninState.SIGNED_OUT || signinState == Cloud.SigninState.NOT_SIGNED_IN_YET || signinState == Cloud.SigninState.SIGNIN_FAILED) {
                    menu.add(new AbstractAction("Sign in") {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                bugCollection.getCloud().signIn();
                            } catch (IOException e1) {
                                showMessageDialog("Sign-in error: " + e1.getMessage());
                                LOGGER.log(Level.SEVERE, "Could not sign in", e1);
                            }
                        }
                    });
                } else {
                    menu.add(new AbstractAction("Sign out") {
                        public void actionPerformed(ActionEvent e) {
                            bugCollection.getCloud().signOut();
                        }
                    }).setEnabled(isSignedIn);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(0,5,0,5);
        statusBar.add(signedInLabel, constraints.clone());

        signedInLabel.setVisible(false);

		JLabel logoLabel = new JLabel();

        constraints.insets = new Insets(0,0,0,0);
		ImageIcon logoIcon = new ImageIcon(MainFrame.class.getResource("logo_umd.png"));
		logoLabel.setIcon(logoIcon);
        logoLabel.setPreferredSize(new Dimension(logoIcon.getIconWidth(), logoIcon.getIconHeight()));
        constraints.anchor = GridBagConstraints.WEST;
		statusBar.add(logoLabel, constraints.clone());

		return statusBar;
	}

    private ImageIcon loadImageResource(String filename, int width, int height) throws IOException {
        return new ImageIcon(ImageIO.read(MainFrame.class.getResource(filename)).getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private String join(String s1, String s2) {
		if (s1 == null || s1.length() == 0) return s2;
		if (s2 == null || s2.length() == 0) return s1;
		return s1 + "; " + s2;
	}
	@SwingThread
	void updateStatusBar() {

		int countFilteredBugs = BugSet.countFilteredBugs();
		String msg = "";
		if (countFilteredBugs == 1) {
	         msg = "  1 " + edu.umd.cs.findbugs.L10N.getLocalString("statusbar.bug_hidden", "bug hidden by filters");
	    } else 	if (countFilteredBugs > 1) {
	        msg = "  " + countFilteredBugs + " " + edu.umd.cs.findbugs.L10N.getLocalString("statusbar.bugs_hidden", "bugs hidden by filters");
        }
        boolean showLoggedInStatus = false;
		if (bugCollection != null) {
			Cloud plugin = bugCollection.getCloud();
			if (plugin != null) {
				String pluginMsg = plugin.getStatusMsg();
				if (pluginMsg != null && pluginMsg.length() > 1)
					msg = join(msg, pluginMsg);
                SigninState state = plugin.getSigninState();
                if (state == SigninState.SIGNING_IN) {
                    signedInLabel.setText("<html>FindBugs Cloud:<br> signing in");
                    signedInLabel.setIcon(null);
                    showLoggedInStatus = true;
                } else if (state == Cloud.SigninState.SIGNED_IN) {
                    signedInLabel.setText("<html>FindBugs Cloud:<br> signed in as " + plugin.getUser());
                    signedInLabel.setIcon(signedInIcon);
                    showLoggedInStatus = true;
                } else if (state == SigninState.SIGNIN_FAILED) {
                    signedInLabel.setText("<html>FindBugs Cloud:<br> sign-in failed");
                    signedInLabel.setIcon(warningIcon);
                    showLoggedInStatus = true;
                } else if (state == SigninState.SIGNED_OUT || state == SigninState.NOT_SIGNED_IN_YET) {
                    signedInLabel.setText("<html>FindBugs Cloud:<br> not signed in");
                    signedInLabel.setIcon(null);
                    showLoggedInStatus = true;
                }
			}
		}
        signedInLabel.setVisible(showLoggedInStatus);
		if (errorMsg != null && errorMsg.length() > 0)
			msg = join(msg, errorMsg);
        waitStatusLabel.setText(msg); // should not be the URL
		if (msg.length() == 0)
			msg = "http://findbugs.sourceforge.net";
        statusBarLabel.setText(msg);
	}

	volatile String errorMsg = "";
	public void setErrorMessage(String errorMsg) {
		this.errorMsg = errorMsg;
		SwingUtilities.invokeLater(new Runnable(){

			public void run() {
	            updateStatusBar();
            }});
	}
	
	
	private void updateSummaryTab(BugLeafNode node)
	{
		final BugInstance bug = node.getBug();

		final ArrayList<BugAnnotation> primaryAnnotations = new ArrayList<BugAnnotation>();
		boolean classIncluded = false;

		//This ensures the order of the primary annotations of the bug
		if(bug.getPrimarySourceLineAnnotation() != null)
			primaryAnnotations.add(bug.getPrimarySourceLineAnnotation());
		if(bug.getPrimaryMethod() != null)
			primaryAnnotations.add(bug.getPrimaryMethod());
		if(bug.getPrimaryField() != null)
			primaryAnnotations.add(bug.getPrimaryField());

		/*
		 * This makes the primary class annotation appear only when
		 * the visible field and method primary annotations don't have
		 * the same class.
		 */
		if(bug.getPrimaryClass() != null){
			FieldAnnotation primeField = bug.getPrimaryField();
			MethodAnnotation primeMethod = bug.getPrimaryMethod();
			ClassAnnotation primeClass = bug.getPrimaryClass();
			String fieldClass = "";
			String methodClass = "";
			if(primeField != null)
				fieldClass = primeField.getClassName();
			if(primeMethod != null)
				methodClass = primeMethod.getClassName();			
			if((primaryAnnotations.size() < 2) || (!(primeClass.getClassName().equals(fieldClass) || 
					primeClass.getClassName().equals(methodClass)))){
				primaryAnnotations.add(primeClass);
				classIncluded = true;
			}
		}

		final boolean classIncluded2 = classIncluded;

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				summaryTopPanel.removeAll();

				summaryTopPanel.add(bugSummaryComponent(bug.getMessageWithoutPrefix(), bug));
				for(BugAnnotation b : primaryAnnotations)
					summaryTopPanel.add(bugSummaryComponent(b, bug));


				if(!classIncluded2 && bug.getPrimaryClass() != null)
					primaryAnnotations.add(bug.getPrimaryClass());

				for(Iterator<BugAnnotation> i = bug.annotationIterator(); i.hasNext();){
					BugAnnotation b = i.next();
					boolean cont = true;
					for(BugAnnotation p : primaryAnnotations)
						if(p == b)
							cont = false;

					if(cont)
						summaryTopPanel.add(bugSummaryComponent(b, bug));
				}

				summaryHtmlArea.setText(bug.getBugPattern().getDetailHTML());

				summaryTopPanel.add(Box.createVerticalGlue());
				summaryTopPanel.revalidate();

				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						summaryHtmlScrollPane.getVerticalScrollBar().setValue(summaryHtmlScrollPane.getVerticalScrollBar().getMinimum());
					}
				});
			}
		});
	}

	private void clearSummaryTab()
	{
		summaryHtmlArea.setText("");
		summaryTopPanel.removeAll();
		summaryTopPanel.revalidate();	
	}

	/**
	 * Creates initial summary tab and sets everything up.
	 * @return
	 */
	JSplitPane summaryTab()
	{
		int fontSize = (int) Driver.getFontSize();
		summaryTopPanel = new JPanel();
		summaryTopPanel.setLayout(new GridLayout(0,1));
		summaryTopPanel.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
		summaryTopPanel.setMinimumSize(new Dimension(fontSize * 50, fontSize*5));

		JPanel summaryTopOuter = new JPanel(new BorderLayout());
		summaryTopOuter.add(summaryTopPanel, BorderLayout.NORTH);

		summaryHtmlArea.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.longer_description", "This gives a longer description of the detected bug pattern"));
		summaryHtmlArea.setContentType("text/html");
		summaryHtmlArea.setEditable(false);
		summaryHtmlArea.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
				public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
					AboutDialog.editorPaneHyperlinkUpdate(evt);
				}
			});
		setStyleSheets();
		//JPanel temp = new JPanel(new BorderLayout());
		//temp.add(summaryTopPanel, BorderLayout.CENTER);
		JScrollPane summaryScrollPane = new JScrollPane(summaryTopOuter);
		summaryScrollPane.getVerticalScrollBar().setUnitIncrement( (int)Driver.getFontSize() );

		JSplitPane splitP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, 
				summaryScrollPane, summaryHtmlScrollPane);
		splitP.setDividerLocation(GUISaveState.getInstance().getSplitSummary());
        splitP.setOneTouchExpandable(true);
        splitP.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {
                    }
                };
            }
        });
        splitP.setBorder(null);
        return splitP;
    }

    /**
	 * Creates bug summary component. If obj is a string will create a JLabel
	 * with that string as it's text and return it. If obj is an annotation
	 * will return a JLabel with the annotation's toString(). If that
	 * annotation is a SourceLineAnnotation or has a SourceLineAnnotation
	 * connected to it and the source file is available will attach
	 * a listener to the label.
	 * @param obj
	 * @param bug TODO
	 * @return
	 */
	
	private Component bugSummaryComponent(String str, BugInstance bug){
		JLabel label = new JLabel();
		label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		label.setForeground(Color.BLACK);

		label.setText(str);
		return label;
	}
	
	private Component bugSummaryComponent(BugAnnotation value, BugInstance bug){
		JLabel label = new JLabel();
		label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		label.setForeground(Color.BLACK);
		ClassAnnotation primaryClass = bug.getPrimaryClass();

		String sourceCodeLabel = edu.umd.cs.findbugs.L10N.getLocalString("summary.source_code", "source code.");
		String summaryLine = edu.umd.cs.findbugs.L10N.getLocalString("summary.line", "Line");
		String summaryLines = edu.umd.cs.findbugs.L10N.getLocalString("summary.lines", "Lines");
		String clickToGoToText = edu.umd.cs.findbugs.L10N.getLocalString("tooltip.click_to_go_to", "Click to go to");
		if (value instanceof SourceLineAnnotation) {
			final SourceLineAnnotation note = (SourceLineAnnotation) value;
			if (sourceCodeExists(note)) {
				String srcStr = "";
				int start = note.getStartLine();
				int end = note.getEndLine();
				if (start < 0 && end < 0)
					srcStr = sourceCodeLabel;
				else if (start == end)
					srcStr = " [" + summaryLine + " " + start + "]";
				else if (start < end)
					srcStr = " [" + summaryLines + " " + start + " - " + end  + "]";

				label.setToolTipText(clickToGoToText + " " + srcStr);

				label.addMouseListener(new BugSummaryMouseListener(bug, label, note));
			}

			label.setText(note.toString());
		} else if (value instanceof BugAnnotationWithSourceLines) {
			BugAnnotationWithSourceLines note = (BugAnnotationWithSourceLines) value;
			final SourceLineAnnotation noteSrc = note.getSourceLines();
			String srcStr = "";
			if (noteSrc != null && sourceCodeExists(noteSrc)) {
				int start = noteSrc.getStartLine();
				int end = noteSrc.getEndLine();
				if (start < 0 && end < 0)
					srcStr = sourceCodeLabel;
				else if (start == end)
					srcStr = " [" + summaryLine + " " + start + "]";
				else if (start < end)
					srcStr = " [" + summaryLines + " " + start + " - " + end + "]";

				if (!srcStr.equals("")) {
					label.setToolTipText(clickToGoToText + " " + srcStr);
					label.addMouseListener(new BugSummaryMouseListener(bug, label, noteSrc));
				}
			}
			String noteText;
			if (note == bug.getPrimaryMethod() || note == bug.getPrimaryField())
				noteText = note.toString();
			else 
				noteText = note.toString(primaryClass);
			if (!srcStr.equals(sourceCodeLabel))
				label.setText(noteText + srcStr);
			else
				label.setText(noteText);
		} else {
			label.setText(value.toString(primaryClass));
		}

		return label;
	}


	/**
	 * @author pugh
	 */
	private final class InitializeGUI implements Runnable {
		public void run()
		{
			setTitle("FindBugs");

			try {
				guiLayout.initialize();
			} catch(Exception e) {
				// If an exception was encountered while initializing, this may
				// be because of a bug in the particular look-and-feel selected
				// (as in sourceforge bug 1899648).  In an attempt to recover
				// gracefully, this code reverts to the cross-platform look-
				// and-feel and attempts again to initialize the layout.
				if(!UIManager.getLookAndFeel().getName().equals("Metal")) {
					System.err.println("Exception caught initializing GUI; reverting to CrossPlatformLookAndFeel");
					try {
						UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					} catch(Exception e2) {
						System.err.println("Exception while setting CrossPlatformLookAndFeel: " + e2);
						throw new Error(e2);
					}
					guiLayout.initialize();
				} else {
					throw new Error(e);
				}
			}
			bugPopupMenu = createBugPopupMenu();
			branchPopupMenu = createBranchPopUpMenu();
			comments.loadPrevCommentsList(GUISaveState.getInstance().getPreviousComments().toArray(new String[GUISaveState.getInstance().getPreviousComments().size()]));
			updateStatusBar();
			setBounds(GUISaveState.getInstance().getFrameBounds()); 
			Toolkit.getDefaultToolkit().setDynamicLayout(true);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setJMenuBar(createMainMenuBar());
			setVisible(true);

			//Initializes save and open filechooser. - Kristin
			
			saveOpenFileChooser = new FBFileChooser();
			saveOpenFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			saveOpenFileChooser.setAcceptAllFileFilterUsed(false);
			saveOpenFileChooser.addChoosableFileFilter(FindBugsProjectFileFilter.INSTANCE);
			saveOpenFileChooser.addChoosableFileFilter(FindBugsAnalysisFileFilter.INSTANCE);
			saveOpenFileChooser.addChoosableFileFilter(FindBugsFBPFileFilter.INSTANCE);
			saveOpenFileChooser.addChoosableFileFilter(FindBugsFBAFileFilter.INSTANCE);
			saveOpenFileChooser.setFileFilter(FindBugsAnalysisFileFilter.INSTANCE);
			filterOpenFileChooser = new FBFileChooser();
			filterOpenFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			filterOpenFileChooser.setFileFilter(FindBugsFilterFileFilter.INSTANCE);
			
			//Sets the size of the tooltip to match the rest of the GUI. - Kristin
			JToolTip tempToolTip = tableheader.createToolTip();
			UIManager.put( "ToolTip.font", new FontUIResource(tempToolTip.getFont().deriveFont(Driver.getFontSize())));

			if (MAC_OS_X)
			{
				 try {
					osxAdapter = Class.forName("edu.umd.cs.findbugs.gui2.OSXAdapter");
					Class[] defArgs = {MainFrame.class};
					Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
					if (registerMethod != null) {
						registerMethod.invoke(osxAdapter, MainFrame.this);
					}
					defArgs[0] = boolean.class;
					osxPrefsEnableMethod = osxAdapter.getDeclaredMethod("enablePrefs", defArgs);
					enablePreferences(true);
				} catch (NoClassDefFoundError e) {
					// This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
					// because OSXAdapter extends ApplicationAdapter in its def
					System.err.println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled (" + e + ")");
				} catch (ClassNotFoundException e) {
					// This shouldn't be reached; if there's a problem with the OSXAdapter we should get the
					// above NoClassDefFoundError first.
					System.err.println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled (" + e + ")");
				} catch (Exception e) {
					System.err.println("Exception while loading the OSXAdapter: " + e);
					e.printStackTrace();
					if (DEBUG) {
						e.printStackTrace();
					}
				}
			}
			String loadFromURL = SystemProperties.getOSDependentProperty("findbugs.loadBugsFromURL");
			

			if (loadFromURL != null) {
				try {
					loadFromURL = SystemProperties.rewriteURLAccordingToProperties(loadFromURL);
					URL url = new URL(loadFromURL);
					BugTreeModel.pleaseWait(edu.umd.cs.findbugs.L10N.getLocalString("msg.loading_bugs_over_network_txt", "Loading bugs over network..."));
					loadAnalysis(url);
				} catch (MalformedURLException e1) {
					JOptionPane.showMessageDialog(MainFrame.this, "Error loading "  + loadFromURL);
				}
			}

			addComponentListener(new ComponentAdapter(){
				@Override
				public void componentResized(ComponentEvent e){
					comments.resized();
				}
			});

			addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e) {
					if(comments.hasFocus())
						setProjectChanged(true);
					callOnClose();
				}				
			});

			Driver.removeSplashScreen();
			mainFrameInitialized.countDown();
		}
	}

	public void waitUntilReady() throws InterruptedException {
		mainFrameInitialized.await();
	}
	/**
	 * Listens for when cursor is over the label and when it is clicked.
	 * When the cursor is over the label will make the label text blue 
	 * and the cursor the hand cursor. When clicked will take the
	 * user to the source code tab and to the lines of code connected
	 * to the SourceLineAnnotation.
	 * @author Kristin Stephens
	 *
	 */
	private class BugSummaryMouseListener extends MouseAdapter{
		private final BugInstance bugInstance;
		private final JLabel label;
		private final SourceLineAnnotation note;

		BugSummaryMouseListener(@NonNull BugInstance bugInstance, @NonNull JLabel label,  @NonNull SourceLineAnnotation note){
			this.bugInstance = bugInstance;
			this.label = label;
			this.note = note;
		}

		@Override
		public void mouseClicked(MouseEvent e) {			
			displayer.displaySource(bugInstance, note);
		}
		@Override
		public void mouseEntered(MouseEvent e){
			label.setForeground(Color.blue);
			setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
		@Override
		public void mouseExited(MouseEvent e){
			label.setForeground(Color.black);
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * Checks if source code file exists/is available
	 * @param note
	 * @return
	 */
	private boolean sourceCodeExists(@Nonnull SourceLineAnnotation note){
		try{
			getProject().getSourceFinder().findSourceFile(note);
		}catch(FileNotFoundException e){
			return false;
		}catch(IOException e){
			return false;
		}
		return true;
	}

	private void setStyleSheets() {
		StyleSheet styleSheet = new StyleSheet();
		styleSheet.addRule("body {font-size: " + Driver.getFontSize() +"pt}");
		styleSheet.addRule("H1 {color: red;  font-size: 120%; font-weight: bold;}");
		styleSheet.addRule("code {font-family: courier; font-size: " + Driver.getFontSize() +"pt}");
		styleSheet.addRule(" a:link { color: #0000FF; } ");
		styleSheet.addRule(" a:visited { color: #800080; } ");
		styleSheet.addRule(" a:active { color: #FF0000; text-decoration: underline; } ");
		htmlEditorKit.setStyleSheet(styleSheet);
		summaryHtmlArea.setEditorKit(htmlEditorKit);
	}

	JPanel createCommentsInputPanel() {
		return comments.createCommentsInputPanel();
	}	

	/**
	 * Creates the source code panel, but does not put anything in it.
	 * @param text
	 * @return
	 */
	JPanel createSourceCodePanel()
	{
		Font sourceFont = new Font("Monospaced", Font.PLAIN, (int)Driver.getFontSize());
		sourceCodeTextPane.setFont(sourceFont);
		sourceCodeTextPane.setEditable(false);
		sourceCodeTextPane.getCaret().setSelectionVisible(true);
		sourceCodeTextPane.setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
		sourceCodeScrollPane = new JScrollPane(sourceCodeTextPane);
		sourceCodeScrollPane.getVerticalScrollBar().setUnitIncrement(20);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(sourceCodeScrollPane, BorderLayout.CENTER);

		panel.revalidate();
		if (DEBUG) System.out.println("Created source code panel");
		return panel;
	}

	JPanel createSourceSearchPanel()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel thePanel = new JPanel();
		thePanel.setLayout(gridbag);
		findButton.setToolTipText("Find first occurrence");
		findNextButton.setToolTipText("Find next occurrence");
		findPreviousButton.setToolTipText("Find previous occurrence");
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.insets = new Insets(0, 5, 0, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(sourceSearchTextField, c);
		thePanel.add(sourceSearchTextField);
		//add the buttons
		findButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				searchSource(0);
			}
		});
		c.gridx = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(findButton, c);
		thePanel.add(findButton);
		findNextButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				searchSource(1);
			}
		});
		c.gridx = 2;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(findNextButton, c);
		thePanel.add(findNextButton);
		findPreviousButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				searchSource(2);
			}
		});
		c.gridx = 3;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints(findPreviousButton, c);
		thePanel.add(findPreviousButton);
		return thePanel;
	}

	void searchSource(int type)
	{
		int targetLineNum = -1;
		String targetString = sourceSearchTextField.getText();
		switch(type)
		{
		case 0: targetLineNum = displayer.find(targetString);
				break;
		case 1: targetLineNum = displayer.findNext(targetString);
				break;
		case 2: targetLineNum = displayer.findPrevious(targetString);
				break;
		}
		if(targetLineNum != -1)
			displayer.foundItem(targetLineNum);
	}



	/**
	 * Sets the title of the source tabs for either docking or non-docking
	 * versions.
	 * @param title
	 * @param bug TODO
	 */
	 void setSourceTab(String title, @CheckForNull BugInstance bug){
		JComponent label = guiLayout.getSourceViewComponent();
		if (label != null) {
			URL u = null;
			if (bug != null) {
				Cloud plugin = this.bugCollection.getCloud();
				if (plugin.supportsSourceLinks())
					u = plugin.getSourceLink(bug);
			}
			if (u != null)
				 addLink(label, u);
			else
				removeLink(label);
			
		}
		guiLayout.setSourceTitle(title);
	}

	 
	 

	 URL sourceLink;
	 boolean listenerAdded = false;
	 void addLink(JComponent component, URL source) {
		 this.sourceLink = source;
		 component.setEnabled(true);
		 if (!listenerAdded) {
			 listenerAdded = true;
			 component.addMouseListener(new MouseAdapter(){
				    @Override
                    public void mouseClicked(MouseEvent e) {
				    	URL u = sourceLink;
				    	if (u != null) 
	                        LaunchBrowser.showDocument(u);
                        
				    	
				    }
			 });
		 }
		 component.setCursor(new Cursor(Cursor.HAND_CURSOR));
		 Cloud plugin = this.bugCollection.getCloud();
			if (plugin != null) 
				 component.setToolTipText(plugin.getSourceLinkToolTip(null));
			
		
	 }
	 void removeLink(JComponent component) {
		 this.sourceLink = null;
		 component.setEnabled(false);
		 component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		 component.setToolTipText("");
	 }


	/**
	 * Returns the SorterTableColumnModel of the MainFrame.
	 * @return
	 */
	SorterTableColumnModel getSorter()
	{
		return sorter;
	}

	/*
	 * This is overridden for changing the font size
	 */
	@Override
	public void addNotify(){
		super.addNotify();

		float size = Driver.getFontSize();

		getJMenuBar().setFont(getJMenuBar().getFont().deriveFont(size));		
		for(int i = 0; i < getJMenuBar().getMenuCount(); i++){
			for(int j = 0; j < getJMenuBar().getMenu(i).getMenuComponentCount(); j++){
				Component temp = getJMenuBar().getMenu(i).getMenuComponent(j);
				temp.setFont(temp.getFont().deriveFont(size));
			}
		}

		bugPopupMenu.setFont(bugPopupMenu.getFont().deriveFont(size));
		setFontSizeHelper(size, bugPopupMenu.getComponents());

		branchPopupMenu.setFont(branchPopupMenu.getFont().deriveFont(size));
		setFontSizeHelper(size, branchPopupMenu.getComponents());

	}

	public JTree getTree()
	{
		return tree;
	}
	
	public BugTreeModel getBugTreeModel() {
		return (BugTreeModel)getTree().getModel();
	}

	static class CutAction extends TextAction {

		public CutAction() {
			super(edu.umd.cs.findbugs.L10N.getLocalString("txt.cut", "Cut"));
		}

		public void actionPerformed( ActionEvent evt ) {
			JTextComponent text = getTextComponent( evt );

			if(text == null)
				return;

			text.cut();
		}
	}

	static class CopyAction extends TextAction {

		public CopyAction() {
			super(edu.umd.cs.findbugs.L10N.getLocalString("txt.copy", "Copy"));
		}

		public void actionPerformed( ActionEvent evt ) {
			JTextComponent text = getTextComponent( evt );

			if(text == null)
				return;

			text.copy();
		}
	}

	static class PasteAction extends TextAction {

		public PasteAction() {
			super(edu.umd.cs.findbugs.L10N.getLocalString("txt.paste", "Paste"));
		}

		public void actionPerformed( ActionEvent evt ) {
			JTextComponent text = getTextComponent( evt );

			if(text == null)
				return;

			text.paste();
		}
	}	

	/**
	 * @return never null
	 */
	public synchronized Project getProject() {
		if(curProject == null){
			curProject = new Project();
		}
		return curProject;
	}
	
	public synchronized void setProject(Project p) {
		curProject=p;
	}

	@SwingThread
	public void setRebuilding(boolean b)
	{
		tableheader.setReorderingAllowed(!b);
		enablePreferences(!b);
		if (b) {
			SorterDialog.getInstance().freeze();
			showWaitCard();
		}
		else {
			SorterDialog.getInstance().thaw();
			showTreeCard();
		}
		recentMenu.setEnabled(!b);
	}

	public void setSorting(boolean b) {
		tableheader.setReorderingAllowed(b);
	}

	private void setSaveMenu() {
		File s = saveFile;
		saveMenuItem.setEnabled(projectChanged && s != null && getSaveType() != SaveType.FBP_FILE && s.exists());
	}
	/**
	 * Called when something in the project is changed and the change needs to be saved.
	 * This method should be called instead of using projectChanged = b.
	 */
	public void setProjectChanged(boolean b){
		if(curProject == null)
			return;

		if(projectChanged == b)
			return;

		projectChanged = b;
		setSaveMenu();
//		if(projectDirectory != null && projectDirectory.exists())
//			saveProjectMenuItem.setEnabled(b);

		getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.valueOf(b));

	}

	public boolean getProjectChanged(){
		return projectChanged;
	}

	/*
	 * DO NOT use the projectDirectory variable to figure out the current project directory in this function
	 * use the passed in value, as that variable may or may not have been set to the passed in value at this point.
	 */
	private SaveReturn saveProject(File dir)
	{
		if (!dir.mkdir()) {
			return SaveReturn.SAVE_IO_EXCEPTION;
		}
		
		File f = new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");	
		File filtersAndSuppressions=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");

		BugSaver.saveBugs(f,bugCollection, getProject());
		try {
			getProjectSettings().save(new FileOutputStream(filtersAndSuppressions));
		} catch (IOException e) {
			Debug.println(e);
			return SaveReturn.SAVE_IO_EXCEPTION;
		}
		setProjectChanged(false);

		return SaveReturn.SAVE_SUCCESSFUL;
	}

    /**
	 * @param currentSelectedBugLeaf2
	 * @param currentSelectedBugAspects2
	 */
	private void saveComments(BugLeafNode theNode, BugAspects theAspects) {
		comments.saveComments(theNode, theAspects);

	}

	void saveComments() {
		comments.saveComments();

	}
	/**
	 * Returns the color of the source code pane's background.
	 * @return the color of the source code pane's background
	 */
	public Color getSourceColor(){
		return sourceCodeTextPane.getBackground();
	}

	/**
	 * Show an error dialog.
	 */
	public void error(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Write a message to the console window.
	 * 
	 * @param message the message to write
	 */
	public void writeToLog(String message) {
		if (DEBUG)
			System.out.println(message);
		//		consoleMessageArea.append(message);
		//		consoleMessageArea.append("\n");
	}

	/**
	 * Save current analysis as file passed in. Return SAVE_SUCCESSFUL if save successful.
	 */
	/*
	 * Method doesn't do much. This method is more if need to do other things in the future for
	 * saving analysis. And to keep saving naming convention.
	 */
	private SaveReturn saveAnalysis(File f){
		BugSaver.saveBugs(f, bugCollection, getProject());

		setProjectChanged(false);

		return SaveReturn.SAVE_SUCCESSFUL;
	}

	/**
	 * Opens the analysis. Also clears the source and summary panes. Makes comments enabled false.
	 * Sets the saveType and adds the file to the recent menu.
	 * @param f
	 * @return whether the operation was successful
	 */
	public boolean openAnalysis(File f, SaveType saveType){
		if (!f.exists() || !f.canRead()) {
			throw new IllegalArgumentException("Can't read " + f.getPath());
		}

		prepareForFileLoad(f, saveType);
	
		loadAnalysis(f);
		return true;
	}
	
	public void openBugCollection(SortedBugCollection bugs){
		
		prepareForFileLoad(null, null);
	
		Project project = bugs.getProject();
		project.setGuiCallback(MainFrame.this);
		BugLoader.addDeadBugMatcher(project);
		setProjectAndBugCollectionInSwingThread(project, bugs);

	}
	
	private void prepareForFileLoad(File f, SaveType saveType) {
	    setRebuilding(true);
		//This creates a new filters and suppressions so don't use the previoues one.
        createProjectSettings();

        clearSourcePane();
		clearSummaryTab();
		comments.setUserCommentInputEnable(false);
		reconfigMenuItem.setEnabled(true);
		setProjectChanged(false);
		this.setSaveType(saveType);
		saveFile = f;

		addFileToRecent(f, saveType);
    }


	private void loadAnalysis(final File file) {

		Runnable runnable = new Runnable() {
			public void run() {

				Project project = new Project();
				project.setGuiCallback(MainFrame.this);
				project.setCurrentWorkingDirectory(file.getParentFile());
				BugLoader.loadBugs(MainFrame.this, project, file);
				project.getSourceFinder(); // force source finder to be initialized
				updateBugTree();
			}
		};
		if (EventQueue.isDispatchThread())
			new Thread(runnable, "Analysis loading thread").start();
		else
			runnable.run();
		return;
	}

	private void loadAnalysis(final URL url) {

		Runnable runnable = new Runnable() {
			public void run() {
				Project project = new Project();
				project.setGuiCallback(MainFrame.this);
				BugLoader.loadBugs(MainFrame.this, project, url);
				project.getSourceFinder(); // force source finder to be initialized
				updateBugTree();
			}
		};
		if (EventQueue.isDispatchThread())
			new Thread(runnable, "Analysis loading thread").start();
		else
			runnable.run();
		return;
	}
	
	/**
	 * @param file
	 * @return
	 */
	private void loadProjectFromFile(final File f) {

		Runnable runnable = new Runnable(){
			public void run()
			{
				final Project  project = BugLoader.loadProject(MainFrame.this, f);
				final BugCollection  bc = project == null ? null : BugLoader.doAnalysis(project);
				updateProjectAndBugCollection(project, bc, null);
				setProjectAndBugCollectionInSwingThread(project, bc);
			}
		};
		if (EventQueue.isDispatchThread())
			new Thread(runnable).start();
		else runnable.run();
		return;
	}
	/**
	 * Redo the analysis
	 */
	void redoAnalysis() {
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		showWaitCard();
		new Thread()
		{
			@Override
			public void run()
			{
				updateDesignationDisplay();
				BugCollection  bc=BugLoader.redoAnalysisKeepComments(getProject());
				updateProjectAndBugCollection(getProject(), bc, null);
			}
		}.start();
	}
	/**
	 * 
	 */
	private void mergeAnalysis() {
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		showWaitCard();
		BugCollection bc=BugLoader.combineBugHistories();
		setBugCollection(bc);

	}

	/**
	 * This takes a directory and opens it as a project.
	 * @param dir
	 */
	private void openProject(final File dir){
		File xmlFile= new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");
		File fasFile=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");

		if (!fasFile.exists())
		{
			JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString(
					"dlg.filter_settings_not_found_lbl", "Filter settings not found, using default settings."));

            createProjectSettings();

        }
		else
		{
			try 
			{
                loadProjectSettings(fasFile);
            } catch (FileNotFoundException e)
			{
				//Impossible.
				if (MainFrame.DEBUG) System.err.println(".fas file not found, using default settings");
                createProjectSettings();
            }
		}
		final File extraFinalReferenceToXmlFile=xmlFile;
		new Thread(new Runnable(){
			public void run()
			{
				try {
	                BugTreeModel.pleaseWait();
	                MainFrame.this.setRebuilding(true);
	                Project project = new Project();
	                project.setGuiCallback(MainFrame.this); // for status msg callbacks during init
	                BugLoader.loadBugs(MainFrame.this, project, extraFinalReferenceToXmlFile);
	                updateBugTree();
                } catch (Exception e) {
                	LOGGER.log(Level.SEVERE, "Error loading project", e);
                }
			}
		}, "GUI analysis loader thread").start();

//		addFileToRecent(xmlFile, SaveType.PROJECT);
		addFileToRecent(dir, SaveType.PROJECT);

		clearSourcePane();
		clearSummaryTab();
		comments.setUserCommentInputEnable(false);

		setProjectChanged(false);
		setSaveType(SaveType.PROJECT);
		saveFile = dir;
		changeTitle();
	}

    @SuppressWarnings({"deprecation"})
    private ProjectSettings getProjectSettings() {
        return ProjectSettings.getInstance();
    }

    @SuppressWarnings({"deprecation"})
    private void loadProjectSettings(File fasFile) throws FileNotFoundException {
        ProjectSettings.loadInstance(new FileInputStream(fasFile));
    }

    @SuppressWarnings({"deprecation"})
    private void createProjectSettings() {
        ProjectSettings.newInstance();
    }

    /**
	 * This checks if the xmlFile is in the GUISaveState. If not adds it. Then adds the file
	 * to the recentMenuCache.
	 * @param xmlFile
	 */
	/*
	 * If the file already existed, its already in the preferences, as well as 
	 * the recent projects menu items, only add it if they change the name, 
	 * otherwise everything we're storing is still accurate since all we're 
	 * storing is the location of the file.
	 */
	private void addFileToRecent(File xmlFile, SaveType st){
		ArrayList<File> xmlFiles=GUISaveState.getInstance().getRecentFiles();
		if (!xmlFiles.contains(xmlFile))
		{
			GUISaveState.getInstance().addRecentFile(xmlFile);
		}
		MainFrame.this.recentMenuCache.addRecentFile(xmlFile);
	}

	private void newProjectMenu() {
		comments.saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);
		new NewProjectWizard();

		newProject = true;
	}
	void updateDesignationDisplay() {
		comments.updateDesignationComboBox();
	}
	void addDesignationItem(JMenu menu, final String menuName,  int keyEvent) {
		comments.addDesignationItem(menu, menuName, keyEvent);
	}

	void warnUserOfFilters()
	{
		JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString("dlg.everything_is_filtered",
				"All bugs in this project appear to be filtered out.  \nYou may wish to check your filter settings in the preferences menu."),
				"Warning",JOptionPane.WARNING_MESSAGE);
	}
	/**
     * @param saveType The saveType to set.
     */
    void setSaveType(SaveType saveType) {
    	if (DEBUG && this.saveType != saveType) 
    		System.out.println("Changing save type from " + this.saveType + " to " + saveType);
	    this.saveType = saveType;
    }
	/**
     * @return Returns the saveType.
     */
    SaveType getSaveType() {
	    return saveType;
    }

    public void showMessageDialogAndWait(final String message) throws InterruptedException {
    	if (SwingUtilities.isEventDispatchThread())
    		JOptionPane.showMessageDialog(this, message);
    	else
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(MainFrame.this, message);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
    }
    
    public void showMessageDialog(final String message) {
    	if (SwingUtilities.isEventDispatchThread()) 
    		JOptionPane.showMessageDialog(this, message);   
    	else
    		SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					JOptionPane.showMessageDialog(MainFrame.this, message);             
                }});
    }
    
    public int showConfirmDialog(String message, String title, int optionType) {
        return JOptionPane.showConfirmDialog(this, message, title, optionType);
    }

    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        return JOptionPane.showOptionDialog(this, message, title,
                                            JOptionPane.OK_CANCEL_OPTION, 
                                            JOptionPane.PLAIN_MESSAGE, null,
                                            new Object[] { ok, cancel }, ok);
    }

    private void setProjectAndBugCollectionInSwingThread(final Project project, final BugCollection bc) {
	    setProjectAndBugCollection(project, bc);
    }
    
    
	private JLabel waitStatusLabel;
	
    public Sortables[] getAvailableSortables() {
    	Sortables[] sortables;
    	ArrayList<Sortables> a = new ArrayList<Sortables>(Sortables.values().length);
    	for(Sortables s : Sortables.values()) 
    		if (s.isAvailable(this))
    			a.add(s);
    	sortables = new Sortables[a.size()];
    	a.toArray(sortables);
    	return sortables;
    }
	public  Sortables[] sortables() {
    	return Sortables.values();
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IGuiCallback#progessMonitoredInputStream(java.io.File, java.lang.String)
     */
  
    
    public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
		ProgressMonitorInputStream pmin = new ProgressMonitorInputStream(this, msg, in);
		ProgressMonitor pm = pmin.getProgressMonitor();

		if (length > 0)
			pm.setMaximum(length);
		return pmin;
	}
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IGuiCallback#showStatus(java.lang.String)
     */
    public void showStatus(String msg) {
	   guiLayout.setSourceTitle(msg);
	    
    }
	
   public void displayNonmodelMessage(String title, String message) {
	  DisplayNonmodelMessage.displayNonmodelMessage(title, message, this, true);
   }
   
  
  public void displayCloudReport() {
	  Cloud cloud = this.bugCollection.getCloud();
		if (cloud == null) {
			JOptionPane.showMessageDialog(this, "There is no cloud");
			return;
		}
	  cloud.waitUntilIssueDataDownloaded();
	  StringWriter stringWriter = new StringWriter();
	  PrintWriter writer = new PrintWriter(stringWriter);
	  cloud.printCloudSummary(writer, getDisplayedBugs(), viewFilter.getPackagePrefixes());
	  writer.close();
	  String report = stringWriter.toString();
	DisplayNonmodelMessage.displayNonmodelMessage("Cloud summary", report, this, false);
	  
  }
	   
   public Iterable<BugInstance> getDisplayedBugs() {
	   return new Iterable<BugInstance>(){

		public Iterator<BugInstance> iterator() {
	       return new ShownBugsIterator();
        }};
   }
   
   class ShownBugsIterator implements Iterator<BugInstance> {
	   Iterator<BugInstance> base = bugCollection.getCollection().iterator();
	   boolean nextKnown;
	   BugInstance next;
	/* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
	   if (!nextKnown) {
		   nextKnown = true;
		   while (base.hasNext()) {
			   next = base.next();
			   if (shouldDisplayIssue(next)) 
				   return true;
		   }
		   next = null;
		   return false;
	   }
	   return next != null;
    }
	/* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public BugInstance next() {
	    if (!hasNext())
	    	throw new NoSuchElementException();
	    BugInstance result = next;
	    next = null;
	    nextKnown = false;
	    return result;
	    
    }
	/* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
	    throw new UnsupportedOperationException();
	    
    }

	   
   }
/* (non-Javadoc)
 * @see edu.umd.cs.findbugs.IGuiCallback#showQuestionDialog(java.lang.String, java.lang.String, java.lang.String)
 */
public String showQuestionDialog(String message, String title, String defaultValue) {
	return (String) JOptionPane.showInputDialog(this, message, title, JOptionPane.QUESTION_MESSAGE, null, null, defaultValue);
}

    public List<String> showForm(String message, String title, List<FormItem> items) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.insets = new Insets(5,5,5,5);
        panel.add(new JLabel(message), gbc);
        gbc.gridwidth = 1;
        for (FormItem item : items) {
            gbc.gridy++;
            panel.add(new JLabel(item.getLabel()), gbc);
            String defaultValue = item.getDefaultValue();
            if (item.getPossibleValues() != null) {
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                JComboBox box = new JComboBox(model);
                item.setField(box);
                for (String possibleValue : item.getPossibleValues()) {
                    model.addElement(possibleValue);
                }
                if (defaultValue == null)
                    model.setSelectedItem(model.getElementAt(0));
                else
                    model.setSelectedItem(defaultValue);
                panel.add(box, gbc);

            } else {
                JTextField field = (item.isPassword() ? new JPasswordField() : new JTextField());
                if (defaultValue != null) {
                    field.setText(defaultValue);
                }
                item.setField(field);
                panel.add(field, gbc);
            }
        }

        int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION)
            return null;
        List<String> results = new ArrayList<String>();
        for (FormItem item : items) {
            JComponent field = item.getField();
            if (field instanceof JTextComponent) {
                JTextComponent textComponent = (JTextComponent) field;
                results.add(textComponent.getText());
            } else if (field instanceof JComboBox) {
                JComboBox box = (JComboBox) field;
                results.add((String) box.getSelectedItem());
            }
        }
        return results;
    }

/* (non-Javadoc)
 * @see edu.umd.cs.findbugs.IGuiCallback#showDocument(java.net.URL)
 */
public boolean showDocument(URL u) {
	return LaunchBrowser.showDocument(u);
}

    private static class EventQueueExecutor extends AbstractExecutorService {
        public void shutdown() {
        }

        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        public boolean isShutdown() {
            return true;
        }

        public boolean isTerminated() {
            return true;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void execute(Runnable command) {
            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
                return;
            }
            try {
                SwingUtilities.invokeAndWait(command);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IGuiCallback#isHeadless()
     */
    public boolean isHeadless() {
	    return false;
    }
	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IGuiCallback#invokeInGUIThread(java.lang.Runnable)
     */
    public void invokeInGUIThread(Runnable r) {
	    SwingUtilities.invokeLater(r);
	    
    }
}
