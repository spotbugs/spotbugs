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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;

/**
 *
 * Lets you choose your new filter, shouldn't let you choose filters that
 * wouldn't filter anything out including filters that you already have
 *
 */
@SuppressWarnings("serial")
public class NewFilterFrame extends FBDialog {

    private final JList<String> list = new JList<>();

    private static NewFilterFrame instance = null;

    public static void open() {
        if (instance == null) {
            instance = new NewFilterFrame();
            instance.setVisible(true);
            instance.toFront();
        }
    }

    public static void close() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    private NewFilterFrame() {
        super(PreferencesFrame.getInstance());
        setContentPane(new JPanel() {
            @Override
            public Insets getInsets() {
                return new Insets(3, 3, 3, 3);
            }
        });
        setLayout(new BorderLayout());

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(Box.createHorizontalStrut(3));
        north.add(new JLabel(edu.umd.cs.findbugs.L10N.getLocalString("dlg.filter_bugs_lbl", "Filter out all bugs whose") + " "));

        // Argh divider
        Sortables[] sortables = MainFrame.getInstance().getAvailableSortables();
        Sortables[] valuesWithoutDivider = new Sortables[sortables.length - 1];
        int index = 0;

        for (int x = 0; x < sortables.length; x++) {
            if (sortables[x] != Sortables.DIVIDER) {
                valuesWithoutDivider[index] = sortables[x];
                index++;
            }
        }

        final JComboBox<Sortables> comboBox = new JComboBox<>(valuesWithoutDivider);
        comboBox.setRenderer(new ListCellRenderer<Sortables>() {
            final Color SELECTED_BACKGROUND = new Color(183, 184, 204);

            @Override
            public Component getListCellRendererComponent(JList<? extends Sortables> list, Sortables value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel result = new JLabel();
                result.setFont(result.getFont().deriveFont(Driver.getFontSize()));
                result.setOpaque(true);
                result.setText(value.toString().toLowerCase());
                if (isSelected) {
                    result.setBackground(SELECTED_BACKGROUND);
                }
                return result;
            }
        });
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Sortables filterBy = (Sortables) comboBox.getSelectedItem();
                String[] rawValues = filterBy.getAllSorted();
                String[] listData = new String[rawValues.length];
                for (int i = 0; i < listData.length; i++) {
                    listData[i] = filterBy.formatValue(rawValues[i]);
                }
                list.setListData(listData);
            }
        });
        comboBox.validate();
        north.add(comboBox);
        north.add(new JLabel(" " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.is", "is") + " "));
        String[] filterModes = { edu.umd.cs.findbugs.L10N.getLocalString("mode.equal_to", "equal to"),
                edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_after", "at or after"),
                edu.umd.cs.findbugs.L10N.getLocalString("mode.at_or_before", "at or before") };
        final JComboBox<String> filterModeComboBox = new JComboBox<>(filterModes);
        north.add(filterModeComboBox);
        north.add(new JLabel(":"));
        north.add(Box.createHorizontalGlue());
        JPanel south = new JPanel();
        JButton okButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.ok_btn", "OK"));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Sortables key = (Sortables) comboBox.getSelectedItem();
                String[] values = key.getAllSorted();

                ArrayList<SortableValue> filterMe = new ArrayList<SortableValue>();
                for (int i : list.getSelectedIndices()) {
                    filterMe.add(new BugAspects.SortableValue(key, values[i]));
                }
                try {
                    MainFrame.getInstance().getProject().getSuppressionFilter().addChild(FilterFactory.makeOrMatcher(filterMe));
                } catch (RuntimeException e) {
                    MainFrame.getInstance().showMessageDialog("Unable to create filter: " + e.getMessage());
                    close();
                    return;
                }
                FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
                PreferencesFrame.getInstance().updateFilterPanel();
                MainFrame.getInstance().setProjectChanged(true);
                close();
            }
        });
        JButton cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                close();
            }
        });
        GuiUtil.addOkAndCancelButtons(south, okButton, cancelButton);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    // Dupe from OK button's ActionListener
                    Sortables key = (Sortables) comboBox.getSelectedItem();
                    String[] values = key.getAllSorted();
                    // for (int i : list.getSelectedIndices())
                    // {
                    // FilterMatcher fm=new FilterMatcher(key,values[i]);
                    // if
                    // (!ProjectSettings.getInstance().getAllMatchers().contains(fm))
                    // ProjectSettings.getInstance().addFilter(fm);
                    // }
                    FilterMatcher[] newFilters = new FilterMatcher[list.getSelectedIndices().length];
                    for (int i = 0; i < newFilters.length; i++) {
                        newFilters[i] = new FilterMatcher(key, values[list.getSelectedIndices()[i]]);
                    }
                    ProjectSettings.getInstance().addFilters(newFilters);
                    PreferencesFrame.getInstance().updateFilterPanel();
                    close();
                }
            }
        });

        add(north, BorderLayout.NORTH);
        add(Box.createHorizontalStrut(2), BorderLayout.WEST);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(Box.createHorizontalStrut(2), BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        // Populate the box with initial values
        comboBox.getActionListeners()[0].actionPerformed(null);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                close();
            }
        });

        setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.create_new_filter_ttl", "Create New Filter"));
        pack();
    }

    public static void main(String[] args) {
        new NewFilterFrame().setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
