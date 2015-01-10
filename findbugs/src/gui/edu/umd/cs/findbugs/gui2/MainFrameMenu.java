package edu.umd.cs.findbugs.gui2;

import static edu.umd.cs.findbugs.gui2.MainFrameHelper.*;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui.AnnotatedString;
import edu.umd.cs.findbugs.gui2.FilterListener.Action;
import edu.umd.cs.findbugs.updates.UpdateChecker;

public class MainFrameMenu implements Serializable {
    private final MainFrame mainFrame;

    JMenuItem reconfigMenuItem = newJMenuItem("menu.reconfig", "Reconfigure...", KeyEvent.VK_F);

    JMenuItem redoAnalysis;
    JMenuItem closeProjectItem;

    RecentMenu recentMenuCache;

    JMenu recentMenu;

    JMenuItem preferencesMenuItem;

    JMenu viewMenu;

    JMenuItem saveMenuItem = newJMenuItem("menu.save_item", "Save", KeyEvent.VK_S);

    private Class<?> osxAdapter;

    private Method osxPrefsEnableMethod;
    private JMenuItem saveAsMenuItem;
    private JMenuItem groupByMenuItem;

    public MainFrameMenu(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JMenuItem createRecentItem(final File f, final SaveType localSaveType) {
        if (MainFrame.GUI2_DEBUG) {
            System.out.println("createRecentItem(" + f + ", " + localSaveType + ")");
        }
        String name = f.getName();

        final JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    mainFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                    if (!f.exists()) {
                        JOptionPane.showMessageDialog(null,
                                L10N.getLocalString("msg.proj_not_found", "This project can no longer be found"));
                        GUISaveState.getInstance().fileNotFound(f);
                        return;
                    }
                    GUISaveState.getInstance().fileReused(f);// Move to front in
                    // GUISaveState, so
                    // it will be last
                    // thing to be
                    // removed from the
                    // list

                    recentMenuCache.addRecentFile(f);

                    if (!f.exists()) {
                        throw new IllegalStateException("User used a recent projects menu item that didn't exist.");
                    }

                    // Moved this outside of the thread, and above the line
                    // saveFile=f.getParentFile()
                    // Since if this save goes on in the thread below, there is
                    // no way to stop the save from
                    // overwriting the files we are about to load.
                    if (mainFrame.getCurProject() != null && mainFrame.isProjectChanged()) {
                        int response = JOptionPane.showConfirmDialog(mainFrame, L10N.getLocalString("dlg.save_current_changes",
                                "The current project has been changed, Save current changes?"), L10N.getLocalString(
                                        "dlg.save_changes", "Save Changes?"), JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.WARNING_MESSAGE);

                        if (response == JOptionPane.YES_OPTION) {
                            if (mainFrame.getSaveFile() != null) {
                                mainFrame.getMainFrameLoadSaveHelper().save();
                            } else {
                                mainFrame.getMainFrameLoadSaveHelper().saveAs();
                            }
                        } else if (response == JOptionPane.CANCEL_OPTION)
                        {
                            return;
                            // IF no, do nothing.
                        }
                    }

                    SaveType st = SaveType.forFile(f);
                    boolean result = true;
                    switch (st) {
                    case XML_ANALYSIS:
                        result = mainFrame.openAnalysis(f, st);
                        break;
                    case FBP_FILE:
                        result = mainFrame.getMainFrameLoadSaveHelper().openFBPFile(f);
                        break;
                    case FBA_FILE:
                        result = mainFrame.getMainFrameLoadSaveHelper().openFBAFile(f);
                        break;
                    default:
                        mainFrame.error("Wrong file type in recent menu item.");
                    }

                    if (!result) {
                        JOptionPane.showMessageDialog(MainFrame.getInstance(), "There was an error in opening the file",
                                "Recent Menu Opening Error", JOptionPane.WARNING_MESSAGE);
                    }
                } finally {
                    mainFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    mainFrame.setSaveType(localSaveType);
                }
            }
        });
        item.setFont(item.getFont().deriveFont(Driver.getFontSize()));
        return item;
    }

    /**
     * Creates the MainFrame's menu bar.
     *
     * @return the menu bar for the MainFrame
     */
    JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create JMenus for menuBar.
        JMenu fileMenu = newJMenu("menu.file_menu", "File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenu editMenu = newJMenu("menu.edit_menu", "Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        // Edit fileMenu JMenu object.
        JMenuItem openMenuItem = newJMenuItem("menu.open_item", "Open...", KeyEvent.VK_O);
        recentMenu = newJMenu("menu.recent_menu", "Recent");
        recentMenuCache = new RecentMenu(recentMenu);
        saveAsMenuItem = newJMenuItem("menu.saveas_item", "Save As...", KeyEvent.VK_A);
        JMenuItem importFilter = newJMenuItem("menu.importFilter_item", "Import bug filters...");
        JMenuItem exportFilter = newJMenuItem("menu.exportFilter_item", "Export bug filters...");

        JMenuItem exitMenuItem = null;
        if (!MainFrame.MAC_OS_X) {
            exitMenuItem = newJMenuItem("menu.exit", "Exit", KeyEvent.VK_X);
            exitMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.callOnClose();
                }
            });
        }
        JMenu windowMenu = mainFrame.getGuiLayout().createWindowMenu();

        JMenuItem newProjectMenuItem = null;
        if (!FindBugs.isNoAnalysis()) {
            newProjectMenuItem = newJMenuItem("menu.new_item", "New Project", KeyEvent.VK_N);

            attachAcceleratorKey(newProjectMenuItem, KeyEvent.VK_N);

            newProjectMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.createNewProjectFromMenuItem();
                }
            });
        }

        reconfigMenuItem.setEnabled(false);
        attachAcceleratorKey(reconfigMenuItem, KeyEvent.VK_F);
        reconfigMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!mainFrame.canNavigateAway()) {
                    return;
                }
                new NewProjectWizard(mainFrame.getCurProject());
            }
        });

        JMenuItem mergeMenuItem = newJMenuItem("menu.mergeAnalysis", "Merge Analysis...");

        mergeMenuItem.setEnabled(true);
        mergeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().mergeAnalysis();
            }
        });

        if (!FindBugs.isNoAnalysis()) {
            redoAnalysis = newJMenuItem("menu.rerunAnalysis", "Redo Analysis", KeyEvent.VK_R);

            redoAnalysis.setEnabled(false);
            attachAcceleratorKey(redoAnalysis, KeyEvent.VK_R);
            redoAnalysis.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.redoAnalysis();
                }
            });
        }
        closeProjectItem = newJMenuItem("menu.closeProject", "Close Project");
        closeProjectItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.getMainFrameLoadSaveHelper().closeProject();
                mainFrame.clearBugCollection();
            }
        });
        closeProjectItem.setEnabled(false);


        openMenuItem.setEnabled(true);
        attachAcceleratorKey(openMenuItem, KeyEvent.VK_O);
        openMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().open();
            }
        });

        saveAsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().saveAs();
            }
        });
        exportFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().exportFilter();
            }
        });
        importFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().importFilter();
            }
        });
        saveMenuItem.setEnabled(false);
        attachAcceleratorKey(saveMenuItem, KeyEvent.VK_S);
        saveMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().save();
            }
        });

        if (!FindBugs.isNoAnalysis()) {
            fileMenu.add(newProjectMenuItem);
        }

        fileMenu.add(openMenuItem);
        fileMenu.add(recentMenu);
        fileMenu.addSeparator();
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(reconfigMenuItem);
        if (!FindBugs.isNoAnalysis()) {
            fileMenu.add(redoAnalysis);
        }

        fileMenu.addSeparator();
        fileMenu.add(closeProjectItem);
        // fileMenu.add(mergeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(importFilter);
        fileMenu.add(exportFilter);

        if (exitMenuItem != null) {
            fileMenu.addSeparator();
            fileMenu.add(exitMenuItem);
        }

        menuBar.add(fileMenu);

        // Edit editMenu Menu object.
        JMenuItem cutMenuItem = new JMenuItem(new CutAction());
        JMenuItem copyMenuItem = new JMenuItem(new CopyAction());
        JMenuItem pasteMenuItem = new JMenuItem(new PasteAction());
        preferencesMenuItem = newJMenuItem("menu.preferences_menu", "Preferences...");
        groupByMenuItem = newJMenuItem("menu.sortConfiguration", "Sort Configuration...");
        JMenuItem goToLineMenuItem = newJMenuItem("menu.gotoLine", "Go to line...");

        attachAcceleratorKey(cutMenuItem, KeyEvent.VK_X);
        attachAcceleratorKey(copyMenuItem, KeyEvent.VK_C);
        attachAcceleratorKey(pasteMenuItem, KeyEvent.VK_V);

        preferencesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.preferences();
            }
        });

        groupByMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!mainFrame.canNavigateAway()) {
                    return;
                }
                SorterDialog.getInstance().setLocationRelativeTo(mainFrame);
                SorterDialog.getInstance().setVisible(true);
            }
        });

        attachAcceleratorKey(goToLineMenuItem, KeyEvent.VK_L);
        goToLineMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getGuiLayout().makeSourceVisible();
                try {
                    int num = Integer.parseInt(JOptionPane.showInputDialog(mainFrame, "",
                            L10N.getLocalString("dlg.go_to_line_lbl", "Go To Line") + ":", JOptionPane.QUESTION_MESSAGE));
                    mainFrame.getSourceCodeDisplayer().showLine(num);
                } catch (NumberFormatException e) {
                }
            }
        });

        editMenu.add(cutMenuItem);
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.addSeparator();
        editMenu.add(goToLineMenuItem);
        editMenu.addSeparator();
        // editMenu.add(selectAllMenuItem);
        // editMenu.addSeparator();
        if (!MainFrame.MAC_OS_X) {
            // Preferences goes in Findbugs menu and is handled by OSXAdapter
            editMenu.add(preferencesMenuItem);
        }

        menuBar.add(editMenu);

        if (windowMenu != null) {
            menuBar.add(windowMenu);
        }

        viewMenu = newJMenu("menu.view_menu", "View");
        setViewMenu();
        menuBar.add(viewMenu);

        final ActionMap map = mainFrame.getMainFrameTree().getTree().getActionMap();

        JMenu navMenu = newJMenu("menu.navigation", "Navigation");

        addNavItem(map, navMenu, "menu.expand", "Expand", "expand", KeyEvent.VK_RIGHT);
        addNavItem(map, navMenu, "menu.collapse", "Collapse", "collapse", KeyEvent.VK_LEFT);
        addNavItem(map, navMenu, "menu.up", "Up", "selectPrevious", KeyEvent.VK_UP);
        addNavItem(map, navMenu, "menu.down", "Down", "selectNext", KeyEvent.VK_DOWN);

        menuBar.add(navMenu);

        JMenu designationMenu = newJMenu("menu.designation", "Designation");
        int i = 0;
        int keyEvents[] = { KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
                KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9 };
        for (String key : I18N.instance().getUserDesignationKeys(true)) {
            String name = I18N.instance().getUserDesignation(key);
            mainFrame.addDesignationItem(designationMenu, key, name, keyEvents[i++]);
        }
        menuBar.add(designationMenu);

        if (!MainFrame.MAC_OS_X) {
            // On Mac, 'About' appears under Findbugs menu, so no need for it
            // here
            JMenu helpMenu = newJMenu("menu.help_menu", "Help");
            JMenuItem aboutItem = newJMenuItem("menu.about_item", "About FindBugs");
            helpMenu.add(aboutItem);

            aboutItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.about();
                }
            });

            JMenuItem updateItem = newJMenuItem("menu.check_for_updates", "Check for Updates...");
            UpdateChecker checker = DetectorFactoryCollection.instance().getUpdateChecker();
            boolean disabled = checker.updateChecksGloballyDisabled();
            updateItem.setEnabled(!disabled);
            if (disabled) {
                updateItem.setToolTipText("Update checks disabled by plugin: "
                        + checker.getPluginThatDisabledUpdateChecks());
            }
            helpMenu.add(updateItem);

            updateItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    DetectorFactoryCollection.instance().checkForUpdates(true);
                }
            });
            menuBar.add(helpMenu);
        }
        return menuBar;
    }

    void setViewMenu() {

        Cloud cloud = mainFrame.getBugCollection() == null ? null : mainFrame.getBugCollection().getCloud();

        viewMenu.removeAll();
        viewMenu.add(groupByMenuItem);
        if (cloud != null && cloud.supportsCloudSummaries()) {
            JMenuItem cloudReport = new JMenuItem("Cloud summary");
            cloudReport.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.displayCloudReport();

                }
            });
            viewMenu.add(cloudReport);
        }
        if (mainFrame.getProjectPackagePrefixes().size() > 0 && mainFrame.getBugCollection() != null) {
            JMenuItem selectPackagePrefixMenu = new JMenuItem("Select class search strings by project...");
            selectPackagePrefixMenu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.selectPackagePrefixByProject();

                }
            });
            viewMenu.add(selectPackagePrefixMenu);

        }
        if (viewMenu.getItemCount() > 0) {
            viewMenu.addSeparator();
        }

        ButtonGroup rankButtonGroup = new ButtonGroup();
        for (final ViewFilter.RankFilter r : ViewFilter.RankFilter.values()) {
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            AnnotatedString.localiseButton(rbMenuItem, "menu.rankFilter_"+r.name().toLowerCase(Locale.ENGLISH), r.toString(), true);
            rankButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.RankFilter.ALL) {
                rbMenuItem.setSelected(true);
            }
            rbMenuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setRank(r);
                    mainFrame.resetViewCache();
                }
            });
            viewMenu.add(rbMenuItem);
        }

        viewMenu.addSeparator();

        ButtonGroup priorityButtonGroup = new ButtonGroup();
        for (final ViewFilter.PriorityFilter r : ViewFilter.PriorityFilter.values()) {
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            AnnotatedString.localiseButton(rbMenuItem, "menu.priorityFilter_"+r.name().toLowerCase(Locale.ENGLISH), r.toString(), true);
            priorityButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.PriorityFilter.ALL_BUGS) {
                rbMenuItem.setSelected(true);
            }
            rbMenuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setPriority(r);
                    mainFrame.resetViewCache();
                }
            });
            viewMenu.add(rbMenuItem);
        }

        viewMenu.addSeparator();

        if (cloud != null && cloud.getMode() == Cloud.Mode.COMMUNAL) {
            ButtonGroup overallClassificationButtonGroup = new ButtonGroup();
            for (final ViewFilter.OverallClassificationFilter r : ViewFilter.OverallClassificationFilter.values()) {
                if (!r.supported(cloud)) {
                    continue;
                }
                JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
                AnnotatedString.localiseButton(rbMenuItem, "menu.classificatonFilter_"+r.name().toLowerCase(Locale.ENGLISH), r.toString(), true);
                overallClassificationButtonGroup.add(rbMenuItem);
                if (r == ViewFilter.OverallClassificationFilter.ALL) {
                    rbMenuItem.setSelected(true);
                }
                rbMenuItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        mainFrame.getViewFilter().setClassification(r);
                        mainFrame.resetViewCache();
                    }
                });
                viewMenu.add(rbMenuItem);
            }
            viewMenu.addSeparator();
        }

        ButtonGroup evalButtonGroup = new ButtonGroup();
        for (final ViewFilter.CloudFilter r : ViewFilter.CloudFilter.values()) {
            if (cloud != null && !r.supported(cloud)) {
                continue;
            }
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            AnnotatedString.localiseButton(rbMenuItem, "menu.cloudFilter_"+r.name().toLowerCase(Locale.ENGLISH), r.toString(), true);
            evalButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.CloudFilter.ALL) {
                rbMenuItem.setSelected(true);
            }
            rbMenuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setEvaluation(r);
                    mainFrame.resetViewCache();
                }
            });
            viewMenu.add(rbMenuItem);
        }
        viewMenu.addSeparator();
        ButtonGroup ageButtonGroup = new ButtonGroup();
        for (final ViewFilter.FirstSeenFilter r : ViewFilter.FirstSeenFilter.values()) {
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            AnnotatedString.localiseButton(rbMenuItem, "menu.firstSeenFilter_"+r.name().toLowerCase(Locale.ENGLISH), r.toString(), true);
            ageButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.FirstSeenFilter.ALL) {
                rbMenuItem.setSelected(true);
            }
            rbMenuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setFirstSeen(r);
                    mainFrame.resetViewCache();
                }
            });
            viewMenu.add(rbMenuItem);
        }
        viewMenu.addSeparator();
        final Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
        Collection<Matcher> filters = suppressionFilter.getChildren();
        JMenuItem filterMenu = new JMenuItem(filters.isEmpty() ? "Add Filters..." : "Filters...");

        filterMenu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PreferencesFrame preferenceFrame = PreferencesFrame.getInstance();
                preferenceFrame.showFilterPane();
                preferenceFrame.setLocationRelativeTo(mainFrame);
                preferenceFrame.setVisible(true);


            }
        });
        viewMenu.add(filterMenu);
        for(final Matcher m : filters) {
            JCheckBoxMenuItem f = new JCheckBoxMenuItem(m.toString(), suppressionFilter.isEnabled(m));
            viewMenu.add(f);
            f.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
                    suppressionFilter.setEnabled(m, enabled);
                    FilterActivity.notifyListeners(enabled ? Action.FILTERING : Action.UNFILTERING, null);

                }
            });


        }


    }

    /**
     * This checks if the xmlFile is in the GUISaveState. If not adds it. Then
     * adds the file to the recentMenuCache.
     *
     * @param xmlFile
     */
    /*
     * If the file already existed, its already in the preferences, as well as
     * the recent projects menu items, only add it if they change the name,
     * otherwise everything we're storing is still accurate since all we're
     * storing is the location of the file.
     */
    public void addFileToRecent(File xmlFile) {
        ArrayList<File> xmlFiles = GUISaveState.getInstance().getRecentFiles();
        if (!xmlFiles.contains(xmlFile)) {
            GUISaveState.getInstance().addRecentFile(xmlFile);
        }
        this.recentMenuCache.addRecentFile(xmlFile);
    }

    public JMenuItem getSaveMenuItem() {
        return saveMenuItem;
    }

    public JMenuItem getReconfigMenuItem() {
        return reconfigMenuItem;
    }

    public void enableRecentMenu(boolean enable) {
        recentMenu.setEnabled(enable);
    }

    public JMenuItem getPreferencesMenuItem() {
        return preferencesMenuItem;
    }

    void setSaveMenu(MainFrame mainFrame) {
        File s = mainFrame.getSaveFile();
        getSaveMenuItem().setEnabled(
                mainFrame.projectChanged() && s != null && mainFrame.getSaveType() != SaveType.FBP_FILE && s.exists());
    }

    /**
     * enable/disable preferences menu
     */
    public void enablePreferencesMenuItem(boolean b) {
        getPreferencesMenuItem().setEnabled(b);
        if (MainFrame.MAC_OS_X) {
            if (osxPrefsEnableMethod != null) {
                Object args[] = { b };
                try {
                    osxPrefsEnableMethod.invoke(osxAdapter, args);
                } catch (Exception e) {
                    System.err.println("Exception while enabling Preferences menu: " + e);
                }
            }
        }
    }

    public void initOSX() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        osxAdapter = Class.forName("edu.umd.cs.findbugs.gui2.OSXAdapter");
        Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", MainFrame.class);
        if (registerMethod != null) {
            registerMethod.invoke(osxAdapter, mainFrame);
        }
        osxPrefsEnableMethod = osxAdapter.getDeclaredMethod("enablePrefs", boolean.class);
    }

    public JMenuItem getCloseProjectItem() {
        return closeProjectItem;
    }

    public void enableOrDisableItems(Project curProject, BugCollection bugCollection) {
        boolean haveBugs = bugCollection != null;
        boolean haveCodeToAnalyze = curProject != null && !curProject.getFileList().isEmpty();
        redoAnalysis.setEnabled(haveBugs && haveCodeToAnalyze);
        closeProjectItem.setEnabled(haveBugs);
        saveMenuItem.setEnabled(haveBugs);
        saveAsMenuItem.setEnabled(haveBugs);
        reconfigMenuItem.setEnabled(haveBugs);
        groupByMenuItem.setEnabled(haveBugs);
    }

    static class CutAction extends TextAction {

        public CutAction() {
            super(L10N.getLocalString("txt.cut", "Cut"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null) {
                return;
            }

            text.cut();
        }
    }

    /**
     * @param map
     * @param navMenu
     */
    private void addNavItem(final ActionMap map, JMenu navMenu, String menuNameKey, String menuNameDefault, String actionName,
            int keyEvent) {
        JMenuItem toggleItem = newJMenuItem(menuNameKey, menuNameDefault);
        toggleItem.addActionListener(mainFrame.getMainFrameTree().treeActionAdapter(map, actionName));
        MainFrameHelper.attachAcceleratorKeyNoCtrl(toggleItem, keyEvent);
        navMenu.add(toggleItem);
    }

    static class CopyAction extends TextAction {

        public CopyAction() {
            super(L10N.getLocalString("txt.copy", "Copy"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null) {
                return;
            }

            text.copy();
        }
    }

    static class PasteAction extends TextAction {

        public PasteAction() {
            super(L10N.getLocalString("txt.paste", "Paste"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null) {
                return;
            }

            text.paste();
        }
    }
}
