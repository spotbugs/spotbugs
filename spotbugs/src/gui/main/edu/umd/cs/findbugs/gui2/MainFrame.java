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

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugsDisplayFeatures;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.log.ConsoleLogger;
import edu.umd.cs.findbugs.log.LogSync;
import edu.umd.cs.findbugs.log.Logger;
import edu.umd.cs.findbugs.sourceViewer.NavigableTextPane;
import edu.umd.cs.findbugs.util.Multiset;

@SuppressWarnings("serial")
/*
 * This is where it all happens... seriously... all of it... All the menus are
 * set up, all the listeners, all the frames, dockable window functionality
 * There is no one style used, no one naming convention, its all just kinda
 * here. This is another one of those classes where no one knows quite why it
 * works. <p> The MainFrame is just that, the main application window where just
 * about everything happens.
 */
public class MainFrame extends FBFrame implements LogSync {
    public static final boolean GUI2_DEBUG = SystemProperties.getBoolean("gui2.debug");

    public static final boolean MAC_OS_X = SystemProperties.getProperty("os.name").toLowerCase().startsWith("mac os x");

    private static final int SEARCH_TEXT_FIELD_SIZE = 32;

    public static final String TITLE_START_TXT = "SpotBugs";

    private final static String WINDOW_MODIFIED = "windowModified";

    public static final boolean USE_WINDOWS_LAF = false;

    private static MainFrame instance;

    private final MyGuiCallback guiCallback = new MyGuiCallback();

    private BugCollection bugCollection;

    private BugAspects currentSelectedBugAspects;

    private volatile Project curProject = new Project();

    private volatile boolean newProject = false;

    private final ProjectPackagePrefixes projectPackagePrefixes = new ProjectPackagePrefixes();

    private final Logger logger = new ConsoleLogger(this);

    @CheckForNull
    private File saveFile = null;

    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    private final CountDownLatch mainFrameInitialized = new CountDownLatch(1);

    private int waitCount = 0;

    private final Object waitLock = new Object();

    private volatile String errorMsg = "";

    /*
     * To change this value must use setProjectChanged(boolean b). This is
     * because saveProjectItemMenu is dependent on it for when
     * saveProjectMenuItem should be enabled.
     */
    private boolean projectChanged = false;

    private final FindBugsLayoutManager guiLayout;

    private final JLabel statusBarLabel = new JLabel();

    private final JTextField sourceSearchTextField = new JTextField(SEARCH_TEXT_FIELD_SIZE);

    private final JButton findButton = MainFrameHelper.newButton("button.find", "First");

    private final JButton findNextButton = MainFrameHelper.newButton("button.findNext", "Next");

    private final JButton findPreviousButton = MainFrameHelper.newButton("button.findPrev", "Previous");

    private final NavigableTextPane sourceCodeTextPane = new NavigableTextPane();

    private JPanel summaryTopPanel;

    JEditorPane summaryHtmlArea = new JEditorPane();

    private final JScrollPane summaryHtmlScrollPane = new JScrollPane(summaryHtmlArea);

    private final SourceCodeDisplay displayer = new SourceCodeDisplay(this);

    private final ViewFilter viewFilter = new ViewFilter(this);

    private SaveType saveType = SaveType.NOT_KNOWN;

    private final MainFrameLoadSaveHelper mainFrameLoadSaveHelper = new MainFrameLoadSaveHelper(this);

    final MainFrameTree mainFrameTree = new MainFrameTree(this);

    final MainFrameMenu mainFrameMenu = new MainFrameMenu(this);

    private final MainFrameComponentFactory mainFrameComponentFactory = new MainFrameComponentFactory(this);

    public static void makeInstance(FindBugsLayoutManagerFactory factory) {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = new MainFrame(factory);
        instance.initializeGUI();
    }

    public static MainFrame getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance;
    }

    private MainFrame(FindBugsLayoutManagerFactory factory) {
        guiLayout = factory.getInstance(this);
        FindBugsDisplayFeatures.setAbridgedMessages(true);
    }

    public void showMessageDialog(String message) {
        guiCallback.showMessageDialog(message);
    }

    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        return guiCallback.showConfirmDialog(message, title, ok, cancel);
    }

    public IGuiCallback getGuiCallback() {
        return guiCallback;
    }

    public void acquireDisplayWait() {
        synchronized (waitLock) {
            waitCount++;
            if (GUI2_DEBUG) {
                System.err.println("acquiring display wait, count " + waitCount);
                Thread.dumpStack();
            }
            if (waitCount == 1) {
                mainFrameTree.showCard(BugCard.WAITCARD, new Cursor(Cursor.WAIT_CURSOR), this);
            }
        }
    }

    volatile Exception previousDecrementToZero;

    public void releaseDisplayWait() {
        synchronized (waitLock) {
            if (waitCount <= 0) {
                if (previousDecrementToZero != null) {
                    throw new IllegalStateException("Can't decrease wait count; already zero", previousDecrementToZero);
                } else {
                    throw new IllegalStateException("Can't decrease wait count; never incremented");
                }
            }
            waitCount--;
            if (GUI2_DEBUG) {
                System.err.println("releasing display wait, count " + waitCount);
                Thread.dumpStack();
            }
            if (waitCount == 0) {
                mainFrameTree.showCard(BugCard.TREECARD, new Cursor(Cursor.DEFAULT_CURSOR), this);
                previousDecrementToZero = new Exception("Previously decremented at");
            }
        }
    }

    public void waitUntilReady() throws InterruptedException {
        mainFrameInitialized.await();
    }

    public JTree getTree() {
        return mainFrameTree.getTree();
    }

    public BugTreeModel getBugTreeModel() {
        return mainFrameTree.getBugTreeModel();
    }

    public synchronized @Nonnull Project getProject() {
        if (curProject == null) {
            curProject = new Project();
        }
        return curProject;
    }

    public synchronized void setProject(Project p) {
        curProject = p;
    }

    /**
     * Called when something in the project is changed and the change needs to
     * be saved. This method should be called instead of using projectChanged =
     * b.
     */
    public void setProjectChanged(boolean b) {
        if (curProject == null) {
            return;
        }

        if (projectChanged == b) {
            return;
        }

        projectChanged = b;
        mainFrameMenu.setSaveMenu(this);

        getRootPane().putClientProperty(WINDOW_MODIFIED, b);

    }

    /**
     * Show an error dialog.
     */
    @Override
    public void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Write a message to stdout.
     */
    @Override
    public void writeToLog(String message) {
        if (GUI2_DEBUG) {
            System.out.println(message);
        }
    }

    public int showConfirmDialog(String message, String title, int optionType) {
        return JOptionPane.showConfirmDialog(this, message, title, optionType);
    }

    public Sortables[] getAvailableSortables() {
        return mainFrameTree.getAvailableSortables();
    }

    // ============================== listeners ============================

    /*
     * This is overridden for changing the font size
     */
    @Override
    public void addNotify() {
        super.addNotify();

        float size = Driver.getFontSize();

        JMenuBar menubar = getJMenuBar();
        if (menubar != null) {
            menubar.setFont(menubar.getFont().deriveFont(size));
            for (int i = 0; i < menubar.getMenuCount(); i++) {
                for (int j = 0; j < menubar.getMenu(i).getMenuComponentCount(); j++) {
                    Component temp = menubar.getMenu(i).getMenuComponent(j);
                    temp.setFont(temp.getFont().deriveFont(size));
                }
            }
            mainFrameTree.updateFonts(size);
        }
    }

    @SwingThread
    void updateStatusBar() {
        int countFilteredBugs = BugSet.countFilteredBugs();
        String msg = "";
        if (countFilteredBugs == 1) {
            msg = "  1 " + L10N.getLocalString("statusbar.bug_hidden", "bug hidden (see view menu)");
        } else if (countFilteredBugs > 1) {
            msg = "  " + countFilteredBugs + " "
                    + L10N.getLocalString("statusbar.bugs_hidden", "bugs hidden (see view menu)");
        }
        if (errorMsg != null && errorMsg.length() > 0) {
            msg = join(msg, errorMsg);
        }

        mainFrameTree.setWaitStatusLabelText(msg); // should not be the URL
        if (msg.length() == 0) {
            msg = "https://github.com/spotbugs";
        }
        statusBarLabel.setText(msg);
    }

    /**
     * This method is called when the application is closing. This is either by
     * the exit menuItem or by clicking on the window's system menu.
     */
    void callOnClose() {
        if (projectChanged && !SystemProperties.getBoolean("findbugs.skipSaveChangesWarning")) {
            Object[] options = {
                L10N.getLocalString("dlg.save_btn", "Save"),
                L10N.getLocalString("dlg.dontsave_btn", "Don't Save"),
                L10N.getLocalString("dlg.cancel_btn", "Cancel"),
            };
            int value = JOptionPane.showOptionDialog(this, getActionWithoutSavingMsg("closing"),
                    L10N.getLocalString("msg.confirm_save_txt", "Do you want to save?"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (value == 2 || value == JOptionPane.CLOSED_OPTION) {
                return;
            } else if (value == 0) {

                if (saveFile == null) {
                    if (!mainFrameLoadSaveHelper.saveAs()) {
                        return;
                    }
                } else {
                    mainFrameLoadSaveHelper.save();
                }
            }
        }

        GUISaveState guiSaveState = GUISaveState.getInstance();
        guiLayout.saveState();
        guiSaveState.setFrameBounds(getBounds());
        guiSaveState.setExtendedWindowState(getExtendedState());
        guiSaveState.save();
        System.exit(0);
    }

    // ========================== misc junk ====================================

    JMenuItem createRecentItem(final File f, final SaveType localSaveType) {
        return mainFrameMenu.createRecentItem(f, localSaveType);
    }

    /**
     * Opens the analysis. Also clears the source and summary panes. Makes
     * comments enabled false. Sets the saveType and adds the file to the recent
     * menu.
     *
     * @param f
     * @return whether the operation was successful
     */
    public boolean openAnalysis(File f, SaveType saveType) {
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Can't read " + f.getPath());
        }

        mainFrameLoadSaveHelper.prepareForFileLoad(f, saveType);

        mainFrameLoadSaveHelper.loadAnalysis(f);
        return true;
    }

    public void openBugCollection(SortedBugCollection bugs) {
        acquireDisplayWait();
        try {
            mainFrameLoadSaveHelper.prepareForFileLoad(null, null);

            Project project = bugs.getProject();
            project.setGuiCallback(guiCallback);
            BugLoader.addDeadBugMatcher(bugs);
            setProjectAndBugCollectionInSwingThread(project, bugs);
        } finally {
            releaseDisplayWait();
        }

    }

    void clearBugCollection() {
        setSaveFile(null);
        setProjectAndBugCollection(null, null);
    }

    @SwingThread
    void setBugCollection(BugCollection bugCollection) {
        setProjectAndBugCollection(bugCollection.getProject(), bugCollection);
    }

    void setProjectAndBugCollectionInSwingThread(final Project project, final BugCollection bc) {
        setProjectAndBugCollection(project, bc);
    }

    @SwingThread
    private void setProjectAndBugCollection(@CheckForNull Project project, @CheckForNull BugCollection bugCollection) {
        if (GUI2_DEBUG) {
            if (bugCollection == null) {
                System.out.println("Setting bug collection to null");
            } else {
                System.out.println("Setting bug collection; contains " + bugCollection.getCollection().size() + " bugs");
            }

        }
        if (bugCollection != null && bugCollection.getProject() != project) {
            Project p2 = bugCollection.getProject();
            throw new IllegalArgumentException(String.format("project %x and bug collection %x don't match",
                    System.identityHashCode(project), System.identityHashCode(p2)));
        }
        acquireDisplayWait();
        try {
            // setRebuilding(false);
            setProject(project);
            this.bugCollection = bugCollection;
            BugLoader.addDeadBugMatcher(bugCollection);

            displayer.clearCache();
            mainFrameTree.updateBugTree();
            setProjectChanged(false);
            Runnable runnable = () -> {
                PreferencesFrame.getInstance().updateFilterPanel();
                mainFrameMenu.getReconfigMenuItem().setEnabled(true);
                mainFrameMenu.setViewMenu();
                newProject();
                clearSourcePane();
                clearSummaryTab();

                /*
                 * This is here due to a threading issue. It can only be
                 * called after curProject has been changed. Since this
                 * method is called by both open methods it is put here.
                 */
                updateTitle();
            };
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            } else {
                SwingUtilities.invokeLater(runnable);
            }
        } finally {
            releaseDisplayWait();
        }
    }

    void updateProjectAndBugCollection(BugCollection bugCollection) {

        if (bugCollection != null) {
            displayer.clearCache();
            BugSet bs = new BugSet(bugCollection);
            // Don't clear data, the data's correct, just get the tree off the
            // listener lists.
            BugTreeModel model = (BugTreeModel) mainFrameTree.getTree().getModel();
            model.getOffListenerList();
            model.changeSet(bs);
            // curProject=BugLoader.getLoadedProject();
            setProjectChanged(true);
        }
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    //    @SuppressWarnings({ "SimplifiableIfStatement" })
    boolean shouldDisplayIssue(BugInstance b) {
        Project project = getProject();
        Filter suppressionFilter = project.getSuppressionFilter();
        if (null == getBugCollection() || suppressionFilter.match(b)) {
            return false;
        }
        return viewFilter.show(b);
    }

    // ============================= menu actions
    // ===============================

    public void createNewProjectFromMenuItem() {
        new NewProjectWizard();

        newProject = true;
    }

    void newProject() {
        clearSourcePane();
        if (!FindBugs.isNoAnalysis()) {
            mainFrameMenu.enableOrDisableItems(curProject, bugCollection);
        }

        if (newProject) {
            setProjectChanged(true);
            // setTitle(TITLE_START_TXT + Project.UNNAMED_PROJECT);
            saveFile = null;
            mainFrameMenu.getSaveMenuItem().setEnabled(false);
            mainFrameMenu.getReconfigMenuItem().setEnabled(true);
            newProject = false;
        }
    }

    void about() {
        AboutDialog dialog = new AboutDialog(this, logger, true);
        dialog.setSize(600, 554);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    void preferences() {
        PreferencesFrame.getInstance().setLocationRelativeTo(this);
        PreferencesFrame.getInstance().setVisible(true);
    }

    void redoAnalysis() {
        /// QQQ-TODO: new RuntimeException("Redo analysis called").printStackTrace();
        acquireDisplayWait();
        edu.umd.cs.findbugs.util.Util.runInDameonThread(
                () -> {
                    try {
                        Project project = getProject();
                        BugCollection bc = BugLoader.redoAnalysisKeepComments(project);
                        updateProjectAndBugCollection(bc);
                        setProjectAndBugCollectionInSwingThread(project, bc);
                    } finally {
                        releaseDisplayWait();
                    }
                });
    }

    // ================================== misc junk 2
    // ==============================

    void syncBugInformation() {
        boolean prevProjectChanged = projectChanged;
        if (mainFrameTree.getCurrentSelectedBugLeaf() != null) {
            BugInstance bug = mainFrameTree.getCurrentSelectedBugLeaf().getBug();
            displayer.displaySource(bug, bug.getPrimarySourceLineAnnotation());
            updateSummaryTab(mainFrameTree.getCurrentSelectedBugLeaf());
        } else if (currentSelectedBugAspects != null) {
            displayer.displaySource(null, null);
            clearSummaryTab();
        } else {
            displayer.displaySource(null, null);
            clearSummaryTab();
        }
        setProjectChanged(prevProjectChanged);
    }

    void clearSourcePane() {
        SwingUtilities.invokeLater(() -> {
            mainFrameComponentFactory.setSourceTab("", null);
            sourceCodeTextPane.setDocument(SourceCodeDisplay.SOURCE_NOT_RELEVANT);
        });
    }

    // =============================== component creation
    // ==================================

    private void initializeGUI() {
        mainFrameComponentFactory.initializeGUI();
    }

    JPanel statusBar() {
        return mainFrameComponentFactory.statusBar();
    }

    JSplitPane summaryTab() {
        return mainFrameComponentFactory.summaryTab();
    }

    JPanel createSourceCodePanel() {
        return mainFrameComponentFactory.createSourceCodePanel();
    }

    JPanel createSourceSearchPanel() {
        return mainFrameComponentFactory.createSourceSearchPanel();
    }

    /**
     * Sets the title of the source tabs for either docking or non-docking
     * versions.
     */
    void setSourceTab(String title, @CheckForNull BugInstance bug) {
        mainFrameComponentFactory.setSourceTab(title, bug);
    }

    SorterTableColumnModel getSorter() {
        return mainFrameTree.getSorter();
    }

    private String getActionWithoutSavingMsg(String action) {
        String msg = L10N.getLocalString("msg.you_are_" + action + "_without_saving_txt", null);
        if (msg != null) {
            return msg;
        }
        return L10N.getLocalString("msg.you_are_" + action + "_txt", "You are " + action) + " "
                + L10N.getLocalString("msg.without_saving_txt", "without saving. Do you want to save?");
    }

    public void updateBugTree() {
        mainFrameTree.updateBugTree();
    }

    public void resetViewCache() {
        ((BugTreeModel) mainFrameTree.getTree().getModel()).clearViewCache();
    }

    /**
     * Changes the title based on curProject and saveFile.
     */
    public void updateTitle() {
        Project project = getProject();
        String name = project.getProjectName();
        if ((name == null || "".equals(name.trim())) && saveFile != null) {
            name = saveFile.getAbsolutePath();
        }
        if (name == null) {
            name = "";//Project.UNNAMED_PROJECT;
        }
        String oldTitle = this.getTitle();
        String newTitle = TITLE_START_TXT + ("".equals(name.trim()) ? "" : " - " + name);
        if (oldTitle.equals(newTitle)) {
            return;
        }
        this.setTitle(newTitle);
    }

    //    @SuppressWarnings({ "SimplifiableIfStatement" })
    private boolean shouldDisplayIssueIgnoringPackagePrefixes(BugInstance b) {
        Project project = getProject();
        Filter suppressionFilter = project.getSuppressionFilter();
        if (null == getBugCollection() || suppressionFilter.match(b)) {
            return false;
        }
        return viewFilter.showIgnoringPackagePrefixes(b);
    }

    public void selectPackagePrefixByProject() {
        TreeSet<String> projects = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Multiset<String> count = new Multiset<>();
        int total = 0;
        for (BugInstance b : getBugCollection().getCollection()) {
            if (shouldDisplayIssueIgnoringPackagePrefixes(b)) {
                TreeSet<String> projectsForThisBug = projectPackagePrefixes.getProjects(b.getPrimaryClass().getClassName());
                projects.addAll(projectsForThisBug);
                count.addAll(projectsForThisBug);
                total++;
            }
        }
        if (projects.size() == 0) {
            JOptionPane.showMessageDialog(this, "No issues in current view");
            return;
        }
        ArrayList<ProjectSelector> selectors = new ArrayList<>(projects.size() + 1);
        ProjectSelector everything = new ProjectSelector("all projects", "", total);
        selectors.add(everything);
        for (String projectName : projects) {
            ProjectPackagePrefixes.PrefixFilter filter = projectPackagePrefixes.getFilter(projectName);
            selectors.add(new ProjectSelector(projectName, filter.toString(), count.getCount(projectName)));
        }
        ProjectSelector choice = (ProjectSelector) JOptionPane.showInputDialog(null,
                "Choose a project to set appropriate package prefix(es)", "Select package prefixes by package",
                JOptionPane.QUESTION_MESSAGE, null, selectors.toArray(), everything);
        if (choice == null) {
            return;
        }

        mainFrameTree.setFieldForPackagesToDisplayText(choice.filter);
        viewFilter.setPackagesToDisplay(choice.filter);
        resetViewCache();

    }

    private static String join(String s1, String s2) {
        if (s1 == null || s1.length() == 0) {
            return s2;
        }
        if (s2 == null || s2.length() == 0) {
            return s1;
        }
        return s1 + "; " + s2;
    }

    private void updateSummaryTab(BugLeafNode node) {
        final BugInstance bug = node.getBug();

        SwingUtilities.invokeLater(() -> {
            summaryTopPanel.removeAll();

            summaryTopPanel.add(mainFrameComponentFactory.bugSummaryComponent(bug.getAbridgedMessage(), bug));

            for (BugAnnotation b : bug.getAnnotationsForMessage(true)) {
                summaryTopPanel.add(mainFrameComponentFactory.bugSummaryComponent(b, bug));
            }


            BugPattern bugPattern = bug.getBugPattern();
            String detailText =
                    bugPattern.getDetailText()
                            + "<br><p> <b>Bug kind and pattern: " +
                            bugPattern.getAbbrev() + " - " + bugPattern.getType();
            String txt = bugPattern.getDetailHTML(detailText);
            summaryHtmlArea.setText(txt);

            summaryTopPanel.add(Box.createVerticalGlue());
            summaryTopPanel.revalidate();

            SwingUtilities.invokeLater(() -> summaryHtmlScrollPane.getVerticalScrollBar().setValue(
                    summaryHtmlScrollPane.getVerticalScrollBar().getMinimum()));
        });
    }

    public void clearSummaryTab() {
        summaryHtmlArea.setText("");
        summaryTopPanel.removeAll();
        summaryTopPanel.revalidate();
    }

    public void searchSource(int type) {
        int targetLineNum = -1;
        String targetString = sourceSearchTextField.getText();
        switch (type) {
        case 0:
            targetLineNum = displayer.find(targetString);
            break;
        case 1:
            targetLineNum = displayer.findNext(targetString);
            break;
        case 2:
            targetLineNum = displayer.findPrevious(targetString);
            break;
        default:
            break;
        }
        if (targetLineNum != -1) {
            displayer.foundItem(targetLineNum);
        }
    }

    @SuppressWarnings({ "deprecation" })
    public void createProjectSettings() {
        ProjectSettings.newInstance();
    }

    /*
     * If the file already existed, its already in the preferences, as well as
     * the recent projects menu items, only add it if they change the name,
     * otherwise everything we're storing is still accurate since all we're
     * storing is the location of the file.
     */
    public void addFileToRecent(File xmlFile) {
        mainFrameMenu.addFileToRecent(xmlFile);
    }

    public void setSaveType(SaveType saveType) {
        if (GUI2_DEBUG && this.saveType != saveType) {
            System.out.println("Changing save type from " + this.saveType + " to " + saveType);
        }
        this.saveType = saveType;
    }

    public SaveType getSaveType() {
        return saveType;
    }

    // =================================== misc accessors for helpers
    // ==========================

    public BugLeafNode getCurrentSelectedBugLeaf() {
        return mainFrameTree.getCurrentSelectedBugLeaf();
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
        return mainFrameMenu.getSaveMenuItem();
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    public ExecutorService getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public JMenuItem getReconfigMenuItem() {
        return mainFrameMenu.getReconfigMenuItem();
    }

    public SourceCodeDisplay getSourceCodeDisplayer() {
        return displayer;
    }

    public ProjectPackagePrefixes getProjectPackagePrefixes() {
        return projectPackagePrefixes;
    }

    public void enableRecentMenu(boolean enable) {
        mainFrameMenu.enableRecentMenu(enable);
    }

    public void setCurrentSelectedBugAspects(BugAspects currentSelectedBugAspects) {
        this.currentSelectedBugAspects = currentSelectedBugAspects;
    }

    public ViewFilter getViewFilter() {
        return viewFilter;
    }

    public Project getCurProject() {
        return curProject;
    }

    public MainFrameLoadSaveHelper getMainFrameLoadSaveHelper() {
        return mainFrameLoadSaveHelper;
    }

    public FindBugsLayoutManager getGuiLayout() {
        return guiLayout;
    }

    public MainFrameTree getMainFrameTree() {
        return mainFrameTree;
    }

    public boolean projectChanged() {
        return projectChanged;
    }

    public MainFrameMenu getMainFrameMenu() {
        return mainFrameMenu;
    }

    public JEditorPane getSummaryHtmlArea() {
        return summaryHtmlArea;
    }

    public JLabel getStatusBarLabel() {
        return statusBarLabel;
    }

    public JButton getFindNextButton() {
        return findNextButton;
    }

    public JScrollPane getSummaryHtmlScrollPane() {
        return summaryHtmlScrollPane;
    }

    public JButton getFindPreviousButton() {
        return findPreviousButton;
    }

    public JTextField getSourceSearchTextField() {
        return sourceSearchTextField;
    }

    public JButton getFindButton() {
        return findButton;
    }

    public JPanel getSummaryTopPanel() {
        return summaryTopPanel;
    }

    public void setSummaryTopPanel(JPanel summaryTopPanel) {
        this.summaryTopPanel = summaryTopPanel;
    }

    void waitForMainFrameInitialized() {
        mainFrameInitialized.countDown();
    }

    enum BugCard {
        TREECARD, WAITCARD
    }

    private static class ProjectSelector {
        public ProjectSelector(String projectName, String filter, int count) {
            this.projectName = projectName;
            this.filter = filter;
            this.count = count;
        }

        final String projectName;

        final String filter;

        final int count;

        @Override
        public String toString() {
            return String.format("%s -- [%d issues]", projectName, count);
        }
    }

    private class MyGuiCallback extends AbstractSwingGuiCallback {
        private MyGuiCallback() {
            super(MainFrame.this);
        }

        @Override
        public void setErrorMessage(String errorMsg) {
            MainFrame.this.errorMsg = errorMsg;
            SwingUtilities.invokeLater(() -> updateStatusBar());
        }
    }
}
