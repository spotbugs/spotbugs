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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugsDisplayFeatures;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.gui.ConsoleLogger;
import edu.umd.cs.findbugs.gui.LogSync;
import edu.umd.cs.findbugs.gui.Logger;
import edu.umd.cs.findbugs.sourceViewer.NavigableTextPane;

@SuppressWarnings("serial")

/*
 * This is where it all happens... seriously... all of it...
 * All the menus are set up, all the listeners, all the frames, dockable window functionality
 * There is no one style used, no one naming convention, its all just kinda here.  This is another one of those 
 * classes where no one knows quite why it works.
 */
/**
 * The MainFrame is just that, the main application window where just about everything happens.
 */
public class MainFrame extends FBFrame implements LogSync
{
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
	private JButton findButton = newButton("button.find", "Find");
	private JButton findNextButton = newButton("button.findNext", "Find Next");
	private JButton findPreviousButton = newButton("button.findPrev", "Find Previous");

	public static final boolean DEBUG = SystemProperties.getBoolean("gui2.debug");

	private static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");
	final static String WINDOW_MODIFIED = "windowModified";

	NavigableTextPane sourceCodeTextPane = new NavigableTextPane();
	private JScrollPane sourceCodeScrollPane;

	final CommentsArea comments;
	private SorterTableColumnModel sorter;
	private JTableHeader tableheader;
	private JLabel statusBarLabel = new JLabel();

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
	SourceFinder sourceFinder;
	private Object lock = new Object();
	private boolean newProject = false;

	private Class osxAdapter;
	private Method osxPrefsEnableMethod;

	private Logger logger = new ConsoleLogger(this);
	SourceCodeDisplay displayer = new SourceCodeDisplay(this);

	enum SaveType {NOT_KNOWN, PROJECT, XML_ANALYSIS};
	SaveType saveType = SaveType.NOT_KNOWN;
	FBFileChooser saveOpenFileChooser;
	@CheckForNull private File saveFile = null;
	enum SaveReturn {SAVE_SUCCESSFUL, SAVE_IO_EXCEPTION, SAVE_ERROR};
	JMenuItem saveMenuItem = newJMenuItem("menu.save_item", "Save", KeyEvent.VK_S);

	static void makeInstance(FindBugsLayoutManagerFactory factory) {
		if (instance != null) throw new IllegalStateException();
		instance=new MainFrame(factory);
		instance.initializeGUI();
	}
	/**
	 * @param string
	 * @param vkF
	 * @return
	 */

	static MainFrame getInstance() {
		if (instance==null) throw new IllegalStateException();
		return instance;
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
		if(projectChanged){
			int value = JOptionPane.showConfirmDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString("msg.you_are_closing_txt", "You are closing") + " " +
					edu.umd.cs.findbugs.L10N.getLocalString("msg.without_saving_txt", "without saving. Do you want to save?"), 
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

		System.exit(0);
	}

	JMenuItem createRecentItem(final File f, final SaveType localSaveType)
	{
		String name = f.getName();

		if(!f.getName().endsWith(".xml"))
			Debug.println("File does not end with .xml!!");

		if(localSaveType == SaveType.PROJECT && f.getName().endsWith(".xml")){
			name = f.getName().substring(0, f.getName().length()-4);
		}

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
						GUISaveState.getInstance().projectNotFound(f,localSaveType);
						return;
					}
					GUISaveState.getInstance().projectReused(f,localSaveType);//Move to front in GUISaveState, so it will be last thing to be removed from the list

					MainFrame.this.recentMenuCache.addRecentFile(f,localSaveType);

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

					if (localSaveType==SaveType.PROJECT)
					{
						if(false){ //Old stuff still here in case openProject doesn't work.
							saveFile=f.getParentFile();

							File fasFile=new File(saveFile.getAbsolutePath() + File.separator + saveFile.getName() + ".fas");
							try 
							{
								ProjectSettings.loadInstance(new FileInputStream(fasFile));
							} catch (Exception exception) 
							{
								Debug.println("We are in the recent menuitem FileNotFoundException.");
								//Silently make a new instance
								ProjectSettings.newInstance();
							}

						}

						openProject(f.getParentFile());
					}
					else if (localSaveType==SaveType.XML_ANALYSIS)
					{
						if(false){
							saveFile=f;
							ProjectSettings.newInstance();//Just make up new filters and suppressions, cuz he doesn't have any
						}

						openAnalysis(f);
					}

					if(false){
						final File extraFinalReferenceToXmlFile=f;
						new Thread(new Runnable(){
							public void run()
							{
								updateDesignationDisplay();

								BugTreeModel model=(BugTreeModel)tree.getModel();
//								Debug.println("please wait called by open menu item");
								BugTreeModel.pleaseWait();
								MainFrame.this.setRebuilding(true);
								Project newProject = new Project();
								SortedBugCollection bc=BugLoader.loadBugs(MainFrame.this, newProject, extraFinalReferenceToXmlFile);
								setProjectAndBugCollection(newProject, bc);
							}
						}).start();
					}
				}
				finally
				{
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					saveType=localSaveType;
				}
			}
		});
		item.setFont(item.getFont().deriveFont(Driver.getFontSize()));
		return item;
	}

	BugCollection bugCollection;
	@SwingThread
	void setProjectAndBugCollection(Project project, BugCollection bugCollection) {
		// setRebuilding(false);
		if (bugCollection == null) {
			showTreeCard();
		} else {
			curProject = project;
			this.bugCollection = bugCollection;
			displayer.clearCache();

			BugTreeModel model = (BugTreeModel) getTree().getModel();     
			setSourceFinder(new SourceFinder());
			getSourceFinder().setSourceBaseList(project.getSourceDirList());
			BugSet bs = new BugSet(bugCollection);
			model.getOffListenerList();
			model.changeSet(bs);
			if (bs.size() == 0 && bs.sizeUnfiltered() > 0) {
				warnUserOfFilters();
			}
			updateStatusBar();
		}
		setProjectChanged(false);
		reconfigMenuItem.setEnabled(true);
		newProject();
		clearSourcePane();
		clearSummaryTab();

		/* This is here due to a threading issue. It can only be called after
		 * curProject has been changed. Since this method is called by both open methods
		 * it is put here.*/
		changeTitle();
	}

	void updateProjectAndBugCollection(Project project, BugCollection bugCollection, BugTreeModel previousModel) {
		setRebuilding(false);
		if (bugCollection != null)
		{
			displayer.clearCache();
			BugSet bs = new BugSet(bugCollection);
			//Dont clear data, the data's correct, just get the tree off the listener lists.
			((BugTreeModel) tree.getModel()).getOffListenerList();
			((BugTreeModel)tree.getModel()).changeSet(bs);
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
		String name = curProject.getProjectName();
		if(name == null && saveFile != null)
			name = saveFile.getAbsolutePath();
		if(name == null)
			name = Project.UNNAMED_PROJECT;
		MainFrame.this.setTitle(TITLE_START_TXT + name);
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
				new NewFilterFromBug(currentSelectedBugLeaf.getBug());

				setProjectChanged(true);
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
				saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

				StackedFilterMatcher sfm = currentSelectedBugAspects.getStackedFilterMatcher();
				if (!ProjectSettings.getInstance().getSuppressionFilter().contains(sfm))
					ProjectSettings.getInstance().addFilter(sfm);

				setProjectChanged(true);
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
		JMenuItem newProjectMenuItem = newJMenuItem("menu.new_item", "New Project", KeyEvent.VK_N);
		JMenuItem openMenuItem = newJMenuItem("menu.open_item", "Open...", KeyEvent.VK_O);
		recentMenu = newJMenu("menu.recent", "Recent");
		recentMenuCache=new RecentMenu(recentMenu);
		JMenuItem saveAsMenuItem = newJMenuItem("menu.saveas_item", "Save As...", KeyEvent.VK_A);;
		redoAnalysis = newJMenuItem("menu.rerunAnalysis", "Redo Analysis", KeyEvent.VK_R);

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


		attachAcceleratorKey(newProjectMenuItem, KeyEvent.VK_N);

		newProjectMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				newProjectMenu();
			}
		});

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

		redoAnalysis.setEnabled(false);
		attachAcceleratorKey(redoAnalysis, KeyEvent.VK_R);
		redoAnalysis.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				redoAnalysis();
			}
		});

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

		saveMenuItem.setEnabled(false);
		attachAcceleratorKey(saveMenuItem, KeyEvent.VK_S);
		saveMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				save();
			}
		});



		fileMenu.add(newProjectMenuItem);
		fileMenu.add(reconfigMenuItem);
		fileMenu.addSeparator();

		fileMenu.add(openMenuItem);
		fileMenu.add(recentMenu);
		fileMenu.addSeparator();
		fileMenu.add(saveAsMenuItem);
		fileMenu.add(saveMenuItem);

		fileMenu.addSeparator();
		fileMenu.add(redoAnalysis);
		// fileMenu.add(mergeMenuItem);

		//TODO: This serves no purpose but to test something
		if(false){
			JMenuItem temp = new JMenuItem("Temp");
			temp.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					System.out.println("Current Project Name: " + curProject.getProjectName());
				}			
			});
			fileMenu.add(temp);
		}

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
		if (true || !MAC_OS_X) {
			// Preferences goes in Findbugs menu and is handled by OSXAdapter
			editMenu.add(preferencesMenuItem);
		}
		editMenu.add(sortMenuItem);

		menuBar.add(editMenu);

		if (windowMenu != null)
			menuBar.add(windowMenu);

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
	void newProject(){
		clearSourcePane();
		redoAnalysis.setEnabled(true);

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
	 * This method is for when the user wants to open a project or analysis.
	 *
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
		while (loading)
		{
			int value=saveOpenFileChooser.showOpenDialog(MainFrame.this);
			if(value!=JFileChooser.APPROVE_OPTION) return;

			loading = false;
			fileType = convertFilterToType(saveOpenFileChooser.getFileFilter());
			final File f = saveOpenFileChooser.getSelectedFile();

			if(fileType == SaveType.PROJECT){
				File xmlFile= new File(f.getAbsolutePath() + File.separator + f.getName() + ".xml");		

				if (!xmlFile.exists())
				{
					JOptionPane.showMessageDialog(saveOpenFileChooser, edu.umd.cs.findbugs.L10N.getLocalString("dlg.no_xml_data_lbl", "This directory does not contain saved bug XML data, please choose a different directory."));
					loading=true;
					continue;
				}

				openProject(f);
			}
			else if(fileType == SaveType.XML_ANALYSIS){
				if(!f.getName().endsWith(".xml")){
					JOptionPane.showMessageDialog(saveOpenFileChooser, edu.umd.cs.findbugs.L10N.getLocalString("dlg.not_xml_data_lbl", "This is not a saved bug XML data file."));
					loading=true;
					continue;
				}

				if(!openAnalysis(f)){
					//TODO: Deal if something happens when loading analysis
					JOptionPane.showMessageDialog(saveOpenFileChooser, "An error occurred while trying to load the analysis.");
					loading=true;
					continue;
				}
			}
		}
	}

	private boolean saveAs(){
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

			alreadyExists = fileAlreadyExists(f, fileType);
			if(alreadyExists){
				int response = -1;

				if(fileType == SaveType.XML_ANALYSIS){
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.analysis_exists_lbl", "This analysis already exists.\nReplace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				}
				else if(fileType == SaveType.PROJECT){
					response = JOptionPane.showConfirmDialog(saveOpenFileChooser, 
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.proj_already_exists_lbl", "This project already exists.\nDo you want to replace it?"),
							edu.umd.cs.findbugs.L10N.getLocalString("dlg.warning_ttl", "Warning!"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				}

				if(response == JOptionPane.OK_OPTION)
					retry = false;
				if(response == JOptionPane.CANCEL_OPTION){
					retry = true;
					continue;
				}

			}

			SaveReturn successful = SaveReturn.SAVE_ERROR;
			if(fileType == SaveType.XML_ANALYSIS)
				successful = saveAnalysis(f);
			else if(fileType == SaveType.PROJECT)
				successful = saveProject(f);

			if (successful != SaveReturn.SAVE_SUCCESSFUL)
			{
				JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString("dlg.saving_error_lbl", "An error occurred in saving."));
				return false;
			}
		}

//		saveProjectMenuItem.setEnabled(false);
		saveMenuItem.setEnabled(false);
		saveType = fileType;
		saveFile = f;
		File xmlFile;
		if (saveType==SaveType.PROJECT)
			xmlFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".xml");
		else
			xmlFile=f;

		addFileToRecent(xmlFile, saveType);

		return true;
	}

	/*
	 * Returns SaveType equivalent depending on what kind of FileFilter passed.
	 */
	private	SaveType convertFilterToType(FileFilter f){
		String des = f.getDescription();
		if(FindBugsAnalysisFileFilter.INSTANCE.getDescription().equals(des))
			return SaveType.XML_ANALYSIS;

		if(FindBugsProjectFileFilter.INSTANCE.getDescription().equals(des))
			return SaveType.PROJECT;

		return SaveType.NOT_KNOWN;
	}

	/*
	 * Depending on File and SaveType determines if f already exists. Returns false if not
	 * exist, true otherwise. For a project if either the .xml or .fas file already exists
	 * will return true.
	 */
	private boolean fileAlreadyExists(File f, SaveType fileType){
		if(fileType == SaveType.XML_ANALYSIS && f.exists())
			return true;

		if(fileType == SaveType.PROJECT){
			File xmlFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".xml");
			File fasFile=new File(f.getAbsolutePath() + File.separator + f.getName() + ".fas");
			return xmlFile.exists() || fasFile.exists();
		}

		return false;
	}

	/*
	 * If f is not of type FileType will convert file so it is.
	 */
	private File convertFile(File f, SaveType fileType){
		if(fileType == SaveType.XML_ANALYSIS && !f.getName().endsWith(".xml"))
			f = new File(f.getAbsolutePath()+".xml");

		//This assumes the filefilter for project is working so f can only be a directory.
		if(fileType == SaveType.PROJECT)
			return f;

		return f;
	}

	private void save(){
		SaveReturn result = SaveReturn.SAVE_ERROR;
		if(saveType == SaveType.PROJECT){
			result = saveProject(saveFile);
		}
		else if(saveType == SaveType.XML_ANALYSIS){
			result = saveAnalysis(saveFile);
		}

		if(result != SaveReturn.SAVE_SUCCESSFUL){
			JOptionPane.showMessageDialog(MainFrame.this, edu.umd.cs.findbugs.L10N.getLocalString(
					"dlg.saving_error_lbl", "An error occurred in saving."));
		}
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
	/**
	 * 
	 * @return
	 */
	JPanel bugListPanel()
	{
		cardPanel = new JPanel(new CardLayout());

		JPanel topPanel = new JPanel();
		waitPanel = new JPanel();
		waitPanel.add(new JLabel("Please wait..."));
		cardPanel.add(topPanel, TREECARD);
		cardPanel.add(waitPanel, WAITCARD);

		topPanel.setMinimumSize(new Dimension(200,200));
		tableheader = new JTableHeader();
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
		curProject= new Project();


		treeScrollPane = new JScrollPane(tree);
		topPanel.setLayout(new BorderLayout());

		//New code to fix problem in Windows
		JTable t = new JTable(new DefaultTableModel(0, Sortables.values().length));
		t.setTableHeader(tableheader);
		JScrollPane sp = new JScrollPane(t);
		//This sets the height of the scrollpane so it is dependent on the fontsize.
		int num = (int) (Driver.getFontSize()*1.2);
		sp.setPreferredSize(new Dimension(0, 10+num));
		//End of new code.
		//Changed code.
		topPanel.add(sp, BorderLayout.NORTH);
		//topPanel.add(tableheader, BorderLayout.NORTH);
		//End of changed code.
		topPanel.add(treeScrollPane, BorderLayout.CENTER);

		return cardPanel;
	}

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
				Container container = treeScrollPane.getParent();

				container.remove(treeScrollPane);
				treeScrollPane = new JScrollPane(newTree);
				container.add(treeScrollPane, BorderLayout.CENTER);
				setFontSizeHelper(container.getComponents(), Driver.getFontSize());
				tree.setRowHeight((int)(Driver.getFontSize() + 7));
				MainFrame.getInstance().getContentPane().validate();
				MainFrame.getInstance().getContentPane().repaint();

				setupTreeListeners();
				newModel.openPreviouslySelected(((BugTreeModel)(tree.getModel())).getOldSelectedBugs());
				MainFrame.this.getSorter().addColumnModelListener(newModel);
				FilterActivity.addFilterListener(newModel.bugTreeFilterListener);
				MainFrame.this.setSorting(true);

			}
		});
	}

	private void setupTreeListeners()
	{
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
			comments.updateCommentsFromLeafInformation(currentSelectedBugLeaf);
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
				setSourceTabTitle("Source");				
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
		statusBar.setLayout(new BorderLayout());
		statusBar.add(statusBarLabel,BorderLayout.WEST);

		JLabel logoLabel = new JLabel();

		ImageIcon logoIcon = new ImageIcon(MainFrame.class.getResource("logo_umd.png"));
		logoLabel.setIcon(logoIcon);
		statusBar.add(logoLabel, BorderLayout.EAST);

		return statusBar;
	}

	void updateStatusBar()
	{

		int countFilteredBugs = BugSet.countFilteredBugs();
		if (countFilteredBugs == 0)
			statusBarLabel.setText("  http://findbugs.sourceforge.net/");
		else if (countFilteredBugs == 1)
			statusBarLabel.setText("  1 " + edu.umd.cs.findbugs.L10N.getLocalString("statusbar.bug_hidden", "bug hidden"));
		else 
			statusBarLabel.setText("  " + countFilteredBugs + " " + edu.umd.cs.findbugs.L10N.getLocalString("statusbar.bugs_hidden", "bugs hidden"));
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
	private Component bugSummaryComponent(Object obj, BugInstance bug){
		JLabel label = new JLabel();
		label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		label.setForeground(Color.BLACK);

		if(obj instanceof String){
			String str = (String) obj;
			label.setText(str);
		}
		else{

			BugAnnotation value = (BugAnnotation) obj;

			if(value == null)
				return new JLabel(edu.umd.cs.findbugs.L10N.getLocalString("summary.null", "null"));

			if(value instanceof SourceLineAnnotation){
				final SourceLineAnnotation note = (SourceLineAnnotation) value;
				if(sourceCodeExist(note)){
					String srcStr = "";
					int start = note.getStartLine();
					int end = note.getEndLine();
					if(start < 0 && end < 0)
						srcStr = edu.umd.cs.findbugs.L10N.getLocalString("summary.source_code", "source code.");
					else if(start == end)
						srcStr = " [" + edu.umd.cs.findbugs.L10N.getLocalString("summary.line", "Line") + " " + start + "]";
					else if(start < end)
						srcStr = " [" + edu.umd.cs.findbugs.L10N.getLocalString("summary.lines", "Lines") + " " + start + " - " + end + "]";

					label.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.click_to_go_to", "Click to go to") + " " + srcStr);

					label.addMouseListener(new BugSummaryMouseListener(bug, label, note));
				}

				label.setText(note.toString());
			}
			else if(value instanceof PackageMemberAnnotation){
				PackageMemberAnnotation note = (PackageMemberAnnotation) value;
				final SourceLineAnnotation noteSrc = note.getSourceLines();
				String srcStr = "";
				if(sourceCodeExist(noteSrc) && noteSrc != null){
					int start = noteSrc.getStartLine();
					int end = noteSrc.getEndLine();
					if(start < 0 && end < 0)
						srcStr = edu.umd.cs.findbugs.L10N.getLocalString("summary.source_code", "source code.");
					else if(start == end)
						srcStr = " [" + edu.umd.cs.findbugs.L10N.getLocalString("summary.line", "Line") + " " + start + "]";
					else if(start < end)
						srcStr = " [" + edu.umd.cs.findbugs.L10N.getLocalString("summary.lines", "Lines") + " " + start + " - " + end + "]";

					if(!srcStr.equals("")){
						label.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.click_to_go_to", "Click to go to") + " " + srcStr);
						label.addMouseListener(new BugSummaryMouseListener(bug, label, noteSrc));
					}
				}
				if(!srcStr.equals(edu.umd.cs.findbugs.L10N.getLocalString("summary.source_code", "source code.")))
					label.setText(note.toString() + srcStr);
				else
					label.setText(note.toString());
			}
			else{
				label.setText(((BugAnnotation) value).toString());
			}
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

			guiLayout.initialize();
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
			String loadFromURL = SystemProperties.getProperty("findbugs.loadBugsFromURL");

			if (loadFromURL != null) {
				InputStream in;
				try {
					in = new URL(loadFromURL).openConnection().getInputStream();
					if (loadFromURL.endsWith(".gz"))
						in = new GZIPInputStream(in);
					BugTreeModel.pleaseWait(edu.umd.cs.findbugs.L10N.getLocalString("msg.loading_bugs_over_network_txt", "Loading bugs over network..."));
					loadAnalysisFromInputStream(in);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, "Error loading "  +e1.getMessage());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					JOptionPane.showMessageDialog(MainFrame.this, "Error loading "  +e1.getMessage());
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
		}
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
		private BugInstance bugInstance;
		private JLabel label;
		private SourceLineAnnotation note;

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
	private boolean sourceCodeExist(SourceLineAnnotation note){
		try{
			sourceFinder.findSourceFile(note);
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
	 */
	 void setSourceTabTitle(String title){
		guiLayout.setSourceTitle(title);

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
		setFontSizeHelper(bugPopupMenu.getComponents(), size);

		branchPopupMenu.setFont(branchPopupMenu.getFont().deriveFont(size));
		setFontSizeHelper(branchPopupMenu.getComponents(), size);

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

	public Project getProject() {
		return curProject;
	}
	public void setProject(Project p) {
		curProject=p;
	}

	public SourceFinder getSourceFinder() 
	{
		return sourceFinder;
	}

	public void setSourceFinder(SourceFinder sf)
	{
		sourceFinder=sf;
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

	/**
	 * Called when something in the project is changed and the change needs to be saved.
	 * This method should be called instead of using projectChanged = b.
	 */
	public void setProjectChanged(boolean b){
		if(curProject == null)
			return;

		if(projectChanged == b)
			return;

		if(saveFile != null && saveFile.exists())
			saveMenuItem.setEnabled(b);
//		if(projectDirectory != null && projectDirectory.exists())
//			saveProjectMenuItem.setEnabled(b);

		getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.valueOf(b));

		projectChanged = b;
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
		if (curProject == null) {
			curProject = new Project(); 
			JOptionPane.showMessageDialog(MainFrame.this, "Null project; this is unexpected. "
					+" Creating a new Project so the bugs can be saved, but please report this error.");

		}

//		Saves current comment to current bug.
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		dir.mkdir();
		//updateDesignationDisplay(); - Don't think we need this anymore - Kristin

		File f = new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");	
		File filtersAndSuppressions=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");

		BugSaver.saveBugs(f,bugCollection,curProject);
		try {
			filtersAndSuppressions.createNewFile();
			ProjectSettings.getInstance().save(new FileOutputStream(filtersAndSuppressions));
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
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		BugSaver.saveBugs(f, bugCollection, curProject);

		setProjectChanged(false);

		return SaveReturn.SAVE_SUCCESSFUL;
	}

	/**
	 * Opens the analysis. Also clears the source and summary panes. Makes comments enabled false.
	 * Sets the saveType and adds the file to the recent menu.
	 * @param f
	 * @return
	 */
	private boolean openAnalysis(File f){
		try {
			FileInputStream in = new FileInputStream(f);
			loadAnalysisFromInputStream(in);

			//This creates a new filters and suppressions so don't use the previoues one.
			ProjectSettings.newInstance();

			clearSourcePane();
			clearSummaryTab();
			comments.setUserCommentInputEnable(false);
			reconfigMenuItem.setEnabled(true);
			setProjectChanged(false);
			saveType = SaveType.XML_ANALYSIS;
			saveFile = f;
			changeTitle();

			addFileToRecent(f, SaveType.XML_ANALYSIS);

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * @param file
	 * @return
	 */
	@SwingThread
	private void loadAnalysisFromInputStream(final InputStream in) {
		// showWaitCard();
		MainFrame.this.setRebuilding(true);
		new Thread(new Runnable(){
			public void run()
			{

				final Project project = new Project();
				final SortedBugCollection bc=BugLoader.loadBugs(MainFrame.this, project, in);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setProjectAndBugCollection(project, bc);
					}});
			}
		}).start();
		return;
	}

	/**
	 * Redo the analysis
	 */
	private void redoAnalysis() {
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		showWaitCard();
		new Thread()
		{
			@Override
			public void run()
			{
				updateDesignationDisplay();
				BugCollection  bc=BugLoader.redoAnalysisKeepComments(curProject);
				updateProjectAndBugCollection(curProject, bc, null);
			}
		}.start();
	}
	/**
	 * 
	 */
	private void mergeAnalysis() {
		saveComments(currentSelectedBugLeaf, currentSelectedBugAspects);

		showWaitCard();
		Project p  = new Project();
		BugCollection bc=BugLoader.combineBugHistories(p);
		setProjectAndBugCollection(p, bc);

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

			ProjectSettings.newInstance();

		} 
		else
		{
			try 
			{
				ProjectSettings.loadInstance(new FileInputStream(fasFile));
			} catch (FileNotFoundException e) 
			{
				//Impossible.
				if (MainFrame.DEBUG) System.err.println(".fas file not found, using default settings");
				ProjectSettings.newInstance();
			}
		}
		final File extraFinalReferenceToXmlFile=xmlFile;
		new Thread(new Runnable(){
			public void run()
			{
				BugTreeModel.pleaseWait();
				MainFrame.this.setRebuilding(true);
				Project project = new Project();
				SortedBugCollection bc=BugLoader.loadBugs(MainFrame.this, project, extraFinalReferenceToXmlFile);
				setProjectAndBugCollection(project, bc);
			}
		}).start();

		addFileToRecent(xmlFile, SaveType.PROJECT);

		clearSourcePane();
		clearSummaryTab();
		comments.setUserCommentInputEnable(false);

		setProjectChanged(false);
		saveType = SaveType.PROJECT;
		saveFile = dir;
		changeTitle();
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
		ArrayList<File> xmlFiles=GUISaveState.getInstance().getRecentProjects();
		if (!xmlFiles.contains(xmlFile))
		{
			GUISaveState.getInstance().addRecentFile(xmlFile, st);
		}
		MainFrame.this.recentMenuCache.addRecentFile(xmlFile,st);
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
}
