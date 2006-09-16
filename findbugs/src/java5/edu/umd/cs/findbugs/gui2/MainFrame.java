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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
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
	private JTree tree;
	private BasicTreeUI treeUI;
	private boolean userInputEnabled;
		
	static final String DEFAULT_SOURCE_CODE_MSG = "No available source";
	
	static final int COMMENTS_TAB_STRUT_SIZE = 5;
	static final int COMMENTS_MARGIN = 5;

	public static final boolean DEBUG = SystemProperties.getBoolean("gui2.debug");
	
	NavigableTextPane sourceCodeTextPane = new NavigableTextPane();
	private JScrollPane sourceCodeScrollPane;
	
	private JTextArea userCommentsText = new JTextArea();
	private Color userCommentsTextUnenabledColor;
	private boolean commentChanged = false;
	private boolean designationChanged=false;
	private JComboBox designationComboBox;
	private ArrayList<String> designationList;
	private LinkedList<String> prevCommentsList = new LinkedList<String>();
	private int prevCommentsMaxSize = 10;
	private JComboBox prevCommentsComboBox = new JComboBox();
	
	private SorterTableColumnModel sorter;
	private JTableHeader tableheader;
	private JLabel statusBarLabel = new JLabel();
	
	private JPanel summaryTopPanel;
	private final HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
	private final JEditorPane summaryHtmlArea = new JEditorPane();
	private JScrollPane summaryHtmlScrollPane = new JScrollPane(summaryHtmlArea);
	

	private FindBugsLayoutManagerFactory findBugsLayoutManagerFactory;
	private FindBugsLayoutManager guiLayout;
	
	/* To change this method must use setProjectChanged(boolean b).
	 * This is because saveProjectItemMenu is dependent on it for when
	 * saveProjectMenuItem should be enabled.
	 */
	private boolean projectChanged = false;
	final private JMenuItem editProjectMenuItem = new JMenuItem("Add/Remove Files", KeyEvent.VK_F);
	final private JMenuItem saveProjectMenuItem = new JMenuItem("Save Project", KeyEvent.VK_S);
	private BugLeafNode currentSelectedBugLeaf;
	private BugAspects currentSelectedBugAspects;
	private JPopupMenu bugPopupMenu;
	private JPopupMenu branchPopupMenu;
	private static MainFrame instance;
	private JMenu recentProjectsMenu;
	private JMenuItem preferencesMenuItem;
	private File projectDirectory;
	private Project curProject;
	private JScrollPane treeScrollPane;
	SourceFinder sourceFinder;
	private SourceLineAnnotation currSrcLineAnnotation;
	private Object lock = new Object();
	private boolean newProject = false;
	private Logger logger = new ConsoleLogger(this);
	SourceCodeDisplay displayer = new SourceCodeDisplay(this);
	
	static void makeInstance(FindBugsLayoutManagerFactory factory) {
		if (instance != null) throw new IllegalStateException();
		instance=new MainFrame(factory);
	}
	static MainFrame getInstance() {
		if (instance==null) throw new IllegalStateException();
		return instance;
	}
	
	
	private MainFrame(FindBugsLayoutManagerFactory factory)
	{
		this.findBugsLayoutManagerFactory = factory;
		this.guiLayout = factory.getInstance(this);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				setTitle("FindBugs: " + Project.UNNAMED_PROJECT);
				
				guiLayout.initialize();
				bugPopupMenu = createBugPopupMenu();
				branchPopupMenu = createBranchPopUpMenu();
				loadPrevCommentsList(GUISaveState.getInstance().getPreviousComments().toArray(new String[GUISaveState.getInstance().getPreviousComments().size()]));
				updateStatusBar();
				setBounds(GUISaveState.getInstance().getFrameBounds()); 
				Toolkit.getDefaultToolkit().setDynamicLayout(true);
				setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				setJMenuBar(createMainMenuBar());
				setVisible(true);
				
				
				if (SystemProperties.getProperty("os.name").startsWith("Mac"))
				{
					 try {
						Class osxAdapter = Class.forName("edu.umd.cs.findbugs.gui2.OSXAdapter");
						Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", MainFrame.class);
						if (registerMethod != null) {
							registerMethod.invoke(osxAdapter, MainFrame.this);
						}
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
						BugTreeModel.pleaseWait("Loading bugs over network...");
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
					public void componentResized(ComponentEvent e){
						resetPrevCommentsComboBox();
						userCommentsText.validate();
					}
				});
				
				addWindowListener(new WindowAdapter(){
					public void windowClosing(WindowEvent e) {
						if(userCommentsText.hasFocus())
							setProjectChanged(true);
						callOnClose();
					}				
				});
				Driver.removeSplashScreen();
			}
		});
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
	 * This method is called when the application is closing. This is either by
	 * the exit menuItem or by clicking on the window's system menu.
	 */
	void callOnClose(){
		saveCommentsToBug(currentSelectedBugLeaf);
		if(projectChanged){
			int value = JOptionPane.showConfirmDialog(MainFrame.this, "You are closing " +
					"without saving. Do you want to save?", 
					"Do you want to save?", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			
			if(value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION)
				return ;
			else if(value == JOptionPane.YES_OPTION){
				if(projectDirectory == null){
					if(!projectSaveAs())
						return ;
				}
				else
					save(projectDirectory);
			}				
		}

		GUISaveState.getInstance().setPreviousComments(prevCommentsList);
		guiLayout.saveState();
		GUISaveState.getInstance().setFrameBounds( getBounds() );
		GUISaveState.getInstance().save();
		
		System.exit(0);
	}

	private void createRecentProjectsMenu(){
		for (File p: GUISaveState.getInstance().getRecentProjects())
		{
			addRecentProjectToMenu(p);
		}
	}
	
	private void addRecentProjectToMenu(final File f)
	{
		if (!f.exists())
		{
			if (MainFrame.DEBUG) System.err.println("a recent project was not found, removing it from menu");
			return;
		}
		final JMenuItem item=new JMenuItem(f.getName().substring(0,f.getName().length()-4));
		item.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					setCursor(new Cursor(Cursor.WAIT_CURSOR));

					if (!f.exists())
					{
						JOptionPane.showMessageDialog(null,"This project can no longer be found");
						GUISaveState.getInstance().projectNotFound(f);
						return;
					}
					GUISaveState.getInstance().projectReused(f);//Move to front in GUISaveState, so it will be last thing to be removed from the list
					//Move to front of recent projects menu, so GUISaveState matches with the project menu seen by the user
					boolean exists=false;
					for (int x=0; x< recentProjectsMenu.getItemCount(); x++)
					{
						if (item.getText().equalsIgnoreCase(recentProjectsMenu.getItem(x).getText()))
						{
							exists=true;
							recentProjectsMenu.remove(x);
							recentProjectsMenu.insert(item, 0);//Move to front
						}
					}
					if (!exists)
						throw new IllegalStateException ("User used a recent projects menu item that didn't exist.");
					
					projectDirectory=f.getParentFile();
					File fasFile=new File(projectDirectory.getAbsolutePath() + File.separator + projectDirectory.getName() + ".fas");
					try 
					{
						ProjectSettings.loadInstance(new FileInputStream(fasFile));
					} catch (FileNotFoundException exception) 
					{
						//Silently make a new instance
						ProjectSettings.newInstance();
					}
					
					final File extraFinalReferenceToXmlFile=f;
					new Thread(new Runnable(){
						public void run()
						{
							updateDesignation();
							if (curProject != null && projectChanged)
							{
								int response = JOptionPane.showConfirmDialog(MainFrame.this, 
										"The current project has been changed, Save current changes?"
										,"Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

								if (response == JOptionPane.YES_OPTION)
								{
									if (projectDirectory!=null)
										save(projectDirectory);
									else
										projectSaveAs();
								}
								else if (response == JOptionPane.CANCEL_OPTION)
									return;
								//IF no, do nothing.
							}							
							BugTreeModel model=(BugTreeModel)tree.getModel();
//							Debug.println("please wait called by open menu item");
							BugTreeModel.pleaseWait();
							MainFrame.this.setRebuilding(true);
							Project newProject = new Project();
							BugSet bs=BugLoader.loadBugs(newProject, extraFinalReferenceToXmlFile);
							MainFrame.this.setRebuilding(false);
							if (bs!=null)
							{
								displayer.clearCache();
								model.getOffListenerList();
								model.changeSet(bs);
								curProject=newProject;
								MainFrame.getInstance().updateStatusBar();
								MainFrame.this.setTitle("FindBugs: " + curProject.getProjectFileName());
							}
							
							
							
							setProjectChanged(false);
							editProjectMenuItem.setEnabled(true);
							clearIndividualBugInformation();
						}
					}).start();
				}
				finally
				{
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
		item.setFont(item.getFont().deriveFont(Driver.getFontSize()));
		
		boolean exists=false;
		for (int x=0; x< recentProjectsMenu.getItemCount(); x++)
		{
			if (item.getText().equalsIgnoreCase(recentProjectsMenu.getItem(x).getText()))
			{
				exists=true;
				recentProjectsMenu.remove(x);
				recentProjectsMenu.insert(item, 0);//Move to front
			}
		}
		if (!exists)
			recentProjectsMenu.insert(item,0);		
	}

	/**
	 * Creates popup menu for bugs on tree.
	 * @return
	 */
	private JPopupMenu createBugPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		
		JMenuItem suppressMenuItem = new JMenuItem("Suppress this bug");
		
		suppressMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){				
				saveCommentsToBug(currentSelectedBugLeaf);
				//This MUST be done in this order:
				//getIndexOfChild relies on the fact that things have not yet been removed from the tree!
				TreePath path=tree.getSelectionPath();
				FilterMatcher.notifyListeners(FilterListener.SUPPRESSING, path);
				ProjectSettings.getInstance().getSuppressionMatcher().add(currentSelectedBugLeaf.getBug());						
				PreferencesFrame.getInstance().suppressionsChanged(currentSelectedBugLeaf);
				((BugTreeModel)(tree.getModel())).resetData();//Necessary to keep suppressions from getting out of sync with tree.  
				clearIndividualBugInformation();
				updateStatusBar();
				
				setProjectChanged(true);
			}
		});
		
		popupMenu.add(suppressMenuItem);
		
		JMenuItem filterMenuItem = new JMenuItem("Filter bugs like this");
		
		filterMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				new NewFilterFromBug(currentSelectedBugLeaf.getBug());
				
				setProjectChanged(true);
			}
		});
		
		popupMenu.add(filterMenuItem);
		
		return popupMenu;
	}
	
	/**
	 * Creates the branch pup up menu that ask if the user wants 
	 * to hide all the bugs in that branch.
	 * @return
	 */
	private JPopupMenu createBranchPopUpMenu(){
		JPopupMenu popupMenu = new JPopupMenu();
		
		JMenuItem filterMenuItem = new JMenuItem("Filter these bugs");
		
		filterMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				saveCommentsToBug(currentSelectedBugLeaf);
				
				FilterMatcher[] filters = new FilterMatcher[currentSelectedBugAspects.size()];
				for (int i = 0; i < filters.length; i++)
					filters[i] = new FilterMatcher(currentSelectedBugAspects.get(i));
				StackedFilterMatcher sfm = new StackedFilterMatcher(filters);
				if (!ProjectSettings.getInstance().getAllMatchers().contains(sfm))
					ProjectSettings.getInstance().addFilter(sfm);
				
				setProjectChanged(true);
			}
		});
		
		popupMenu.add(filterMenuItem);
		
		return popupMenu;
	}

	/**
	 * Creates the MainFrame's menu bar.
	 * @return
	 */
	protected JMenuBar createMainMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		//Create JMenus for menuBar.
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		
		//Edit fileMenu JMenu object.
		JMenuItem newProjectMenuItem = new JMenuItem("New Project", KeyEvent.VK_N);
		JMenuItem openProjectMenuItem = new JMenuItem("Open Project...", KeyEvent.VK_O);
		recentProjectsMenu = new JMenu("Recent Projects");
		recentProjectsMenu.setMnemonic(KeyEvent.VK_E);
		createRecentProjectsMenu();
		JMenuItem saveAsProjectMenuItem = new JMenuItem("Save Project As...", KeyEvent.VK_A);
		JMenuItem importBugsMenuItem = new JMenuItem("Load Analysis...", KeyEvent.VK_L);
		JMenuItem exportBugsMenuItem = new JMenuItem("Save Analysis...", KeyEvent.VK_B);
		JMenuItem redoAnalysis = new JMenuItem("Redo Analysis", KeyEvent.VK_R);
		JMenuItem mergeMenuItem = new JMenuItem("Merge Analysis...");
		
		JMenuItem exitMenuItem = null;
		if (!System.getProperty("os.name").startsWith("Mac")) {
			exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
			exitMenuItem.addActionListener(new ActionListener(){			
			public void actionPerformed(ActionEvent evt){
				callOnClose();
			}
			});
		}
		JMenu windowMenu = guiLayout.createWindowMenu();

		
		attachAccelaratorKey(newProjectMenuItem, KeyEvent.VK_N);
		
		newProjectMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				newProjectMenu();
			}
		});
		
		attachAccelaratorKey(editProjectMenuItem, KeyEvent.VK_F);
		editProjectMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				saveCommentsToBug(currentSelectedBugLeaf);
				new NewProjectWizard(curProject);
			}
		});
		
		openProjectMenuItem.setEnabled(true);
		attachAccelaratorKey(openProjectMenuItem, KeyEvent.VK_O);
		openProjectMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				openProject();
			}
		});

		mergeMenuItem.setEnabled(true);
		mergeMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				mergeAnalysis();
			}
		});
		
		redoAnalysis.setEnabled(true);
		attachAccelaratorKey(redoAnalysis, KeyEvent.VK_R);
		redoAnalysis.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				redoAnalysis();
			}
		});
		
		saveProjectMenuItem.setEnabled(false);
		attachAccelaratorKey(saveProjectMenuItem, KeyEvent.VK_S);
		saveProjectMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveCommentsToBug(currentSelectedBugLeaf);
				
				save(projectDirectory);
			}
		});
		
		saveAsProjectMenuItem.setEnabled(true);
		saveAsProjectMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveCommentsToBug(currentSelectedBugLeaf);
				
				if(projectSaveAs())
					saveProjectMenuItem.setEnabled(true);
			}
		});
		
		importBugsMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				loadAnalysis();
			}
		});
		
		exportBugsMenuItem.setEnabled(true);
		exportBugsMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveAnalysis();

			}
		});

		
				
		fileMenu.add(newProjectMenuItem);
		fileMenu.add(editProjectMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(openProjectMenuItem);
		fileMenu.add(recentProjectsMenu);
		fileMenu.addSeparator();
		fileMenu.add(saveProjectMenuItem);
		fileMenu.add(saveAsProjectMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(importBugsMenuItem);
		fileMenu.add(exportBugsMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(redoAnalysis);
		fileMenu.add(mergeMenuItem);
		if (exitMenuItem != null) {
			fileMenu.addSeparator();
			fileMenu.add(exitMenuItem);
		}
				
		menuBar.add(fileMenu);
		
		//Edit editMenu Menu object.
		JMenuItem cutMenuItem = new JMenuItem(new CutAction());
		JMenuItem copyMenuItem = new JMenuItem(new CopyAction());
		JMenuItem pasteMenuItem = new JMenuItem(new PasteAction());
		preferencesMenuItem = new JMenuItem("Preferences");
		JMenuItem sortMenuItem = new JMenuItem("Sort Configuration...");
		JMenuItem goToLineMenuItem = new JMenuItem("Go to line...");
		
		attachAccelaratorKey(cutMenuItem, KeyEvent.VK_X);
		attachAccelaratorKey(copyMenuItem, KeyEvent.VK_C);
		attachAccelaratorKey(pasteMenuItem, KeyEvent.VK_V);
		
		preferencesMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveCommentsToBug(currentSelectedBugLeaf);
				PreferencesFrame.getInstance().setVisible(true);
			}
		});
		
		sortMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveCommentsToBug(currentSelectedBugLeaf);
				SorterDialog.getInstance().setVisible(true);
			}
		});
		
		attachAccelaratorKey(goToLineMenuItem, KeyEvent.VK_L);
		goToLineMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){				
				guiLayout.makeSourceVisible();
				try{
					int num = Integer.parseInt(JOptionPane.showInputDialog(MainFrame.this, "", "Go To Line:", JOptionPane.QUESTION_MESSAGE));
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
		editMenu.add(preferencesMenuItem);
		editMenu.add(sortMenuItem);
		
		menuBar.add(editMenu);
		
		if (windowMenu != null)
			menuBar.add(windowMenu);
		
		final ActionMap map = tree.getActionMap();
		
		JMenu navMenu = new JMenu("Navigation");
		
		addNavItem(map, navMenu, "Expand", "expand", KeyEvent.VK_RIGHT );
		addNavItem(map, navMenu, "Collapse", "collapse", KeyEvent.VK_LEFT);
		addNavItem(map, navMenu, "Up", "selectPrevious", KeyEvent.VK_UP );
		addNavItem(map, navMenu, "Down", "selectNext", KeyEvent.VK_DOWN);
				
		menuBar.add(navMenu);
		
		JMenu designationMenu = new JMenu("Designation");
		int i = 0;
		int keyEvents [] = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
		for(String key :  I18N.instance().getUserDesignationKeys(true)) {
			String name = I18N.instance().getUserDesignation(key);
			addDesignationItem(designationMenu, name, keyEvents[i++]);
		}
		menuBar.add(designationMenu);
		
		
		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem("About FindBugs");
		helpMenu.add(aboutItem);

         aboutItem.addActionListener(new java.awt.event.ActionListener() {
             public void actionPerformed(java.awt.event.ActionEvent evt) {
                 about();
             }
         });
         menuBar.add(helpMenu);
		return menuBar;
	}
	/**
	 * @param map
	 * @param navMenu
	 */
	private void addNavItem(final ActionMap map, JMenu navMenu, String menuName, String actionName, int keyEvent) {
		JMenuItem toggleItem = new JMenuItem(menuName);
		toggleItem.addActionListener(treeActionAdapter(map, actionName));	
		attachAccelaratorKey(toggleItem, keyEvent);
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
	void attachAccelaratorKey(JMenuItem item, int keystroke) {
		item.setAccelerator(KeyStroke.getKeyStroke(keystroke,
            Toolkit.getDefaultToolkit(  ).getMenuShortcutKeyMask(  )));
	}
	void newProject(){
		setProjectChanged(true);		
		clearIndividualBugInformation();
		
		if(newProject){
			setTitle("FindBugs: " + Project.UNNAMED_PROJECT);
			projectDirectory = null;
			saveProjectMenuItem.setEnabled(false);
			editProjectMenuItem.setEnabled(true);
		}		
	}

	
	/**
	 * Called when use has not previous saved project. Uses save() after finds
	 * where user want to save file.
	 * @return True if successful.
	 */
	private boolean projectSaveAs(){
		if (curProject==null)
		{
			JOptionPane.showMessageDialog(MainFrame.this,"There is no project to save");
			return false;
		}
		
		FBFileChooser jfc=new FBFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setFileFilter(new FindBugsProjectFileFilter());
		jfc.setDialogTitle("Save as...");
		boolean saving = true;
		boolean exists = false;
		File dir=null;
		while (saving)
		{
			int value=jfc.showSaveDialog(MainFrame.this);
			if(value==JFileChooser.APPROVE_OPTION)
			{
				saving = false;
				dir = jfc.getSelectedFile();
				File xmlFile=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");
				File fasFile=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");
				exists=xmlFile.exists() && fasFile.exists();

				if(exists){
					int response = JOptionPane.showConfirmDialog(jfc, 
							"This project already exists.\nDo you want to replace it?",
							"Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					
					if(response == JOptionPane.OK_OPTION)
						saving = false;
					if(response == JOptionPane.CANCEL_OPTION){
						saving = true;
						continue;
					}
				}
						
				boolean good=save(dir);
				if (good==false)
				{
					JOptionPane.showMessageDialog(MainFrame.this, "An error occured in saving");
					return false;
				}
				projectDirectory=dir;				
			}
			else
				return false;
		}
		curProject.setProjectFileName(projectDirectory.getName());
		File xmlFile=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");

		//If the file already existed, its already in the preferences, as well as the recent projects menu items, only add it if they change the name, otherwise everything we're storing is still accurate since all we're storing is the location of the file.
		if (!exists)
		{
			GUISaveState.getInstance().addRecentProject(xmlFile);
		}

		MainFrame.this.addRecentProjectToMenu(xmlFile);

		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	JPanel bugListPanel()
	{
		JPanel topPanel = new JPanel();
		topPanel.setMinimumSize(new Dimension(200,200));
		tableheader = new JTableHeader();
		//Listener put here for when user double clicks on sorting
		//column header SorterDialog appears.
		tableheader.addMouseListener(new MouseAdapter(){

			public void mouseClicked(MouseEvent e) {
				if (!tableheader.getReorderingAllowed())
					return;
				if (e.getClickCount()==2)
					SorterDialog.getInstance().setVisible(true);
			}

			public void mouseReleased(MouseEvent arg0) {
				if (!tableheader.getReorderingAllowed())
					return;
				BugTreeModel bt=(BugTreeModel) (MainFrame.this.getTree().getModel());
				bt.checkSorter();
			}
		});
		sorter = GUISaveState.getInstance().getStarterTable();
		tableheader.setColumnModel(sorter);
		
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
		curProject=BugLoader.getLoadedProject();
		
		
		treeScrollPane = new JScrollPane(tree);
		topPanel.setLayout(new BorderLayout());

		topPanel.add(tableheader, BorderLayout.NORTH);
		topPanel.add(treeScrollPane, BorderLayout.CENTER);
		
		return topPanel;
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
				FilterMatcher.addFilterListener(newModel);
				newTree.addTreeExpansionListener(newModel);
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
					saveCommentsToBug(currentSelectedBugLeaf);

					if ((path.getLastPathComponent() instanceof BugLeafNode))
					{	
						boolean beforeProjectChanged = projectChanged;
						updateDesignation();
						setUserCommentInputEnable(true);
						BugLeafNode bugLeaf = (BugLeafNode)path.getLastPathComponent();
						BugInstance bug = bugLeaf.getBug();
						currentSelectedBugLeaf = bugLeaf;
						currentSelectedBugAspects = null;
						updateSummaryTab(bugLeaf);
						updateCommentsTab(bugLeaf);
						displayer.displaySource(bug, bug.getPrimarySourceLineAnnotation());
						setProjectChanged(beforeProjectChanged);
					}
					else
					{
						updateDesignation();
						currentSelectedBugLeaf = null;
						currentSelectedBugAspects = (BugAspects)path.getLastPathComponent();
						clearIndividualBugInformation();
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
		
	
	void setDesignation(String value) {
		if (currentSelectedBugLeaf != null) {
			designationComboBox.setSelectedItem(value);
			designationChanged=true;
			guiLayout.makeCommentsVisible();
		}
	}
	private void addDesignationItem(JMenu menu, final String menuName,  int keyEvent) {
		JMenuItem toggleItem = new JMenuItem(menuName);

		toggleItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setDesignation(menuName);
			}});	
		attachAccelaratorKey(toggleItem, keyEvent);
		menu.add(toggleItem);
	}
	protected void updateDesignation() {
		
		if (currentSelectedBugLeaf==null)
			return;
		
		if (!getSorter().getOrder().contains(Sortables.DESIGNATION))
		{
			if (designationChanged)
			{
				int index = designationComboBox.getSelectedIndex();
				currentSelectedBugLeaf.getBug().getSafeUserDesignation().setDesignation(designationList.get(index));
				designationChanged=false;
			}
		}
		else if (getSorter().getOrderBeforeDivider().contains(Sortables.DESIGNATION))
		{
			if (designationChanged)
			{
				Debug.println("What huh!?!?");
				int index = designationComboBox.getSelectedIndex();
				BugTreeModel model= (BugTreeModel)tree.getModel();
				TreePath path=model.getPathToBug(currentSelectedBugLeaf.getBug());
				if (path==null)
				{
					designationChanged=false;
					return;
				}
				Object[] objPath=path.getParentPath().getPath();
				ArrayList<Object> reconstruct=new ArrayList<Object>();
				ArrayList<TreePath> listOfNodesToReconstruct=new ArrayList<TreePath>();
				for (int x=0; x< objPath.length;x++)
				{
					Object o=objPath[x];
					reconstruct.add(o);
					if (o instanceof BugAspects)
					{
						if (((BugAspects)o).getCount()==1)
						{
		//					Debug.println((BugAspects)(o));
							break;
						}
					}
					TreePath pathToNode=new TreePath(reconstruct.toArray());
					listOfNodesToReconstruct.add(pathToNode);
				}
		
				currentSelectedBugLeaf.getBug().getSafeUserDesignation().setDesignation(designationList.get(index));
				model.suppressBug(path);
				TreePath unsuppressPath=model.getPathToBug(currentSelectedBugLeaf.getBug());
				if (unsuppressPath!=null)//If choosing their designation has not moved the bug under any filters
				{
					model.unsuppressBug(unsuppressPath);
		//			tree.setSelectionPath(unsuppressPath);
				}
				for (TreePath pathToNode: listOfNodesToReconstruct)
				{
					model.treeNodeChanged(pathToNode);
				}
				setProjectChanged(true);
				designationChanged=false;
			}
		}
		else if (getSorter().getOrderAfterDivider().contains(Sortables.DESIGNATION))
		{
			if (designationChanged)
			{
				int index = designationComboBox.getSelectedIndex();
				currentSelectedBugLeaf.getBug().getSafeUserDesignation().setDesignation(designationList.get(index));
				
				BugTreeModel model = (BugTreeModel)tree.getModel();
				TreePath pathToBranch=model.getPathToBug(currentSelectedBugLeaf.getBug()).getParentPath();
				model.sortBranch(pathToBranch);
				designationChanged=false;
			}
		}
	}

	/**
	 * Clears the bottom tabs so not show bug information.
	 *
	 */
	 void clearIndividualBugInformation(){
			
		setUserCommentInputEnable(false); //Do not put in Swing thread b/c already in it.
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				setSourceTabTitle("Source");				
				clearSummaryTab();
				sourceCodeTextPane.setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
				currSrcLineAnnotation = null;
			}
		});		
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
			statusBarLabel.setText("  1 bug hidden");
		else 
			statusBarLabel.setText("  " + countFilteredBugs + " bugs hidden");
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
	Component summaryTab()
	{
		int fontSize = (int) Driver.getFontSize();
		summaryTopPanel = new JPanel();
		summaryTopPanel.setLayout(new GridLayout(0,1));
		summaryTopPanel.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
		summaryTopPanel.setMinimumSize(new Dimension(fontSize * 50, fontSize*5));
		
		JPanel summaryTopOuter = new JPanel(new BorderLayout());
		summaryTopOuter.add(summaryTopPanel, BorderLayout.NORTH);
		
		summaryHtmlArea.setContentType("text/html");
		summaryHtmlArea.setEditable(false);
		summaryHtmlArea.setToolTipText("This gives a longer description of the detected bug pattern");
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
		splitP.setDividerLocation(85);
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
				return new JLabel("null");
			
			if(value instanceof SourceLineAnnotation){
				final SourceLineAnnotation note = (SourceLineAnnotation) value;
				if(sourceCodeExist(note)){
					String srcStr = "";
					int start = note.getStartLine();
					int end = note.getEndLine();
					if(start < 0 && end < 0)
						srcStr = "source code.";
					else if(start == end)
						srcStr = " [Line " + start + "]";
					else if(start < end)
						srcStr = " [Lines " + start + " - " + end + "]";
					
					label.setToolTipText("Click to go to " + srcStr);
					
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
						srcStr = "source code.";
					else if(start == end)
						srcStr = " [Line " + start + "]";
					else if(start < end)
						srcStr = " [Lines " + start + " - " + end + "]";
					
					if(!srcStr.equals("")){
						label.setToolTipText("Click to go to " + srcStr);
						label.addMouseListener(new BugSummaryMouseListener(bug, label, noteSrc));
					}
				}
				if(!srcStr.equals("source code."))
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
		
		public void mouseClicked(MouseEvent e) {			
			displayer.displaySource(bugInstance, note);
		}
		public void mouseEntered(MouseEvent e){
			label.setForeground(Color.blue);
			setCursor(new Cursor(Cursor.HAND_CURSOR));
		}
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
	
	/**
	 * Creates the comments tab JPanel.
	 */
	JPanel commentsPanel()
	{	
		if (true) return createCommentsInputPanel();
		JPanel commentsPanel = new JPanel();
		BorderLayout commentsLayout = new BorderLayout();
		commentsLayout.setHgap(10);
		commentsLayout.setVgap(10);
		commentsPanel.setLayout(commentsLayout);
		
		commentsPanel.add(createCommentsInputPanel(), BorderLayout.CENTER);
		
		//Create labels for combo boxes and textArea.
		//Got a little complicated so that spacing was correct.
		//Possible need of revision
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		
		JPanel statusPanel= new JPanel();
		JLabel statusLabel = new JLabel("Designation:");
		statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
		statusPanel.setAlignmentY(Component.RIGHT_ALIGNMENT);
		statusPanel.add(Box.createVerticalStrut(COMMENTS_TAB_STRUT_SIZE));		
		statusPanel.add(statusLabel);
				
		JLabel commentsLabel = new JLabel("Comments:");
		commentsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		JPanel prevCommentsPanel = new JPanel();
		JLabel prevCommentsLabel = new JLabel("Previous:");
		prevCommentsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		prevCommentsPanel.setLayout(new BoxLayout(prevCommentsPanel, BoxLayout.Y_AXIS));
		prevCommentsPanel.setAlignmentY(Component.RIGHT_ALIGNMENT);
		prevCommentsPanel.add(prevCommentsLabel);
		prevCommentsPanel.add(Box.createVerticalStrut(COMMENTS_TAB_STRUT_SIZE));
		
		leftPanel.add(statusPanel, BorderLayout.NORTH);
		leftPanel.add(commentsLabel, BorderLayout.CENTER);
		leftPanel.add(prevCommentsPanel, BorderLayout.SOUTH);
		
		commentsPanel.add(leftPanel, BorderLayout.WEST);
		
		commentsPanel.setBorder(BorderFactory.createEmptyBorder(COMMENTS_MARGIN,COMMENTS_MARGIN,COMMENTS_MARGIN,COMMENTS_MARGIN));
		return commentsPanel;
	}
	
	/**
	 * Create center panel that holds the user input combo boxes and TextArea.
	 */
	private JPanel createCommentsInputPanel(){
		JPanel centerPanel = new JPanel();
		BorderLayout centerLayout = new BorderLayout();
		centerLayout.setVgap(10);
		centerPanel.setLayout(centerLayout);
		
		userCommentsText.getDocument().addDocumentListener(new DocumentListener(){

			public void insertUpdate(DocumentEvent e) {
				commentChanged = true;
				setProjectChanged(true);
			}

			public void removeUpdate(DocumentEvent e) {
				commentChanged = true;
				setProjectChanged(true);
			}

			public void changedUpdate(DocumentEvent e) {}
			
		});
		
		userCommentsTextUnenabledColor = centerPanel.getBackground();
		
		userCommentsText.setLineWrap(true);
		userCommentsText.setToolTipText("Enter your comments about this bug here");
		userCommentsText.setWrapStyleWord(true);
		userCommentsText.setEnabled(false);
		userCommentsText.setBackground(userCommentsTextUnenabledColor);
		JScrollPane commentsScrollP = new JScrollPane(userCommentsText);

		prevCommentsComboBox.setEnabled(false);
		prevCommentsComboBox.setToolTipText("Use this to reuse a previous textual comment for this bug");
		prevCommentsComboBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {	
				if(e.getStateChange() == ItemEvent.SELECTED && prevCommentsComboBox.getSelectedIndex() != 0){
					setCurrentUserCommentsText(getCurrentPrevCommentsSelection());
					
					prevCommentsComboBox.setSelectedIndex(0);					
				}
			}			
		});
		
		designationComboBox = new JComboBox();
		designationList = new ArrayList<String>();
		
		designationComboBox.setEnabled(false);
		designationComboBox.setToolTipText("Select a user designation for this bug");
		designationComboBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {	
				if (userInputEnabled)
				{
					if(e.getStateChange() == ItemEvent.SELECTED && currentSelectedBugLeaf != null && !alreadySelected())
					{
						designationChanged=true;
						setProjectChanged(true);
					}
				}
			}
			
			/*
			 * Checks to see if the designation is already selected as that.
			 * This was created because it was found the itemStateChanged method is called
			 * when the combo box is set when a bug is clicked.
			 */
			private boolean alreadySelected(){
				return designationList.get(designationComboBox.getSelectedIndex()).
						equals(currentSelectedBugLeaf.getBug().getSafeUserDesignation().getDesignation());
			}
		});
		
		for(String s : I18N.instance().getUserDesignationKeys(true)){
			designationList.add(s);
			designationComboBox.addItem(Sortables.DESIGNATION.formatValue(s));
		}
		
		designationComboBox.setSelectedIndex(0); //WARNING: this is hard coded in here.
		
		centerPanel.add(designationComboBox, BorderLayout.NORTH);
		centerPanel.add(commentsScrollP, BorderLayout.CENTER);
		centerPanel.add(prevCommentsComboBox, BorderLayout.SOUTH);
				
		return centerPanel;
	}
	
	/**
	 * Sets the user comment panel to whether or not it is enabled.
	 * If isEnabled is false will clear the user comments text pane.
	 * @param isEnabled
	 */
	private void setUserCommentInputEnable(final boolean isEnabled){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){				
				userInputEnabled=isEnabled;
				if(!isEnabled){
					userCommentsText.setText("");
					commentChanged = false;
					//WARNING: this is hard coded in here, but needed
					//so when not enabled shows default setting of designation
					designationComboBox.setSelectedIndex(0);
				}
				
				userCommentsText.setEnabled(isEnabled);
				prevCommentsComboBox.setEnabled(isEnabled);
				designationComboBox.setEnabled(isEnabled);
				
				if(isEnabled)
					userCommentsText.setBackground(Color.WHITE);
				else
					userCommentsText.setBackground(userCommentsTextUnenabledColor);
			}
		});
	}
	
	/**
	 * Updates comments tab.
	 * Takes node passed and sets the designation and comments.
	 * @param node
	 */
	private void updateCommentsTab(final BugLeafNode node){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				boolean b = projectChanged;
				BugInstance bug = node.getBug();
				setCurrentUserCommentsText(bug.getAnnotationText());
				designationComboBox.setSelectedIndex(designationList.indexOf(node.getBug().getSafeUserDesignation().getDesignation()));
				commentChanged = false;
				setProjectChanged(b);
			}
		});		
	}
	
	/**
	 * Saves the current comments to the BugLeafNode passed in.
	 * If the passed in node's annotation is already equal to the current
	 * user comment then will not do anything so setProjectedChanged is 
	 * not made true. Will also add the comment if it is new to the previous
	 * comments list.
	 * @param node
	 */
	private void saveCommentsToBug(BugLeafNode node){
		if(node == null || !commentChanged)
			return;
		
		if(node.getBug().getAnnotationText().equals(getCurrentUserCommentsText()))
			return;
		
		node.getBug().setAnnotationText(getCurrentUserCommentsText());		
		setProjectChanged(true);
		
		addToPrevComments(getCurrentUserCommentsText());						
		commentChanged = false;
	}
	
	/**
	 * Saves comments to the current selected bug.
	 *
	 */
	public void saveComments(){
		saveCommentsToBug(currentSelectedBugLeaf);
	}
	
	/**
	 * Deletes the list have already. Then loads from list. Will load from
	 * the list until run out of room in the prevCommentsList.
	 * @param list
	 */
	private void loadPrevCommentsList(String[] list){
		int count = 0;
		for(String str : list){
			if(str.equals(""))
				count++;
		}
		
		String[] ary = new String[list.length-count];
		int j = 0;
		for(String str : list){
			if(!str.equals("")){
				ary[j] = str;
				j++;
			}
		}
		
		String[] temp;
		prevCommentsList = new LinkedList<String>();
		if((ary.length) > prevCommentsMaxSize){
			temp = new String[prevCommentsMaxSize];
			for(int i = 0; i < temp.length && i < ary.length; i++)
				temp[i] = ary[i];
		}
		else{
			temp = new String[ary.length];
			for(int i = 0; i < ary.length; i++)
				temp[i] = ary[i];
		}
		
		for(String str : temp)
			prevCommentsList.add(str);
		
		resetPrevCommentsComboBox();
	}
	
	/**
	 * Adds the comment into the list. If the comment is already in the list
	 * then simply moves to the front. If the list is too big when adding
	 * the comment then deletes the last comment on the list.
	 * @param comment
	 */
	private void addToPrevComments(String comment){
		if(comment.equals(""))
			return;
		
		if(prevCommentsList.contains(comment)){
			int index = prevCommentsList.indexOf(comment);
			prevCommentsList.remove(index);
		}
		
		prevCommentsList.addFirst(comment);			
		
		while(prevCommentsList.size() > prevCommentsMaxSize)
			prevCommentsList.removeLast();
		
		resetPrevCommentsComboBox();
	}
	
	/**
	 * Removes all items in the comboBox for previous comments. Then
	 * refills it using prevCommentsList.
	 *
	 */
	private void resetPrevCommentsComboBox(){
		prevCommentsComboBox.removeAllItems();	
		
		prevCommentsComboBox.addItem("");
			
		for(String str : prevCommentsList){
			if (str.length() < 20)
				prevCommentsComboBox.addItem(str);
			else 
				prevCommentsComboBox.addItem(str.substring(0,17)+"...");
		}
	}
	
	/**
	 * Returns the text in the current user comments textArea.
	 * @return
	 */
	private String getCurrentUserCommentsText(){
		return userCommentsText.getText();
	}

	/**
	 * Sets the current user comments text area to comment.
	 * @param comment
	 */
	private void setCurrentUserCommentsText(String comment){
		userCommentsText.setText(comment);
	}
	
	/**
	 * Returns the current selected previous comments. Returns as an
	 * object.
	 */
	private String getCurrentPrevCommentsSelection(){
		return prevCommentsList.get(prevCommentsComboBox.getSelectedIndex() - 1);
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
	
	static class CutAction extends TextAction {
		
		public CutAction() {
			super("Cut");
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
			super("Copy");
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
			super("Paste");
		}
		
		public void actionPerformed( ActionEvent evt ) {
			JTextComponent text = getTextComponent( evt );
			
			if(text == null)
				return;
			
			text.paste();
		}
	}	

	public void setProject(Project p) {
		curProject=p;
		setTitle(p.getProjectFileName());
	}

	public SourceFinder getSourceFinder() 
	{
		return sourceFinder;
	}
	
	public void setSourceFinder(SourceFinder sf)
	{
		sourceFinder=sf;
	}

	public void setRebuilding(boolean b)
	{
		tableheader.setReorderingAllowed(!b);
		preferencesMenuItem.setEnabled(!b);
		if (b)
			SorterDialog.getInstance().freeze();
		else
			SorterDialog.getInstance().thaw();
		recentProjectsMenu.setEnabled(!b);
	}
	
	public void setSorting(boolean b) {
		tableheader.setReorderingAllowed(b);
	}
	
	/**
	 * Called when something in the project is changed and the change
	 * needs to be saved.
	 */
	/*
	 * This should be called instead of using projectChanged = b.
	 */
	public void setProjectChanged(boolean b){
		if(curProject == null)
			return;
		
		if(projectChanged == b)
			return;
		
		if(projectDirectory != null && projectDirectory.exists())
			saveProjectMenuItem.setEnabled(b);
		
		projectChanged = b;
	}
	
	/*
	 * DO NOT use the projectDirectory variable to figure out the current project directory in this function
	 * use the passed in value, as that variable may or may not have been set to the passed in value at this point.
	 */
	private boolean save(File dir)
	{
		saveCommentsToBug(currentSelectedBugLeaf);
		
		dir.mkdir();
		updateDesignation();
		
		File f = new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");	
		File filtersAndSuppressions=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");
		//Saves current comment to current bug.
		saveCommentsToBug(currentSelectedBugLeaf);

		BugSaver.saveBugs(f,BugSet.getMainBugSet(),curProject);
		try {
			filtersAndSuppressions.createNewFile();
			ProjectSettings.getInstance().save(new FileOutputStream(filtersAndSuppressions));
		} catch (IOException e) {
			Debug.println(e);
			return false;
		}
		setProjectChanged(false);
		MainFrame.this.setTitle("FindBugs: " + dir.getName());
		
		saveProjectMenuItem.setEnabled(false);
		return true;
	}
	
	/**
	 * Returns the color of the source code pane's background.
	 * @return
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
	 * 
	 */
	private void saveAnalysis() {
		saveCommentsToBug(currentSelectedBugLeaf);
		
		if (curProject==null)
		{
			JOptionPane.showMessageDialog(MainFrame.this,"There is no project to save");
			return;
		}
		
		FBFileChooser chooser=new FBFileChooser();
		chooser.setFileFilter(new FindBugsAnalysisFileFilter());
		boolean saving=true;
		while (saving)
		{

			int value=chooser.showSaveDialog(MainFrame.this);
			if (value==JFileChooser.APPROVE_OPTION)
			{
				saving=false;
				File xmlFile = chooser.getSelectedFile();
				
				if(!xmlFile.getName().endsWith(".xml"))
					xmlFile = new File(xmlFile.getAbsolutePath()+".xml");
				
				if (xmlFile.exists())
				{
					int response = JOptionPane.showConfirmDialog(chooser, 
							"This analysis already exists.\nReplace it?",
							"Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					
					if(response == JOptionPane.OK_OPTION)
						saving = false;
					if(response == JOptionPane.CANCEL_OPTION){
						saving = true;
						continue;
					}
				}
				BugSaver.saveBugs(xmlFile, BugSet.getMainBugSet(), MainFrame.this.curProject);
			}
		else return;
		}
	}
	/**
	 * 
	 */
	private void loadAnalysis() {
		saveCommentsToBug(currentSelectedBugLeaf);

		FBFileChooser jfc = new FBFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setFileFilter(new FindBugsAnalysisFileFilter());

		// jfc.setCurrentDirectory(GUISaveState.getInstance().getStarterDirectoryForLoadBugs());
		// this is done by FBFileChooser now.

		while (true) {
			int returnValue = jfc.showOpenDialog(new JFrame());

			if (returnValue != JFileChooser.APPROVE_OPTION)
				return;

			File file = jfc.getSelectedFile();

			if (!file.exists()) {
				JOptionPane.showMessageDialog(jfc, "That file does not exist");
				continue;
			} 
			try {
				FileInputStream in = new FileInputStream(file);
				loadAnalysisFromInputStream(in);
				return;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(jfc, e.getMessage());
			}


		}
	}
	/**
	 * @param file
	 * @return
	 */
	private void loadAnalysisFromInputStream(final InputStream in) {
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
		new Thread(new Runnable(){
			public void run()
			{
				BugTreeModel model=(BugTreeModel)tree.getModel();
//								BugTreeModel.pleaseWait();
				MainFrame.this.setRebuilding(true);
				Project project = new Project();
				BugSet bs=BugLoader.loadBugs(project, in);
				MainFrame.this.setRebuilding(false);
				if (bs!=null)
				{
					ProjectSettings.newInstance();
					model.getOffListenerList();
					updateDesignation();
					model.changeSet(bs);
					curProject=project;
					MainFrame.this.updateStatusBar();
					MainFrame.this.setTitle(project.getProjectFileName());
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					setProjectChanged(false);
				}
			}
		}).start();
		return;
	}
	/**
	 * 
	 */
	private void redoAnalysis() {
		saveCommentsToBug(currentSelectedBugLeaf);
		
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		new Thread()
		{
			public void run()
			{
				updateDesignation();
				BugSet bs=BugLoader.redoAnalysisKeepComments(curProject);
				
				if (bs!=null)
				{
					displayer.clearCache();
					//Dont clear data, the data's correct, just get the tree off the listener lists.
					((BugTreeModel) tree.getModel()).getOffListenerList();
					((BugTreeModel)tree.getModel()).changeSet(bs);
					curProject=BugLoader.getLoadedProject();
				}
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				setProjectChanged(true);
			}
		}.start();
	}
	/**
	 * 
	 */
	private void mergeAnalysis() {
		saveCommentsToBug(currentSelectedBugLeaf);
		
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		BugSet bs=BugLoader.combineBugHistories();
		if (bs!=null)
		{
			displayer.clearCache();
			((BugTreeModel)tree.getModel()).getOffListenerList();
			updateDesignation();
			((BugTreeModel)tree.getModel()).changeSet(bs);
			curProject=BugLoader.getLoadedProject();
		}
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		
		setProjectChanged(true);
	}
	/**
	 * 
	 */
	private void openProject() {
		saveCommentsToBug(currentSelectedBugLeaf);
		
		FBFileChooser jfc=new FBFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setFileFilter(FindBugsProjectFileFilter.INSTANCE);
		File xmlFile=null;
		if (projectChanged)
		{
			int response = JOptionPane.showConfirmDialog(MainFrame.this, 
					"The current project has been changed, Save current changes?"
					,"Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (response == JOptionPane.YES_OPTION)
			{
				if (projectDirectory!=null)
					save(projectDirectory);
				else
					projectSaveAs();
			}
			else if (response == JOptionPane.CANCEL_OPTION)
				return;
			//IF no, do nothing.
		}
		
		boolean loading = true;
		while (loading)
		{
			int value=jfc.showOpenDialog(MainFrame.this);
			if(value==JFileChooser.APPROVE_OPTION){
				loading = false;
				final File dir = jfc.getSelectedFile();						
				
				if(!dir.exists() || !dir.isDirectory())
				{
					JOptionPane.showMessageDialog(null, "Warning! This project is not a directory.");
					loading = true;
					continue;
				}
				else
				{
					xmlFile= new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".xml");		
					File fasFile=new File(dir.getAbsolutePath() + File.separator + dir.getName() + ".fas");

					if (!xmlFile.exists())
					{
						JOptionPane.showMessageDialog(null, "This directory does not contain saved bug XML data, please choose a different directory.");
						loading=true;
						continue;
					}
					
					if (!fasFile.exists())
					{
						JOptionPane.showMessageDialog(MainFrame.this, "Filter settings not found, using default settings.");
						try {
							fasFile.createNewFile();
							ProjectSettings.newInstance().save(new FileOutputStream(fasFile));
						} catch (IOException e) {
							if (MainFrame.DEBUG) System.err.println("Error saving new filter settings file, using default settings without saving these settings to the project.");
							ProjectSettings.newInstance();
						}
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
							BugTreeModel model=(BugTreeModel)tree.getModel();
//									Debug.println("please wait called by open menu item");
							BugTreeModel.pleaseWait();
							MainFrame.this.setRebuilding(true);
							Project project = new Project();
							BugSet bs=BugLoader.loadBugs(project, extraFinalReferenceToXmlFile);
							MainFrame.this.setRebuilding(false);
							if (bs!=null)
							{
								editProjectMenuItem.setEnabled(true);
								displayer.clearCache();
								model.getOffListenerList();
								updateDesignation();
								model.changeSet(bs);
								curProject=project;
								projectDirectory=dir;
								curProject.setProjectFileName(projectDirectory.getName());
								MainFrame.this.setTitle("FindBugs: " + project.getProjectFileName());
								MainFrame.getInstance().updateStatusBar();
							}
						}
					}).start();
				}
			}
			else if (value==JFileChooser.CANCEL_OPTION)
			{
				return;
			}
			else
				loading = false;
		}
//				List<String> projectPaths=new ArrayList<String>();
		ArrayList<File> xmlFiles=GUISaveState.getInstance().getRecentProjects();

		if (!xmlFiles.contains(xmlFile))
		{
			GUISaveState.getInstance().addRecentProject(xmlFile);
			MainFrame.this.addRecentProjectToMenu(xmlFile);
		}
		
		//Clears the bottom tabs so they are blank. And makes comments
		//tab not enabled.				
		clearIndividualBugInformation();

		designationChanged = false;
		projectChanged = false;
	}
	/**
	 * 
	 */
	private void newProjectMenu() {
		saveCommentsToBug(currentSelectedBugLeaf);
		new NewProjectWizard();
		
		newProject = true;
	}
}