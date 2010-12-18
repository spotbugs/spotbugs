package edu.umd.cs.findbugs.gui2;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.cloud.Cloud;

public class MainFrameMenu implements Serializable {
    private final MainFrame mainFrame;

    JMenuItem reconfigMenuItem = MainFrameHelper.newJMenuItem("menu.reconfig", "Reconfigure...", KeyEvent.VK_F);

    JMenuItem redoAnalysis;

    RecentMenu recentMenuCache;

    JMenu recentMenu;

    JMenuItem preferencesMenuItem;

    JMenu viewMenu;

    JMenuItem saveMenuItem = MainFrameHelper.newJMenuItem("menu.save_item", "Save", KeyEvent.VK_S);

    private Class<?> osxAdapter;

    private Method osxPrefsEnableMethod;

    public MainFrameMenu(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JMenuItem createRecentItem(final File f, final SaveType localSaveType) {
        if (MainFrame.GUI2_DEBUG)
            System.out.println("createRecentItem(" + f + ", " + localSaveType + ")");
        String name = f.getName();

        final JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {
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

                    if (!f.exists())
                        throw new IllegalStateException("User used a recent projects menu item that didn't exist.");

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
                            if (mainFrame.getSaveFile() != null)
                                mainFrame.getMainFrameLoadSaveHelper().save();
                            else
                                mainFrame.getMainFrameLoadSaveHelper().saveAs();
                        } else if (response == JOptionPane.CANCEL_OPTION)
                            return;
                        // IF no, do nothing.
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
        JMenu fileMenu = MainFrameHelper.newJMenu("menu.file_menu", "File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenu editMenu = MainFrameHelper.newJMenu("menu.edit_menu", "Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        // Edit fileMenu JMenu object.
        JMenuItem openMenuItem = MainFrameHelper.newJMenuItem("menu.open_item", "Open...", KeyEvent.VK_O);
        recentMenu = MainFrameHelper.newJMenu("menu.recent", "Recent");
        recentMenuCache = new RecentMenu(recentMenu);
        JMenuItem saveAsMenuItem = MainFrameHelper.newJMenuItem("menu.saveas_item", "Save As...", KeyEvent.VK_A);
        JMenuItem importFilter = MainFrameHelper.newJMenuItem("menu.importFilter_item", "Import filter...");
        JMenuItem exportFilter = MainFrameHelper.newJMenuItem("menu.exportFilter_item", "Export filter...");

        JMenuItem exitMenuItem = null;
        if (!MainFrame.MAC_OS_X) {
            exitMenuItem = MainFrameHelper.newJMenuItem("menu.exit", "Exit", KeyEvent.VK_X);
            exitMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.callOnClose();
                }
            });
        }
        JMenu windowMenu = mainFrame.getGuiLayout().createWindowMenu();

        JMenuItem newProjectMenuItem = null;
        if (!FindBugs.noAnalysis) {
            newProjectMenuItem = MainFrameHelper.newJMenuItem("menu.new_item", "New Project", KeyEvent.VK_N);

            MainFrameHelper.attachAcceleratorKey(newProjectMenuItem, KeyEvent.VK_N);

            newProjectMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.createNewProjectFromMenuItem();
                }
            });
        }

        reconfigMenuItem.setEnabled(false);
        MainFrameHelper.attachAcceleratorKey(reconfigMenuItem, KeyEvent.VK_F);
        reconfigMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (!mainFrame.canNavigateAway())
                    return;
                new NewProjectWizard(mainFrame.getCurProject());
            }
        });

        JMenuItem mergeMenuItem = MainFrameHelper.newJMenuItem("menu.mergeAnalysis", "Merge Analysis...");

        mergeMenuItem.setEnabled(true);
        mergeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().mergeAnalysis();
            }
        });

        if (!FindBugs.noAnalysis) {
            redoAnalysis = MainFrameHelper.newJMenuItem("menu.rerunAnalysis", "Redo Analysis", KeyEvent.VK_R);

            redoAnalysis.setEnabled(false);
            MainFrameHelper.attachAcceleratorKey(redoAnalysis, KeyEvent.VK_R);
            redoAnalysis.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.redoAnalysis();
                }
            });
        }

        openMenuItem.setEnabled(true);
        MainFrameHelper.attachAcceleratorKey(openMenuItem, KeyEvent.VK_O);
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().open();
            }
        });

        saveAsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().saveAs();
            }
        });
        exportFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().exportFilter();
            }
        });
        importFilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().importFilter();
            }
        });
        saveMenuItem.setEnabled(false);
        MainFrameHelper.attachAcceleratorKey(saveMenuItem, KeyEvent.VK_S);
        saveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.getMainFrameLoadSaveHelper().save();
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

        // Edit editMenu Menu object.
        JMenuItem cutMenuItem = new JMenuItem(new CutAction());
        JMenuItem copyMenuItem = new JMenuItem(new CopyAction());
        JMenuItem pasteMenuItem = new JMenuItem(new PasteAction());
        preferencesMenuItem = MainFrameHelper.newJMenuItem("menu.preferences_menu", "Preferences...");
        JMenuItem sortMenuItem = MainFrameHelper.newJMenuItem("menu.sortConfiguration", "Sort Configuration...");
        JMenuItem goToLineMenuItem = MainFrameHelper.newJMenuItem("menu.gotoLine", "Go to line...");

        MainFrameHelper.attachAcceleratorKey(cutMenuItem, KeyEvent.VK_X);
        MainFrameHelper.attachAcceleratorKey(copyMenuItem, KeyEvent.VK_C);
        MainFrameHelper.attachAcceleratorKey(pasteMenuItem, KeyEvent.VK_V);

        preferencesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mainFrame.preferences();
            }
        });

        sortMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (!mainFrame.canNavigateAway())
                    return;
                SorterDialog.getInstance().setLocationRelativeTo(mainFrame);
                SorterDialog.getInstance().setVisible(true);
            }
        });

        MainFrameHelper.attachAcceleratorKey(goToLineMenuItem, KeyEvent.VK_L);
        goToLineMenuItem.addActionListener(new ActionListener() {
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
        editMenu.add(sortMenuItem);

        menuBar.add(editMenu);

        if (windowMenu != null)
            menuBar.add(windowMenu);

        viewMenu = MainFrameHelper.newJMenu("menu.view", "View");
        setViewMenu();
        menuBar.add(viewMenu);

        final ActionMap map = mainFrame.getMainFrameTree().getTree().getActionMap();

        JMenu navMenu = MainFrameHelper.newJMenu("menu.navigation", "Navigation");

        addNavItem(map, navMenu, "menu.expand", "Expand", "expand", KeyEvent.VK_RIGHT);
        addNavItem(map, navMenu, "menu.collapse", "Collapse", "collapse", KeyEvent.VK_LEFT);
        addNavItem(map, navMenu, "menu.up", "Up", "selectPrevious", KeyEvent.VK_UP);
        addNavItem(map, navMenu, "menu.down", "Down", "selectNext", KeyEvent.VK_DOWN);

        menuBar.add(navMenu);

        JMenu designationMenu = MainFrameHelper.newJMenu("menu.designation", "Designation");
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
            JMenu helpMenu = MainFrameHelper.newJMenu("menu.help_menu", "Help");
            JMenuItem aboutItem = MainFrameHelper.newJMenuItem("menu.about_item", "About FindBugs");
            helpMenu.add(aboutItem);

            aboutItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    mainFrame.about();
                }
            });
            menuBar.add(helpMenu);
        }
        return menuBar;
    }

    void setViewMenu() {

        Cloud cloud = mainFrame.getBugCollection() == null ? null : mainFrame.getBugCollection().getCloud();

        viewMenu.removeAll();
        if (cloud != null && cloud.supportsCloudSummaries()) {
            JMenuItem cloudReport = new JMenuItem("Cloud summary");
            cloudReport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mainFrame.displayCloudReport();

                }
            });
            viewMenu.add(cloudReport);
        }
        if (mainFrame.getProjectPackagePrefixes().size() > 0 && mainFrame.getBugCollection() != null) {
            JMenuItem selectPackagePrefixMenu = new JMenuItem("Select class search strings by project...");
            selectPackagePrefixMenu.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mainFrame.selectPackagePrefixByProject();

                }
            });
            viewMenu.add(selectPackagePrefixMenu);

        }
        if (viewMenu.getItemCount() > 0)
            viewMenu.addSeparator();

        ButtonGroup rankButtonGroup = new ButtonGroup();
        for (final ViewFilter.RankFilter r : ViewFilter.RankFilter.values()) {
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            rankButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.RankFilter.ALL)
                rbMenuItem.setSelected(true);
            rbMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setRank(r);
                    mainFrame.resetViewCache();
                }
            });
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
            if (cloud != null && !r.supported(cloud))
                continue;
            JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(r.toString());
            evalButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.CloudFilter.ALL)
                rbMenuItem.setSelected(true);
            rbMenuItem.addActionListener(new ActionListener() {

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
            ageButtonGroup.add(rbMenuItem);
            if (r == ViewFilter.FirstSeenFilter.ALL)
                rbMenuItem.setSelected(true);
            rbMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mainFrame.getViewFilter().setFirstSeen(r);
                    mainFrame.resetViewCache();
                }
            });
            viewMenu.add(rbMenuItem);
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

    public JMenuItem getRedoAnalysisItem() {
        return redoAnalysis;
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
        Class[] defArgs = { MainFrame.class };
        Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
        if (registerMethod != null) {
            registerMethod.invoke(osxAdapter, mainFrame);
        }
        defArgs[0] = boolean.class;
        osxPrefsEnableMethod = osxAdapter.getDeclaredMethod("enablePrefs", defArgs);
    }

    static class CutAction extends TextAction {

        public CutAction() {
            super(L10N.getLocalString("txt.cut", "Cut"));
        }

        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null)
                return;

            text.cut();
        }
    }

    /**
     * @param map
     * @param navMenu
     */
    private void addNavItem(final ActionMap map, JMenu navMenu, String menuNameKey, String menuNameDefault, String actionName,
            int keyEvent) {
        JMenuItem toggleItem = MainFrameHelper.newJMenuItem(menuNameKey, menuNameDefault);
        toggleItem.addActionListener(mainFrame.getMainFrameTree().treeActionAdapter(map, actionName));
        MainFrameHelper.attachAcceleratorKey(toggleItem, keyEvent);
        navMenu.add(toggleItem);
    }

    static class CopyAction extends TextAction {

        public CopyAction() {
            super(L10N.getLocalString("txt.copy", "Copy"));
        }

        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null)
                return;

            text.copy();
        }
    }

    static class PasteAction extends TextAction {

        public PasteAction() {
            super(L10N.getLocalString("txt.paste", "Paste"));
        }

        public void actionPerformed(ActionEvent evt) {
            JTextComponent text = getTextComponent(evt);

            if (text == null)
                return;

            text.paste();
        }
    }
}
