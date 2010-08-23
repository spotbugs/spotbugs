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
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import edu.umd.cs.findbugs.AbstractSwingGuiCallback;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugAnnotationWithSourceLines;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugsDisplayFeatures;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.CloudListener;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import edu.umd.cs.findbugs.sourceViewer.NavigableTextPane;
import edu.umd.cs.findbugs.util.LaunchBrowser;
import edu.umd.cs.findbugs.util.Multiset;

@SuppressWarnings("serial")

/*
 * This is where it all happens... seriously... all of it...
 * All the menus are set up, all the listeners, all the frames, dockable window functionality
 * There is no one style used, no one naming convention, its all just kinda here.  This is another one of those 
 * classes where no one knows quite why it works.
 * <p>
 * The MainFrame is just that, the main application window where just about everything happens.
 */
public class MainFrame extends FBFrame implements LogSync {
    public static final boolean GUI2_DEBUG = SystemProperties.getBoolean("gui2.debug");
    public static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private static final int SEARCH_TEXT_FIELD_SIZE = 32;
    public static final String TITLE_START_TXT = "FindBugs: ";
	private final static String WINDOW_MODIFIED = "windowModified";
    private static final boolean USE_WINDOWS_LAF = false;

	private static MainFrame instance;

    private final MyGuiCallback guiCallback = new MyGuiCallback();

    private BugCollection bugCollection;
    private BugAspects currentSelectedBugAspects;
	private Project curProject = new Project();
	private boolean newProject = false;
	private final ProjectPackagePrefixes projectPackagePrefixes = new ProjectPackagePrefixes();

	private Class<?> osxAdapter;
	private Method osxPrefsEnableMethod;

    private Logger logger = new ConsoleLogger(this);

    @CheckForNull
    private File saveFile = null;

    private CloudListener userAnnotationListener = new MyCloudListener();
    private Cloud.CloudStatusListener cloudStatusListener = new MyCloudStatusListener();

    private ExecutorService backgroundExecutor = Executors.newCachedThreadPool();
	private final CountDownLatch mainFrameInitialized = new CountDownLatch(1);
    private int waitCount = 0;
    private final Object waitLock = new Object();
    private final Runnable updateStatusBarRunner = new statusBarUpdater();

    private volatile String errorMsg = "";

    private URL sourceLink;

    private boolean listenerAdded = false;
    private boolean userInputEnabled;

    /* To change this value must use setProjectChanged(boolean b).
      * This is because saveProjectItemMenu is dependent on it for when
      * saveProjectMenuItem should be enabled.
      */
	private boolean projectChanged = false;

	private final FindBugsLayoutManager guiLayout;

	private final CommentsArea comments;
	private JLabel statusBarLabel = new JLabel();
    private JLabel signedInLabel;
	private JTextField sourceSearchTextField = new JTextField(SEARCH_TEXT_FIELD_SIZE);
	private JButton findButton = MainFrameHelper.newButton("button.find", "First");
	private JButton findNextButton = MainFrameHelper.newButton("button.findNext", "Next");
	private JButton findPreviousButton = MainFrameHelper.newButton("button.findPrev", "Previous");
	private NavigableTextPane sourceCodeTextPane = new NavigableTextPane();
    private JPanel summaryTopPanel;
    private JEditorPane summaryHtmlArea = new JEditorPane();
	private JScrollPane summaryHtmlScrollPane = new JScrollPane(summaryHtmlArea);
	private SourceCodeDisplay displayer = new SourceCodeDisplay(this);
	private ViewFilter viewFilter = new ViewFilter(this);

    private JMenuItem reconfigMenuItem = MainFrameHelper.newJMenuItem("menu.reconfig", "Reconfigure...", KeyEvent.VK_F);
    private JMenuItem redoAnalysis;
	private RecentMenu recentMenuCache;
	private JMenu recentMenu;
	private JMenuItem preferencesMenuItem;
	private JMenu viewMenu ;
    private JMenuItem saveMenuItem = MainFrameHelper.newJMenuItem("menu.save_item", "Save", KeyEvent.VK_S);

	private SaveType saveType = SaveType.NOT_KNOWN;

    private ImageIcon signedInIcon;
    private ImageIcon warningIcon;
    private MainFrameLoadSaveHelper mainFrameLoadSaveHelper;
	final MainFrameTree mainFrameTree = new MainFrameTree(this);


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

	private MainFrame(FindBugsLayoutManagerFactory factory) {
		guiLayout = factory.getInstance(this);
		comments = new CommentsArea(this);
		FindBugsDisplayFeatures.setAbridgedMessages(true);
		
	}

    public void showMessageDialog(String message) {
        guiCallback.showMessageDialog(message);
    }


    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        return guiCallback.showConfirmDialog(message, title, ok, cancel);
    }

    public boolean showDocument(URL u) {
        return guiCallback.showDocument(u);
    }

    public IGuiCallback getGuiCallback() {
        return guiCallback;
    }
	
	public void resetCommentsInputPane() {
		guiLayout.resetCommentsInputPane();
	}

	public void acquireDisplayWait() {
		synchronized(waitLock) {
			waitCount++;
			if (GUI2_DEBUG) {
                System.err.println("acquiring display wait, count " + waitCount);
                Thread.dumpStack();
            }
			if (waitCount == 1)
				mainFrameTree.showCard(BugCard.WAITCARD, new Cursor(Cursor.WAIT_CURSOR), this);
		}
	}
	public void releaseDisplayWait() {
		synchronized(waitLock) {
			if (waitCount <= 0)
				throw new AssertionError("Can't decrease wait count; already zero");
			waitCount--;
			if (GUI2_DEBUG) {
                System.err.println("releasing display wait, count " + waitCount);
                Thread.dumpStack();
            }
			if (waitCount == 0)
				mainFrameTree.showCard(BugCard.TREECARD, new Cursor(Cursor.DEFAULT_CURSOR), this);
		}
	}
	
	public void newTree(final JTree newTree, final BugTreeModel newModel)
	{
		mainFrameTree.newTree(newTree, newModel);
	}

	public void waitUntilReady() throws InterruptedException {
		mainFrameInitialized.await();
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

		mainFrameTree.updateFonts(size);

	}

	public JTree getTree()
	{
		return mainFrameTree.getTree();
	}
	
	public BugTreeModel getBugTreeModel() {
		return mainFrameTree.getBugTreeModel();
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

		getRootPane().putClientProperty(WINDOW_MODIFIED, b);

	}

	public boolean getProjectChanged(){
		return projectChanged;
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
		if (GUI2_DEBUG)
			System.out.println(message);
		//		consoleMessageArea.append(message);
		//		consoleMessageArea.append("\n");
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

        mainFrameLoadSaveHelper.prepareForFileLoad(f, saveType);

        mainFrameLoadSaveHelper.loadAnalysis(f);
		return true;
	}
	
	public void openBugCollection(SortedBugCollection bugs){
		
		acquireDisplayWait();
		try {
            mainFrameLoadSaveHelper.prepareForFileLoad(null, null);

			Project project = bugs.getProject();
			project.setGuiCallback(guiCallback);
			BugLoader.addDeadBugMatcher(project);
			setProjectAndBugCollectionInSwingThread(project, bugs);
		} finally {
			releaseDisplayWait();
		}

	}

    public int showConfirmDialog(String message, String title, int optionType) {
        return JOptionPane.showConfirmDialog(this, message, title, optionType);
    }
	
    public Sortables[] getAvailableSortables() {
		return mainFrameTree.getAvailableSortables();
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
		saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);
		PreferencesFrame.getInstance().setLocationRelativeTo(this);
		PreferencesFrame.getInstance().setVisible(true);
	}


	/**
	 * This method is called when the application is closing. This is either by
	 * the exit menuItem or by clicking on the window's system menu.
	 */
	void callOnClose(){
		comments.saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);
		
		if(projectChanged && !SystemProperties.getBoolean("findbugs.skipSaveChangesWarning")){
			int value = JOptionPane.showConfirmDialog(this, getActionWithoutSavingMsg("closing"),
					edu.umd.cs.findbugs.L10N.getLocalString("msg.confirm_save_txt", "Do you want to save?"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if(value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION)
				return ;
			else if(value == JOptionPane.YES_OPTION){

				if(saveFile == null){
					if(!mainFrameLoadSaveHelper.saveAs())
						return;
				}
				else
                    mainFrameLoadSaveHelper.save();
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

	/*
	 * A lot of if(false) here is for switching from special cases based on localSaveType
	 * to depending on the SaveType.forFile(f) method. Can delete when sure works.
	 */
	JMenuItem createRecentItem(final File f, final SaveType localSaveType)
	{
		if (GUI2_DEBUG) System.out.println("createRecentItem("+f+", "+localSaveType +")");
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
                                mainFrameLoadSaveHelper.save();
							else
                                mainFrameLoadSaveHelper.saveAs();
						}
						else if (response == JOptionPane.CANCEL_OPTION)
							return;
						//IF no, do nothing.
					}

					SaveType st = SaveType.forFile(f);
					boolean result = true;
					switch(st){
					case XML_ANALYSIS:
						result = openAnalysis(f, st);
						break;
					case FBP_FILE:
						result = mainFrameLoadSaveHelper.openFBPFile(f);
						break;
					case FBA_FILE:
						result = mainFrameLoadSaveHelper.openFBAFile(f);
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

	@SwingThread
	void setBugCollection(BugCollection bugCollection) {
		setProjectAndBugCollection(bugCollection.getProject(), bugCollection);
	}
	void updateProjectAndBugCollection(BugCollection bugCollection) {
		
		if (bugCollection != null) {
			displayer.clearCache();
			BugSet bs = new BugSet(bugCollection);
			//Dont clear data, the data's correct, just get the tree off the listener lists.
			BugTreeModel model = (BugTreeModel) mainFrameTree.getTree().getModel();
			model.getOffListenerList();
			model.changeSet(bs);
			//curProject=BugLoader.getLoadedProject();
			setProjectChanged(true);
		}
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@SuppressWarnings({"SimplifiableIfStatement"})
    boolean shouldDisplayIssue(BugInstance b) {
		Project project = getProject();
		Filter suppressionFilter = project.getSuppressionFilter();
		if (null == getBugCollection() || suppressionFilter.match(b))
			return false;
		return viewFilter.show(b);
		}

	void newProject(){
		clearSourcePane();
		if (!FindBugs.noAnalysis) {	
			if (curProject == null)
				redoAnalysis.setEnabled(false);
			else {
				List<String> fileList = curProject.getFileList();
				redoAnalysis.setEnabled(!fileList.isEmpty());
			}
		}

		if(newProject){
			setProjectChanged(true);
//			setTitle(TITLE_START_TXT + Project.UNNAMED_PROJECT);
			saveFile = null;
			saveMenuItem.setEnabled(false);
			reconfigMenuItem.setEnabled(true);
			newProject=false;
		}		
	}


	void syncBugInformation (){
		boolean prevProjectChanged = projectChanged;
		if (mainFrameTree.getCurrentSelectedBugLeaf() != null)  {
			BugInstance bug  = mainFrameTree.getCurrentSelectedBugLeaf().getBug();
			displayer.displaySource(bug, bug.getPrimarySourceLineAnnotation());
			updateDesignationDisplay();
			comments.updateCommentsFromLeafInformation(mainFrameTree.getCurrentSelectedBugLeaf());
			updateSummaryTab(mainFrameTree.getCurrentSelectedBugLeaf());
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
				SigninState signinState = getBugCollection().getCloud().getSigninState();
				boolean isSignedIn = signinState == Cloud.SigninState.SIGNED_IN;
				final JCheckBoxMenuItem signInAuto = new JCheckBoxMenuItem("Sign in automatically");
				signInAuto.setToolTipText("Saves your Cloud session for the next time you run FindBugs. "
				        + "No personal information or passwords are saved.");
				signInAuto.setSelected(getBugCollection().getCloud().isSavingSignInInformationEnabled());
				signInAuto.setEnabled(isSignedIn);
				signInAuto.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						boolean checked = signInAuto.isSelected();
						if (checked != getBugCollection().getCloud().isSavingSignInInformationEnabled()) {
							System.out.println("checked: " + checked);
							getBugCollection().getCloud().setSaveSignInInformation(checked);
						}
					}
				});
				menu.add(signInAuto);
				switch (signinState) {
				case SIGNED_OUT:
				case UNAUTHENTICATED:
				case SIGNIN_FAILED:

					menu.add(new AbstractAction("Sign in") {
						public void actionPerformed(ActionEvent e) {
							try {
								getBugCollection().getCloud().signIn();
							} catch (IOException e1) {
								guiCallback.showMessageDialog("Sign-in error: " + e1.getMessage());
								LOGGER.log(Level.SEVERE, "Could not sign in", e1);
							}
						}
					});
					break;
				default:
					menu.add(new AbstractAction("Sign out") {
						public void actionPerformed(ActionEvent e) {
							getBugCollection().getCloud().signOut();
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
		if (getBugCollection() != null) {
			Cloud plugin = getBugCollection().getCloud();
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
                } else if (state == SigninState.SIGNED_OUT || state == SigninState.UNAUTHENTICATED) {
                    signedInLabel.setText("<html>FindBugs Cloud:<br> not signed in");
                    signedInLabel.setIcon(null);
                    showLoggedInStatus = true;
                }
			}
		}
        signedInLabel.setVisible(showLoggedInStatus);
		if (errorMsg != null && errorMsg.length() > 0)
			msg = join(msg, errorMsg);
        mainFrameTree.setWaitStatusLabelText(msg); // should not be the URL
		if (msg.length() == 0)
			msg = "http://findbugs.sourceforge.net";
        statusBarLabel.setText(msg);
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

	JPanel createCommentsInputPanel() {
		return comments.createCommentsInputPanel();
	}	

	/**
	 * Creates the source code panel, but does not put anything in it.
	 */
	JPanel createSourceCodePanel()
	{
		Font sourceFont = new Font("Monospaced", Font.PLAIN, (int)Driver.getFontSize());
		sourceCodeTextPane.setFont(sourceFont);
		sourceCodeTextPane.setEditable(false);
		sourceCodeTextPane.getCaret().setSelectionVisible(true);
		sourceCodeTextPane.setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
        JScrollPane sourceCodeScrollPane = new JScrollPane(sourceCodeTextPane);
		sourceCodeScrollPane.getVerticalScrollBar().setUnitIncrement(20);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(sourceCodeScrollPane, BorderLayout.CENTER);

		panel.revalidate();
		if (GUI2_DEBUG) System.out.println("Created source code panel");
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



	/**
	 * Sets the title of the source tabs for either docking or non-docking
	 * versions.
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


	/**
	 * Returns the SorterTableColumnModel of the MainFrame.
	 * @return
	 */
	SorterTableColumnModel getSorter()
	{
		return mainFrameTree.getSorter();
	}

	/**
	 * Redo the analysis
	 */
	void redoAnalysis() {
		saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);

		acquireDisplayWait();
		new Thread()
		{
			@Override
			public void run()
			{
				try {
					updateDesignationDisplay();
					BugCollection  bc=BugLoader.redoAnalysisKeepComments(getProject());
					updateProjectAndBugCollection(bc);
				} finally {
					releaseDisplayWait();
				}
			}
		}.start();
	}
	void updateDesignationDisplay() {
		comments.updateDesignationComboBox();
	}

    void setProjectAndBugCollectionInSwingThread(final Project project, final BugCollection bc) {
	    setProjectAndBugCollection(project, bc);
    }


	private void initializeGUI() {
		SwingUtilities.invokeLater(new InitializeGUI());
	}

	/**
	 * enable/disable preferences menu
	 */
	public void enablePreferences(boolean b) {
		preferencesMenuItem.setEnabled(b);
		if (MAC_OS_X) {
			if (osxPrefsEnableMethod != null) {
				Object args[] = {b};
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
     * @return
     */
    private String getActionWithoutSavingMsg(String action) {
    	String msg = edu.umd.cs.findbugs.L10N.getLocalString("msg.you_are_"+action+"_without_saving_txt", null);
    	if (msg != null) return msg;
	    return edu.umd.cs.findbugs.L10N.getLocalString("msg.you_are_"+action+"_txt", "You are "+action) + " " +
	    		edu.umd.cs.findbugs.L10N.getLocalString("msg.without_saving_txt", "without saving. Do you want to save?");
    }

	@SwingThread
	private void setProjectAndBugCollection(@CheckForNull Project project, @CheckForNull BugCollection bugCollection) {
		if (GUI2_DEBUG) {
			if (bugCollection == null) 
				System.out.println("Setting bug collection to null");
			else 
				System.out.println("Setting bug collection; contains " + bugCollection.getCollection().size() + " bugs");
			
		}
		acquireDisplayWait();
		try {
		if (project != null) {
			Filter suppressionMatcher = project.getSuppressionFilter();
			if (suppressionMatcher != null) {
				suppressionMatcher.softAdd(LastVersionMatcher.DEAD_BUG_MATCHER);
			}
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
			setProject(project);
			this.bugCollection = bugCollection;
			displayer.clearCache();
			Cloud plugin = bugCollection.getCloud();
			if (plugin != null) {
				plugin.addListener(userAnnotationListener);
                plugin.addStatusListener(cloudStatusListener);
			}
			mainFrameTree.updateBugTree();
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
	    		updateTitle();
	    	}};
    	if (SwingUtilities.isEventDispatchThread()) 
    		runnable.run();
    	else
    		SwingUtilities.invokeLater(runnable);
    	
		} finally {
			releaseDisplayWait();
		}
		
		
	}
	public void updateBugTree() {
		mainFrameTree.updateBugTree();
	}

	public void resetViewCache() {
		 ((BugTreeModel) mainFrameTree.getTree().getModel()).clearViewCache();
	}

	/**
	 * Changes the title based on curProject and saveFile.
	 *
	 */
	public void updateTitle(){
		Project project = getProject();
		String name = project == null ? null : project.getProjectName();
		if(name == null && saveFile != null)
			name = saveFile.getAbsolutePath();
		if(name == null)
			name = Project.UNNAMED_PROJECT;
		String oldTitle = this.getTitle();
		String newTitle = TITLE_START_TXT + name;
		if (oldTitle.equals(newTitle))
			return;
        this.setTitle(newTitle);
	}

	@SuppressWarnings({"SimplifiableIfStatement"})
    private boolean shouldDisplayIssueIgnoringPackagePrefixes(BugInstance b) {
		Project project = getProject();
		Filter suppressionFilter = project.getSuppressionFilter();
		if (null == getBugCollection() || suppressionFilter.match(b))
			return false;
        return viewFilter.showIgnoringPackagePrefixes(b);
    }

	/**
	 * Creates the MainFrame's menu bar.
	 * @return the menu bar for the MainFrame
	 */
    private JMenuBar createMainMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		//Create JMenus for menuBar.
		JMenu fileMenu = MainFrameHelper.newJMenu("menu.file_menu", "File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenu editMenu = MainFrameHelper.newJMenu("menu.edit_menu", "Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		//Edit fileMenu JMenu object.
		JMenuItem openMenuItem = MainFrameHelper.newJMenuItem("menu.open_item", "Open...", KeyEvent.VK_O);
		recentMenu = MainFrameHelper.newJMenu("menu.recent", "Recent");
		recentMenuCache=new RecentMenu(recentMenu);
		JMenuItem saveAsMenuItem = MainFrameHelper.newJMenuItem("menu.saveas_item", "Save As...", KeyEvent.VK_A);
		JMenuItem importFilter = MainFrameHelper.newJMenuItem("menu.importFilter_item", "Import filter...");
		JMenuItem exportFilter = MainFrameHelper.newJMenuItem("menu.exportFilter_item", "Export filter...");
		
		JMenuItem exitMenuItem = null;
		if (!MAC_OS_X) {
			exitMenuItem = MainFrameHelper.newJMenuItem("menu.exit", "Exit", KeyEvent.VK_X);
			exitMenuItem.addActionListener(new ActionListener(){			
			public void actionPerformed(ActionEvent evt){
				callOnClose();
			}
			});
		}
		JMenu windowMenu = guiLayout.createWindowMenu();

		JMenuItem newProjectMenuItem  = null;
		if (!FindBugs.noAnalysis) {
			newProjectMenuItem = MainFrameHelper.newJMenuItem("menu.new_item", "New Project", KeyEvent.VK_N);

			MainFrameHelper.attachAcceleratorKey(newProjectMenuItem, KeyEvent.VK_N);

			newProjectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					newProjectMenu();
				}
			});
		}

		reconfigMenuItem.setEnabled(false);
		MainFrameHelper.attachAcceleratorKey(reconfigMenuItem, KeyEvent.VK_F);
		reconfigMenuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);
				new NewProjectWizard(curProject);
			}
		});

		JMenuItem mergeMenuItem = MainFrameHelper.newJMenuItem("menu.mergeAnalysis", "Merge Analysis...");

		mergeMenuItem.setEnabled(true);
		mergeMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
                mainFrameLoadSaveHelper.mergeAnalysis();
			}
		});

		if (!FindBugs.noAnalysis) {
		redoAnalysis = MainFrameHelper.newJMenuItem("menu.rerunAnalysis", "Redo Analysis", KeyEvent.VK_R);
		
		redoAnalysis.setEnabled(false);
		MainFrameHelper.attachAcceleratorKey(redoAnalysis, KeyEvent.VK_R);
		redoAnalysis.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				redoAnalysis();
			}
		});
		}

		openMenuItem.setEnabled(true);
		MainFrameHelper.attachAcceleratorKey(openMenuItem, KeyEvent.VK_O);
		openMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
                mainFrameLoadSaveHelper.open();
			}
		});

		saveAsMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
                mainFrameLoadSaveHelper.saveAs();
			}
		});
		exportFilter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
                mainFrameLoadSaveHelper.exportFilter();
			}
		});
		importFilter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
                mainFrameLoadSaveHelper.importFilter();
			}
		});
		saveMenuItem.setEnabled(false);
		MainFrameHelper.attachAcceleratorKey(saveMenuItem, KeyEvent.VK_S);
		saveMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
                mainFrameLoadSaveHelper.save();
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
		preferencesMenuItem = MainFrameHelper.newJMenuItem("menu.preferences_menu", "Filters/Suppressions...");
		JMenuItem sortMenuItem = MainFrameHelper.newJMenuItem("menu.sortConfiguration", "Sort Configuration...");
		JMenuItem goToLineMenuItem = MainFrameHelper.newJMenuItem("menu.gotoLine", "Go to line...");

		MainFrameHelper.attachAcceleratorKey(cutMenuItem, KeyEvent.VK_X);
		MainFrameHelper.attachAcceleratorKey(copyMenuItem, KeyEvent.VK_C);
		MainFrameHelper.attachAcceleratorKey(pasteMenuItem, KeyEvent.VK_V);

		preferencesMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				preferences();
			}
		});

		sortMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);
				SorterDialog.getInstance().setLocationRelativeTo(MainFrame.this);
				SorterDialog.getInstance().setVisible(true);
			}
		});

		MainFrameHelper.attachAcceleratorKey(goToLineMenuItem, KeyEvent.VK_L);
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
		
		viewMenu = MainFrameHelper.newJMenu("menu.view", "View");
		setViewMenu();
		menuBar.add(viewMenu);

		final ActionMap map = mainFrameTree.getTree().getActionMap();

		JMenu navMenu = MainFrameHelper.newJMenu("menu.navigation", "Navigation");

		addNavItem(map, navMenu, "menu.expand", "Expand", "expand", KeyEvent.VK_RIGHT );
		addNavItem(map, navMenu, "menu.collapse", "Collapse", "collapse", KeyEvent.VK_LEFT);
		addNavItem(map, navMenu, "menu.up", "Up", "selectPrevious", KeyEvent.VK_UP );
		addNavItem(map, navMenu, "menu.down", "Down", "selectNext", KeyEvent.VK_DOWN);

		menuBar.add(navMenu);

				
		JMenu designationMenu = MainFrameHelper.newJMenu("menu.designation", "Designation");
		int i = 0;
		int keyEvents [] = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
		for(String key :  I18N.instance().getUserDesignationKeys(true)) {
			String name = I18N.instance().getUserDesignation(key);
			addDesignationItem(designationMenu, name, keyEvents[i++]);
		}
		menuBar.add(designationMenu);

		if (!MAC_OS_X) {		
			// On Mac, 'About' appears under Findbugs menu, so no need for it here
			JMenu helpMenu = MainFrameHelper.newJMenu("menu.help_menu", "Help");
			JMenuItem aboutItem = MainFrameHelper.newJMenuItem("menu.about_item", "About FindBugs");
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
	private void selectPackagePrefixByProject() {
		TreeSet<String> projects = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Multiset<String> count = new Multiset<String>();
		int total = 0;
		for (BugInstance b : getBugCollection().getCollection())
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

		mainFrameTree.setFieldForPackagesToDisplayText(choice.filter);
		viewFilter.setPackagesToDisplay(choice.filter);
		resetViewCache();

	}
	private void setViewMenu() {

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
				if (!r.supported(cloud))
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
	/**
	 * @param map
	 * @param navMenu
	 */
	private void addNavItem(final ActionMap map, JMenu navMenu, String menuNameKey, String menuNameDefault, String actionName, int keyEvent) {
		JMenuItem toggleItem = MainFrameHelper.newJMenuItem(menuNameKey, menuNameDefault);
		toggleItem.addActionListener(mainFrameTree.treeActionAdapter(map, actionName));
		MainFrameHelper.attachAcceleratorKey(toggleItem, keyEvent);
		navMenu.add(toggleItem);
	}


	/**
	 * @param b
	 */
	public void setUserCommentInputEnable(boolean b) {
		comments.setUserCommentInputEnable(b);

	}

    private ImageIcon loadImageResource(String filename, int width, int height) throws IOException {
        return new ImageIcon(ImageIO.read(MainFrame.class.getResource(filename)).getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private String join(String s1, String s2) {
		if (s1 == null || s1.length() == 0) return s2;
		if (s2 == null || s2.length() == 0) return s1;
		return s1 + "; " + s2;
	}


    private void updateSummaryTab(BugLeafNode node)
	{
		final BugInstance bug = node.getBug();

		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				summaryTopPanel.removeAll();

				summaryTopPanel.add(bugSummaryComponent(bug.getAbridgedMessage(), bug));
				
				for(BugAnnotation b : bug.getAnnotationsForMessage(true)) 
					summaryTopPanel.add(bugSummaryComponent(b, bug));

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

	public void clearSummaryTab()
	{
		summaryHtmlArea.setText("");
		summaryTopPanel.removeAll();
		summaryTopPanel.revalidate();	
	}

    /**
	 * Creates bug summary component. If obj is a string will create a JLabel
	 * with that string as it's text and return it. If obj is an annotation
	 * will return a JLabel with the annotation's toString(). If that
	 * annotation is a SourceLineAnnotation or has a SourceLineAnnotation
	 * connected to it and the source file is available will attach
	 * a listener to the label.
	 * @return
	 */
	
	private Component bugSummaryComponent(String str, BugInstance bug){
		JLabel label = new JLabel();
		label.setFont(label.getFont().deriveFont(Driver.getFontSize()));
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		label.setForeground(Color.BLACK);

		label.setText(str);
		
		SourceLineAnnotation link = bug.getPrimarySourceLineAnnotation();
		if (link != null) 
			label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
		
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
			final SourceLineAnnotation link = (SourceLineAnnotation) value;
			if (sourceCodeExists(link)) {
				String srcStr = "";
				int start = link.getStartLine();
				int end = link.getEndLine();
				if (start < 0 && end < 0)
					srcStr = sourceCodeLabel;
				else if (start == end)
					srcStr = " [" + summaryLine + " " + start + "]";
				else if (start < end)
					srcStr = " [" + summaryLines + " " + start + " - " + end  + "]";

				label.setToolTipText(clickToGoToText + " " + srcStr);

				label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
			}

			label.setText(link.toString());
		} else if (value instanceof BugAnnotationWithSourceLines) {
			BugAnnotationWithSourceLines note = (BugAnnotationWithSourceLines) value;
			final SourceLineAnnotation link = note.getSourceLines();
			String srcStr = "";
			if (link != null && sourceCodeExists(link)) {
				int start = link.getStartLine();
				int end = link.getEndLine();
				if (start < 0 && end < 0)
					srcStr = sourceCodeLabel;
				else if (start == end)
					srcStr = " [" + summaryLine + " " + start + "]";
				else if (start < end)
					srcStr = " [" + summaryLines + " " + start + " - " + end + "]";

				if (!srcStr.equals("")) {
					label.setToolTipText(clickToGoToText + " " + srcStr);
					label.addMouseListener(new BugSummaryMouseListener(bug, label, link));
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
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        htmlEditorKit.setStyleSheet(styleSheet);
		summaryHtmlArea.setEditorKit(htmlEditorKit);
	}

	private void searchSource(int type)
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
	 private void addLink(JComponent component, URL source) {
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
	 private void removeLink(JComponent component) {
         this.sourceLink = null;
		 component.setEnabled(false);
		 component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		 component.setToolTipText("");
	 }

	private void setSaveMenu() {
		File s = saveFile;
		saveMenuItem.setEnabled(projectChanged && s != null && getSaveType() != SaveType.FBP_FILE && s.exists());
	}

	void saveComments() {
		comments.saveComments();

	}
	public void saveComments(BugLeafNode theNode, BugAspects theAspects) {
		comments.saveComments(theNode, theAspects);
	}

    @SuppressWarnings({"deprecation"})
    public void createProjectSettings() {
        ProjectSettings.newInstance();
    }

    public void saveComments2() {
        saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), getCurrentSelectedBugAspects());
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
	public void addFileToRecent(File xmlFile){
		ArrayList<File> xmlFiles=GUISaveState.getInstance().getRecentFiles();
		if (!xmlFiles.contains(xmlFile))
		{
			GUISaveState.getInstance().addRecentFile(xmlFile);
		}
        this.recentMenuCache.addRecentFile(xmlFile);
	}

	private void newProjectMenu() {
		comments.saveComments(mainFrameTree.getCurrentSelectedBugLeaf(), currentSelectedBugAspects);
		new NewProjectWizard();

		newProject = true;
	}
    
	public void addDesignationItem(JMenu menu, final String menuName,  int keyEvent) {
		comments.addDesignationItem(menu, menuName, keyEvent);
	}

    public void setSaveType(SaveType saveType) {
    	if (GUI2_DEBUG && this.saveType != saveType)
    		System.out.println("Changing save type from " + this.saveType + " to " + saveType);
        this.saveType = saveType;
    }

    public SaveType getSaveType() {
	    return saveType;
    }

    private void displayCloudReport() {
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

    private Iterable<BugInstance> getDisplayedBugs() {
        return new Iterable<BugInstance>() {

            public Iterator<BugInstance> iterator() {
	       return new ShownBugsIterator();
        }};
   }

    public BugLeafNode getCurrentSelectedBugLeaf() {
		return mainFrameTree.getCurrentSelectedBugLeaf();
	}

    public boolean isUserInputEnabled() {
        return userInputEnabled;
    }

    public void setUserInputEnabled(boolean userInputEnabled) {
        this.userInputEnabled = userInputEnabled;
    }

    public BugAspects getCurrentSelectedBugAspects() {
        return currentSelectedBugAspects;
    }

    public NavigableTextPane getSourceCodeTextPane() {
        return sourceCodeTextPane;
    }

    public BugCollection getBugCollection() {
        return bugCollection;
    }

    public boolean isProjectChanged() {
        return projectChanged;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public Project getCurrentProject() {
        return curProject;
    }

    public JMenuItem getSaveMenuItem() {
        return saveMenuItem;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    public ExecutorService getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public CommentsArea getComments() {
        return comments;
    }

    public JMenuItem getReconfigMenuItem() {
        return reconfigMenuItem;
    }

    public SourceCodeDisplay getSourceCodeDisplayer() {
        return displayer;
    }

    public ProjectPackagePrefixes getProjectPackagePrefixes() {
        return projectPackagePrefixes;
    }

	public void enableRecentMenu(boolean enable) {
		recentMenu.setEnabled(enable);
	}

	public void setCurrentSelectedBugAspects(BugAspects currentSelectedBugAspects) {
		this.currentSelectedBugAspects = currentSelectedBugAspects;
	}

	public ViewFilter getViewFilter() {
		return viewFilter;
	}


	enum BugCard  {TREECARD, WAITCARD}
	
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



	/**
	 * @author pugh
	 */
	private final class InitializeGUI implements Runnable {
		public void run()
		{
			setTitle("FindBugs");
            if (USE_WINDOWS_LAF && System.getProperty("os.name").toLowerCase().contains("windows")) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Could not load Windows Look&Feel", e);
                }
            }

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
			mainFrameTree.setBugPopupMenu(mainFrameTree.createBugPopupMenu());
			mainFrameTree.setBranchPopupMenu(mainFrameTree.createBranchPopUpMenu());
			comments.loadPrevCommentsList(GUISaveState.getInstance().getPreviousComments().toArray(new String[GUISaveState.getInstance().getPreviousComments().size()]));
			updateStatusBar();
			setBounds(GUISaveState.getInstance().getFrameBounds()); 
			Toolkit.getDefaultToolkit().setDynamicLayout(true);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setJMenuBar(createMainMenuBar());
			setVisible(true);

            mainFrameLoadSaveHelper = new MainFrameLoadSaveHelper(MainFrame.this);

			//Sets the size of the tooltip to match the rest of the GUI. - Kristin
			JToolTip tempToolTip = mainFrameTree.getTableheader().createToolTip();
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
					if (GUI2_DEBUG) {
						e.printStackTrace();
					}
				}
			}
			String loadFromURL = SystemProperties.getOSDependentProperty("findbugs.loadBugsFromURL");
			

			if (loadFromURL != null) {
				try {
					loadFromURL = SystemProperties.rewriteURLAccordingToProperties(loadFromURL);
					URL url = new URL(loadFromURL);
                    mainFrameLoadSaveHelper.loadAnalysis(url);
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

		BugSummaryMouseListener(@NonNull BugInstance bugInstance, @NonNull JLabel label,  @NonNull SourceLineAnnotation link){
			this.bugInstance = bugInstance;
			this.label = label;
			this.note = link;
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

    class ShownBugsIterator implements Iterator<BugInstance> {
        Iterator<BugInstance> base = getBugCollection().getCollection().iterator();
        boolean nextKnown;
        BugInstance next;

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

        public BugInstance next() {
            if (!hasNext())
                throw new NoSuchElementException();
            BugInstance result = next;
            next = null;
            nextKnown = false;
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();

        }
    }

    private class MyGuiCallback extends AbstractSwingGuiCallback {
        private MyGuiCallback() {
            super(MainFrame.this);
        }

        public void registerCloud(Project project, BugCollection collection, Cloud plugin) {
            assert collection.getCloud() == plugin;
            if (MainFrame.this.bugCollection == collection) {
                plugin.addListener(userAnnotationListener);
                plugin.addStatusListener(cloudStatusListener);
            }
            // setProjectAndBugCollectionInSwingThread(project, collection);
        }

        public void unregisterCloud(Project project, BugCollection collection, Cloud plugin) {
            assert collection.getCloud() == plugin;
            if (MainFrame.this.bugCollection == collection) {
                plugin.removeListener(userAnnotationListener);
                plugin.removeStatusListener(cloudStatusListener);
            }
            // Don't think we need to do this
            // setProjectAndBugCollectionInSwingThread(project, collection);
        }

        public void setErrorMessage(String errorMsg) {
            MainFrame.this.errorMsg = errorMsg;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateStatusBar();
                }
            });
        }

    }

    private class MyCloudListener implements CloudListener {

        public void issueUpdated(BugInstance bug) {
            if (mainFrameTree.getCurrentSelectedBugLeaf() != null
                    && mainFrameTree.getCurrentSelectedBugLeaf().getBug() == bug)
                comments.updateCommentsFromLeafInformation(mainFrameTree.getCurrentSelectedBugLeaf());
        }

        public void statusUpdated() {
            SwingUtilities.invokeLater(updateStatusBarRunner);
        }

        public void taskStarted(Cloud.CloudTask task) {
        }
    }

    private class MyCloudStatusListener implements Cloud.CloudStatusListener {
        public void handleIssueDataDownloadedEvent() {
			mainFrameTree.rebuildBugTreeIfSortablesDependOnCloud();
        }

        public void handleStateChange(SigninState oldState, SigninState state) {
			mainFrameTree.rebuildBugTreeIfSortablesDependOnCloud();
        }
    }

    private class statusBarUpdater implements Runnable {
        public void run() {
            updateStatusBar();
        }
    }

}
