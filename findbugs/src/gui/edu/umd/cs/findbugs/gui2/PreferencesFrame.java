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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;

import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;

/**
 * User Preferences
 */

/*
 *
 * Preferences, which should really be renamed Filters And Suppressions (fas,
 * like fas file!) since thats all that's actually here now
 */
@SuppressWarnings("serial")
public class PreferencesFrame extends FBDialog {

    JTabbedPane mainTabPane;

    private static PreferencesFrame instance;

    private final CheckBoxList filterCheckBoxList = new CheckBoxList();

    private JButton addButton;

    JButton removeButton;

    JButton removeAllButton;

    boolean frozen = false;

    // Variables for Properties tab.
    private JTextField tabTextField;

    private JTextField fontTextField;

    private JTextField packagePrefixLengthTextField;

    private static int TAB_MIN = 1;

    private static int TAB_MAX = 20;

    private static int FONT_MIN = 10;

    private static int FONT_MAX = 99;

    public static PreferencesFrame getInstance() {
        // MainFrame.getInstance().getSorter().freezeOrder();
        if (instance == null)
            instance = new PreferencesFrame();

        return instance;
    }

    private PreferencesFrame() {
        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.fil_sup_ttl", "Preferences"));
        setModal(true);

        mainTabPane = new JTabbedPane();

        mainTabPane.add("Properties", createPropertiesPane());

        mainTabPane.add(edu.umd.cs.findbugs.L10N.getLocalString("pref.filters", "Filters"), createFilterPane());
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
            public void actionPerformed(ActionEvent evt) {
                // MainFrame.getInstance().getSorter().thawOrder();
                PreferencesFrame.this.setVisible(false);
                TreeModel bt = (MainFrame.getInstance().getTree().getModel());
                if (bt instanceof BugTreeModel)
                    ((BugTreeModel) bt).checkSorter();

                resetPropertiesPane();
            }
        }));
        bottom.add(Box.createHorizontalStrut(5));

        add(Box.createVerticalStrut(5));
        add(top);
        add(Box.createVerticalStrut(5));
        add(bottom);
        add(Box.createVerticalStrut(5));

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        pack();
    }



    Map<Plugin, Boolean> selectedStatus = new HashMap<Plugin, Boolean>();

    private JPanel createPluginPane() {
        final JPanel pluginPanel = new JPanel();
        final JPanel center = new JPanel();
        pluginPanel.setLayout(new BorderLayout());

        pluginPanel.add(center, BorderLayout.CENTER);

        BoxLayout centerLayout = new BoxLayout(center, BoxLayout.Y_AXIS);
        center.setLayout(centerLayout);
        Collection<Plugin> plugins = Plugin.getAllPlugins();
        for (final Plugin plugin : plugins) {
            if (plugin.isCorePlugin())
                continue;
            String text = plugin.getShortDescription();
            String id = plugin.getPluginId();
            if (text == null)
                text = id;
            final JCheckBox checkBox = new JCheckBox(text, plugin.isGloballyEnabled());
            String longText = plugin.getDetailedDescription();
            if (longText != null)
                checkBox.setToolTipText("<html>" + longText +"</html>");
            selectedStatus.put(plugin, plugin.isGloballyEnabled());
            checkBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    boolean selected = checkBox.isSelected();
                    selectedStatus.put(plugin, selected);
                }
            });
            center.add(checkBox);

        }

        JButton okButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.apply_btn", "Apply"));
        JButton cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
        JButton addButton = new JButton("Add");

        JPanel south = new JPanel();

        south.add(addButton);
        south.add(cancelButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "Select jar file containing plugin for FindBugs";
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory())
                        return true;
                    if (!f.canRead())
                        return false;
                    if (f.getName().endsWith(".jar"))
                        return true;
                    return false;
                }
            });
            int retvalue = chooser.showDialog(PreferencesFrame.this, "Select");

            if (retvalue == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    Plugin plugin = Plugin.addAvailablePlugin(f.toURI().toURL());
                    String shortText = plugin.getShortDescription();

                    JCheckBox checkBox = new JCheckBox(shortText, plugin.isGloballyEnabled());
                    center.add(checkBox);
                    center.validate();
                    PreferencesFrame.this.pack();
                } catch (MalformedURLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (PluginException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }


            }

            }});

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for(Map.Entry<Plugin,Boolean> entry : selectedStatus.entrySet()) {
                    Plugin plugin = entry.getKey();
                plugin.setGloballyEnabled(entry.getValue());
                }
                Project project = MainFrame.getInstance().getBugCollection().getProject();
                I18N i18n = I18N.newInstanceWithGloballyEnabledPlugins();
                project.setConfiguration(i18n);

                PreferencesFrame.this.dispose();

            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PreferencesFrame.this.dispose();

            }
        });

        south.add(okButton);
        pluginPanel.add(south, BorderLayout.SOUTH);

        return pluginPanel;
    }

    private JPanel createPropertiesPane() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JPanel mainPanel = new JPanel();

        float currFS = Driver.getFontSize();

        JPanel temp = new JPanel();
        temp.add(new JLabel("Tab size"));
        tabTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getTabSize()));
        tabTextField.setPreferredSize(new Dimension((int) (currFS * 4), (int) (currFS * 2)));
        temp.add(tabTextField);
        mainPanel.add(temp);
        mainPanel.add(Box.createVerticalStrut(5));

        temp = new JPanel();
        temp.add(new JLabel("Font size"));
        fontTextField = new JTextField(Float.toString(GUISaveState.getInstance().getFontSize()));
        fontTextField.setPreferredSize(new Dimension((int) (currFS * 6), (int) (currFS * 2)));
        temp.add(fontTextField);
        mainPanel.add(temp);
        mainPanel.add(Box.createVerticalGlue());

        temp = new JPanel();
        temp.add(new JLabel("Package prefix length"));
        packagePrefixLengthTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getPackagePrefixSegments()));
        packagePrefixLengthTextField.setPreferredSize(new Dimension((int) (currFS * 4), (int) (currFS * 2)));
        temp.add(packagePrefixLengthTextField);
        mainPanel.add(temp);
        mainPanel.add(Box.createVerticalGlue());

        contentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton(new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent evt) {
                changeTabSize();
                changeFontSize();
                changePackagePrefixLength();
            }
        }));

        bottomPanel.add(new JButton(new AbstractAction("Reset") {
            public void actionPerformed(ActionEvent evt) {
                resetPropertiesPane();
            }
        }));

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                resetPropertiesPane();
            }
        });

        return contentPanel;
    }

    private void changeTabSize() {
        int tabSize = 0;

        try {
            tabSize = Integer.decode(tabTextField.getText()).intValue();
        } catch (NumberFormatException exc) {
            JOptionPane
                    .showMessageDialog(instance, "Error in tab size field.", "Tab Size Error", JOptionPane.INFORMATION_MESSAGE);
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
        float fontSize = 0;

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
            JOptionPane.showMessageDialog(instance, "To implement the new font size please restart FindBugs.", "Changing Font",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void changePackagePrefixLength() {
        int value = 0;

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
    }

    /**
     * Create a JPanel to display the filtering controls.
     */
    private JPanel createFilterPane() {
        addButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.add_dot_btn", "Add..."));
        removeButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.remove_btn", "Remove"));
        removeAllButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.remove_all_btn", "Remove All"));
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridheight = 4;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        filterPanel.add(new JScrollPane(filterCheckBoxList), gbc);
        updateFilterPanel();

        gbc.gridheight = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
        filterPanel.add(addButton, gbc);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                NewFilterFrame.open();
            }
        });

        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
        filterPanel.add(removeButton, gbc);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                Object[] selected = filterCheckBoxList.getSelectedValues();
                boolean needsRebuild = false;
                if (selected.length == 0)
                    return;
                for (Object o : selected) {
                    MatchBox box = (MatchBox) o;
                    if (MainFrame.getInstance().getProject().getSuppressionFilter().isEnabled(box.getMatcher()))
                        needsRebuild = true;
                    MainFrame.getInstance().getProject().getSuppressionFilter().removeChild(box.getMatcher());
                }
                FilterActivity.notifyListeners(FilterListener.Action.UNFILTERING, null);
                MainFrame.getInstance().setProjectChanged(true);
                updateFilterPanel();
            }
        });
        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        filterPanel.add(removeAllButton, gbc);
        removeAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                boolean needsRebuild = false;
                Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
                if (!suppressionFilter.isEmpty())
                    needsRebuild = true;
                suppressionFilter.clear();

                if (needsRebuild)// TODO This will rebuild even if all the
                                 // filters being cleared were disabled
                    FilterActivity.notifyListeners(FilterListener.Action.UNFILTERING, null);
                MainFrame.getInstance().setProjectChanged(true);
                updateFilterPanel();
            }
        });
        gbc.gridy = 3;
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

        for (Iterator<Matcher> i = f.childIterator(); i.hasNext();) {
            final Matcher m = i.next();
            MatchBox box = new MatchBox(m.toString(), m);
            box.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt) {
                    boolean isSelected = ((JCheckBox) evt.getSource()).isSelected();
                    boolean wasSelected = f.isEnabled(m);
                    if (isSelected == wasSelected)
                        return;
                    f.setEnabled(m, isSelected);
                    FilterActivity.notifyListeners(isSelected ? FilterListener.Action.FILTERING
                            : FilterListener.Action.UNFILTERING, null);
                    MainFrame.getInstance().updateStatusBar();
                    MainFrame.getInstance().setProjectChanged(true);

                }
            });
            box.setSelected(f.isEnabled(m));
            boxes.add(box);
        }

        filterCheckBoxList.setListData(boxes.toArray(new MatchBox[boxes.size()]));
    }

    private static class UneditableTableModel extends DefaultTableModel {
        public UneditableTableModel(Object[][] tableData, String[] strings) {
            super(tableData, strings);
        }

        @Override
        public boolean isCellEditable(int x, int y) {
            return false;
        }
    }

    private static class FilterCheckBoxListener implements ItemListener {
        FilterMatcher filter;

        FilterCheckBoxListener(FilterMatcher filter) {
            this.filter = filter;
        }

        public void itemStateChanged(ItemEvent evt) {
            // RebuildThreadMonitor.waitForRebuild();
            filter.setActive(((JCheckBox) evt.getSource()).isSelected());
            MainFrame.getInstance().updateStatusBar();
            MainFrame.getInstance().setProjectChanged(true);
        }
    }

    void freeze() {
        frozen = true;
        filterCheckBoxList.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    void thaw() {
        filterCheckBoxList.setEnabled(true);
        addButton.setEnabled(true);
        removeButton.setEnabled(true);
        frozen = false;
    }

    void setSelectedTab(int index) {
        if (index > 0 && index <= mainTabPane.getTabCount())
            mainTabPane.setSelectedIndex(index);
    }
}
