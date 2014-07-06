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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeModel;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.util.LaunchBrowser;

@SuppressWarnings("serial")
public class PreferencesFrame extends FBDialog {
    private static final Logger LOGGER = Logger.getLogger(PreferencesFrame.class.getName());

    private static final int TAB_MIN = 1;
    private static final int TAB_MAX = 20;

    private static final int FONT_MIN = 10;
    private static final int FONT_MAX = 99;

    private static PreferencesFrame instance;

    private final CheckBoxList<MatchBox> filterCheckBoxList = new CheckBoxList<>();

    // Variables for Properties tab.
    private JTextField tabTextField;
    private JTextField fontTextField;

    private JTextField packagePrefixLengthTextField;

    private final Map<Plugin, EnabledSettings> pluginEnabledStatus = new HashMap<Plugin, EnabledSettings>();
    private JPanel pluginPanelCenter;

    private static class EnabledSettings {
        public boolean global;
        public Boolean project;

        private EnabledSettings(boolean global, Boolean project) {
            this.global = global;
            this.project = project;
        }
    }

    public static PreferencesFrame getInstance() {
        if (instance == null) {
            instance = new PreferencesFrame();
        }

        return instance;
    }

    private boolean pluginsAdded = false;
    private final JPanel filterPane;
    private final JTabbedPane mainTabPane;

    public void showFilterPane() {
        mainTabPane.setSelectedComponent(filterPane);

    }
    private PreferencesFrame() {
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.fil_sup_ttl", "Preferences"));
        setModal(true);

        mainTabPane = new JTabbedPane();

        mainTabPane.add("General", createPropertiesPane());

        filterPane = createFilterPane();
        mainTabPane.add(edu.umd.cs.findbugs.L10N.getLocalString("pref.filters", "Filters"), filterPane);
        mainTabPane.add("Plugins", createPluginPane());

        MainFrame.getInstance().updateStatusBar();
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        top.add(Box.createHorizontalStrut(5));
        top.add(mainTabPane);
        top.add(Box.createHorizontalStrut(5));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        bottom.add(Box.createHorizontalGlue());
        bottom.add(new JButton(new AbstractAction(edu.umd.cs.findbugs.L10N.getLocalString("pref.close", "Close")) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                handleWindowClose();
                PreferencesFrame.this.setVisible(false);
            }
        }));
        bottom.add(Box.createHorizontalStrut(5));

        add(Box.createVerticalStrut(5));
        add(top);
        add(Box.createVerticalStrut(5));
        add(bottom);
        add(Box.createVerticalStrut(5));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                resetPropertiesPane();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                handleWindowClose();
            }
        });
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        pack();
    }

    private void handleWindowClose() {
        TreeModel bt = (MainFrame.getInstance().getTree().getModel());
        if (bt instanceof BugTreeModel) {
            ((BugTreeModel) bt).checkSorter();
        }
        Project project = getCurrentProject();

        boolean changed = pluginsAdded;
        pluginsAdded = false;
        List<String> enabledPlugins = new ArrayList<String>();
        List<String> disabledPlugins = new ArrayList<String>();
        for (Map.Entry<Plugin, EnabledSettings> entry : pluginEnabledStatus.entrySet()) {
            Plugin plugin = entry.getKey();
            EnabledSettings enabled = entry.getValue();
            if (project != null) {
                Boolean newSetting = enabled.project;
                Boolean existingSetting = project.getPluginStatus(plugin);
                boolean sameSettings = Objects.equals(existingSetting, newSetting);
                if (!sameSettings) {
                    project.setPluginStatusTrinary(plugin.getPluginId(), newSetting);
                    changed = true;
                }
            }
            if (enabled.global) {
                enabledPlugins.add(plugin.getPluginId());
            } else {
                disabledPlugins.add(plugin.getPluginId());
            }
            if (plugin.isGloballyEnabled() != enabled.global) {
                plugin.setGloballyEnabled(enabled.global);
                changed = true;
            }
        }

        if (changed && project != null) {
            MainFrame.getInstance().updateBugTree();
            MainFrame.getInstance().setProjectChanged(true);
        }
        if (project == null) {
            GUISaveState.getInstance().setPluginsEnabled(enabledPlugins, disabledPlugins);
            GUISaveState.getInstance().save();
        }
    }

    private Project getCurrentProject() {
        BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
        return bugCollection == null ? null : bugCollection.getProject();
    }

    private JPanel createPluginPane() {
        final JPanel pluginPanel = new JPanel();
        pluginPanel.setLayout(new BorderLayout());
        pluginPanelCenter = new JPanel();

        pluginPanel.add(pluginPanelCenter, BorderLayout.CENTER);

        pluginPanelCenter.setBorder(new EmptyBorder(10,10,10,10));
        pluginPanelCenter.setLayout(new GridBagLayout());

        JButton addButton = new JButton("Install new plugin...");
        JPanel south = new JPanel();

        south.add(addButton);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "FindBugs Plugin (*.jar)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        if (!f.canRead()) {
                            return false;
                        }
                        if (f.getName().endsWith(".jar")) {
                            return true;
                        }
                        return false;
                    }
                });
                chooser.setDialogTitle("Select a FindBugs plugin jar");
                int retvalue = chooser.showDialog(PreferencesFrame.this, "Install");

                if (retvalue == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    try {
                        // load and enable for project (if loaded)
                        Plugin plugin = Plugin.loadCustomPlugin(f, PreferencesFrame.this.getCurrentProject());

                        GUISaveState guiSaveState = GUISaveState.getInstance();
                        URL url = f.toURI().toURL();
                        // add to FBGUI custom plugins list
                        guiSaveState.addCustomPlugin(url);
                        // add to list of enabled plugins
                        guiSaveState.setPluginEnabled(plugin.getPluginId());
                        plugin.setGloballyEnabled(true);
                        guiSaveState.save();
                        pluginsAdded = true;
                        rebuildPluginCheckboxes();

                    } catch (PluginException | MalformedURLException e1) {
                        LOGGER.log(Level.WARNING, "Could not load " + f.getPath(), e1);
                        JOptionPane.showMessageDialog(PreferencesFrame.this, "Could not load " + f.getPath()
                                + "\n\n"
                                + e1.getClass().getSimpleName() + ": " + e1.getMessage(),
                                "Error Loading Plugin", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        pluginPanel.add(south, BorderLayout.SOUTH);

        return pluginPanel;
    }

    EnabledSettings isEnabled(@CheckForNull Project project, Plugin plugin) {
        return new EnabledSettings(plugin.isGloballyEnabled(), project == null ? null : project.getPluginStatus(plugin));
    }

    private void rebuildPluginCheckboxes() {
        Project currentProject = getCurrentProject();
        pluginPanelCenter.removeAll();
        if (currentProject != null) {
            GridBagConstraints g = new GridBagConstraints();
            g.fill = GridBagConstraints.NONE;
            g.insets = new Insets(5,5,5,5);
            g.gridy = 0;
            //        g.anchor = GridBagConstraints.WEST;
            //        g.gridx = 1;
            //        pluginPanelCenter.add(new JLabel("Global Setting"), g);
            g.anchor = GridBagConstraints.CENTER;
            g.gridx = 2;
            pluginPanelCenter.add(new JLabel("Project Setting"), g);
        }
        Collection<Plugin> plugins = Plugin.getAllPlugins();
        int added = 0;
        for (final Plugin plugin : plugins) {
            if (plugin.isCorePlugin()) {
                continue;
            }
            String text = plugin.getShortDescription();
            String id = plugin.getPluginId();
            if (text == null) {
                text = id;
            }
            final URL url = plugin.getPluginLoader().getURL();
            String pluginUrlStr = url.toExternalForm();
            if ("file".equals(url.getProtocol())) {
                try {
                    pluginUrlStr = new File(URLDecoder.decode(url.getPath(), "UTF-8")).getAbsolutePath();
                } catch (UnsupportedEncodingException e) {

                }
            }
            text = String.format("<html>%s<br><font style='font-weight:normal;font-style:italic'>%s",
                    text, pluginUrlStr);

            EnabledSettings enabled = isEnabled(currentProject, plugin);
            final JCheckBox checkGlobal = new JCheckBox(text, enabled.global);
            final boolean cannotDisable = plugin.isEnabledByDefault() && plugin.cannotDisable();
            if (cannotDisable) {
                if (!enabled.global) {
                    throw new IllegalStateException(
                            plugin.getPluginId() + " is enabled by default and cannot be disabled, but is disabled");
                }
                checkGlobal.setEnabled(false);
            } else {
                checkGlobal.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem item = new JMenuItem("Uninstall " + plugin.getShortDescription() + "...");
                            item.addActionListener(new UninstallClickListener(plugin, url));
                            menu.add(item);
                            menu.show(checkGlobal, e.getX(), e.getY());
                        }
                    }
                });
                checkGlobal.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pluginEnabledStatus.get(plugin).global = checkGlobal.isSelected();
                    }
                });
            }
            checkGlobal.setVerticalTextPosition(SwingConstants.TOP);
            String longText = plugin.getDetailedDescription();
            if (longText != null) {
                checkGlobal.setToolTipText("<html>" + longText + "</html>");
            }
            pluginEnabledStatus.put(plugin, enabled);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.gridx = 1;
            gbc.insets = new Insets(0,5,0,5);
            gbc.gridy = added+1;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            pluginPanelCenter.add(checkGlobal, gbc);

            if (currentProject != null && !cannotDisable) {
                final JComboBox<String> combo = new WideComboBox<>(new String[]{"DEFAULT", "DISABLED", "ENABLED"});
                if (enabled.project == null) {
                    combo.setSelectedIndex(0);
                } else {
                    combo.setSelectedIndex(enabled.project ? 2 : 1);
                }
                combo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        if (index == -1) {
                            if (value.equals("DEFAULT")) {
                                value = "Default";
                            }
                            if (value.equals("DISABLED")) {
                                value = "Disabled";
                            }
                            if (value.equals("ENABLED")) {
                                value = "Enabled";
                            }
                        } else {
                            if (value.equals("DEFAULT")) {
                                value = "Default (use global setting)";
                            }
                            if (value.equals("DISABLED")) {
                                value = "Disabled for this project";
                            }
                            if (value.equals("ENABLED")) {
                                value = "Enabled for this project";
                            }
                        }
                        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    }
                });
                combo.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Boolean[] array = { null, false, true };
                        int i = combo.getSelectedIndex();
                        if (i < 0 || i > 2)
                        {
                            return; // ??
                        }
                        pluginEnabledStatus.get(plugin).project = array[i];
                    }
                });
                gbc.gridx = 2;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0;
                pluginPanelCenter.add(combo, gbc);
            }
            added++;
        }
        if (added == 0) {
            JLabel label = new JLabel("<html>No plugins are loaded.<br> " +
                    "Try installing <u><font color=blue>fb-contrib</font></u> - or write your own<br>" +
                    "plugin for your project's needs!");
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        LaunchBrowser.showDocument(new URL("https://sourceforge.net/projects/fb-contrib/"));
                    } catch (MalformedURLException e1) {
                        throw new IllegalStateException(e1);
                    }
                }
            });
            label.setBorder(new EmptyBorder(10,10,10,10));
            pluginPanelCenter.add(label);
        }
        PreferencesFrame.this.pack();
    }

    private void addField(JPanel p, GridBagConstraints c, int y, String lbl, JComponent field) {
        c.gridy = y;
        JLabel l = new JLabel(lbl, SwingConstants.TRAILING);
        l.setLabelFor(field);
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx = 0;
        p.add(l, c);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 1;
        p.add(field, c);
    }
    private JPanel createPropertiesPane() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = c.ipady = 5;


        float currFS = Driver.getFontSize();

        tabTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getTabSize()));
        tabTextField.setPreferredSize(new Dimension((int) (currFS * 4), (int) (currFS * 2)));
        addField(mainPanel, c, 0, "Tab size", tabTextField);

        fontTextField = new JTextField(Float.toString(GUISaveState.getInstance().getFontSize()));
        fontTextField.setPreferredSize(new Dimension((int) (currFS * 6), (int) (currFS * 2)));
        addField(mainPanel, c, 1, "Font size", fontTextField);

        packagePrefixLengthTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getPackagePrefixSegments()));
        packagePrefixLengthTextField.setPreferredSize(new Dimension((int) (currFS * 4), (int) (currFS * 2)));
        addField(mainPanel, c, 2, "Package prefix length", packagePrefixLengthTextField);

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Apply") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                changeTabSize();
                changeFontSize();
                changePackagePrefixLength();
            }
        }));

        bottomPanel.add(new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                resetPropertiesPane();
            }
        }));

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        return contentPanel;
    }

    static class PluginWithDescription {

        public PluginWithDescription(String description, CloudPlugin plugin) {
            this.description = description;
            //            this.plugin = plugin;
        }
        final String description;
        //        final CloudPlugin plugin;

        @Override
        public String toString() {
            return description;
        }

    }
    private void changeTabSize() {
        int tabSize;

        try {
            tabSize = Integer.decode(tabTextField.getText());
        } catch (NumberFormatException exc) {
            JOptionPane.showMessageDialog(instance, "Error in tab size field.", "Tab Size Error",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (tabSize < TAB_MIN || tabSize > TAB_MAX) {
            JOptionPane.showMessageDialog(instance, "Tab size exceedes range (" + TAB_MIN + " - " + TAB_MAX + ").",
                    "Tab Size Excedes Range", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (tabSize != GUISaveState.getInstance().getTabSize()) {
            GUISaveState.getInstance().setTabSize(tabSize);
            MainFrame.getInstance().getSourceCodeDisplayer().clearCache();
            // This causes the GUI to redisplay the current code
            MainFrame.getInstance().syncBugInformation();
        }
    }

    private void changeFontSize() {
        float fontSize;

        try {
            fontSize = Float.parseFloat(fontTextField.getText());
        } catch (NumberFormatException exc) {
            JOptionPane.showMessageDialog(instance, "Error in font size field.", "Font Size Error",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (fontSize < FONT_MIN || fontSize > FONT_MAX) {
            JOptionPane.showMessageDialog(instance, "Font size exceedes range (" + FONT_MIN + " - " + FONT_MAX + ").",
                    "Font Size Exceedes Range", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (fontSize != GUISaveState.getInstance().getFontSize()) {
            GUISaveState.getInstance().setFontSize(fontSize);
            JOptionPane.showMessageDialog(instance, "To implement the new font size please restart FindBugs.",
                    "Changing Font",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void changePackagePrefixLength() {
        int value;

        try {
            value = Integer.parseInt(packagePrefixLengthTextField.getText());
        } catch (NumberFormatException exc) {
            JOptionPane.showMessageDialog(instance, "Error in package prefix length field.", "Package Prefix Length Error",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (value < 1 || value > 12) {
            JOptionPane.showMessageDialog(instance, "package prefix length exceedes range (" + 1 + " - " + 12 + ").",
                    "package prefix lengthe exceedes range", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (value != GUISaveState.getInstance().getPackagePrefixSegments()) {
            GUISaveState.getInstance().setPackagePrefixSegments(value);
            BugTreeModel bt = (BugTreeModel) (MainFrame.getInstance().getTree().getModel());
            bt.needToRebuild();
            bt.checkSorter();
        }

    }

    private void resetPropertiesPane() {
        tabTextField.setText(Integer.toString(GUISaveState.getInstance().getTabSize()));
        fontTextField.setText(Float.toString(GUISaveState.getInstance().getFontSize()));
        rebuildPluginCheckboxes();
    }

    /**
     * Create a JPanel to display the filtering controls.
     */
    private JPanel createFilterPane() {
        JButton addButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.add_dot_btn", "Add..."));
        JButton removeButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.remove_btn", "Remove"));
        JButton removeAllButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.remove_all_btn", "Remove All"));
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        filterPanel.add(new JLabel("<HTML>These rules control which bugs are shown and which are hidden in this project"), gbc);

        gbc.gridheight = 4;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        filterPanel.add(new JScrollPane(filterCheckBoxList), gbc);
        updateFilterPanel();

        gbc.gridheight = 1;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
        filterPanel.add(addButton, gbc);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                NewFilterFrame.open();
            }
        });

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 0, 0);
        filterPanel.add(removeButton, gbc);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object[] selected = filterCheckBoxList.getSelectedValues();
                if (selected.length == 0) {
                    return;
                }
                for (Object o : selected) {
                    MatchBox box = (MatchBox) o;
                    MainFrame.getInstance().getProject().getSuppressionFilter().removeChild(box.getMatcher());
                }
                FilterActivity.notifyListeners(FilterListener.Action.UNFILTERING, null);
                MainFrame.getInstance().setProjectChanged(true);
                updateFilterPanel();
            }
        });
        gbc.gridy = 3;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        filterPanel.add(removeAllButton, gbc);
        removeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean needsRebuild = false;
                Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
                if (!suppressionFilter.isEmpty()) {
                    needsRebuild = true;
                }
                suppressionFilter.clear();

                if (needsRebuild) {
                    // filters being cleared were disabled
                    FilterActivity.notifyListeners(FilterListener.Action.UNFILTERING, null);
                }
                MainFrame.getInstance().setProjectChanged(true);
                updateFilterPanel();
            }
        });
        gbc.gridy = 4;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        filterPanel.add(Box.createGlue(), gbc);

        return filterPanel;
    }

    private static class MatchBox extends JCheckBox {
        Matcher m;

        MatchBox(String text, Matcher m) {
            super(text);
            this.m = m;
        }

        Matcher getMatcher() {
            return m;
        }
    }

    void updateFilterPanel() {
        ArrayList<MatchBox> boxes = new ArrayList<MatchBox>();
        final Filter f = MainFrame.getInstance().getProject().getSuppressionFilter();

        for (final Matcher m : f.getChildren()) {
            MatchBox box = new MatchBox(m.toString(), m);
            box.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent evt) {
                    boolean isSelected = ((JCheckBox) evt.getSource()).isSelected();
                    boolean wasSelected = f.isEnabled(m);
                    if (isSelected == wasSelected) {
                        return;
                    }
                    f.setEnabled(m, isSelected);
                    updateFilters(isSelected);

                }
            });
            box.setSelected(f.isEnabled(m));
            boxes.add(box);
        }

        filterCheckBoxList.setListData(boxes.toArray(new MatchBox[boxes.size()]));
    }


    public static void updateFilters(boolean addedFilter) {
        FilterActivity.notifyListeners(addedFilter ? FilterListener.Action.FILTERING
                : FilterListener.Action.UNFILTERING, null);
        MainFrame.getInstance().setProjectChanged(true);
    }

    private class UninstallClickListener implements ActionListener {
        private final Plugin plugin;
        private final URL url;

        public UninstallClickListener(Plugin plugin, URL url) {
            this.plugin = plugin;
            this.url = url;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showOptionDialog(PreferencesFrame.this,
                    "Are you sure you want to uninstall " + plugin.getShortDescription() + "?" +
                            "\n\nNo files will be deleted from your computer.", "",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            new Object[]{"Uninstall", "Cancel"}, "Cancel");
            if (result == 0) {
                if (!GUISaveState.getInstance().removeCustomPlugin(url)) {
                    if ("file".equals(url.getProtocol())) {
                        String path = url.toExternalForm();
                        try {
                            path = new File(URLDecoder.decode(url.getPath(), "UTF-8")).getAbsolutePath();
                        } catch (UnsupportedEncodingException ex) {
                        }
                        try {
                            StringSelection contents = new StringSelection(path);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, contents);
                        } catch (Exception e1) {
                        }
                        JOptionPane.showMessageDialog(PreferencesFrame.this,
                                "The plugin could not be uninstalled automatically.\n\n" +
                                        "You can try to delete this plugin manually: \n"
                                        + path + "\n\n(This path has been copied to your clipboard)",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(PreferencesFrame.this,
                                "This plugin is not actually in the list of plugins...\n" +
                                        "Not sure what to do...\n "
                                        + url.toExternalForm()
                                        + "\n\nPlugin URL's:\n" +
                                        GUISaveState.getInstance().getCustomPlugins(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(PreferencesFrame.this,
                            "Changes will take effect after you restart FindBugs.");
                }
            }
        }
    }
}
