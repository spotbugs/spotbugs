/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.Insets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.filter.NotMatcher;

/**
 * Creates a list of options on for filtering bugs based on the current bug selected.
 *
 * Gives the option to invert the created filter by wrapping it in a {@link NotMatcher}.
 *
 * @author Graham Allan (grundlefleck@gmail.com)
 */
final class FilterFromBugPicker {

    private final HashMap<JCheckBox, Sortables> map = new HashMap<JCheckBox, Sortables>();
    private final BugInstance bug;
    private final List<Sortables> availableSortables;
    private final JPanel pickerPanel;
    private final JCheckBox notFilterCheck = new JCheckBox("Invert (i.e. filter bugs which do not match selected criteria).");

    public FilterFromBugPicker(BugInstance bug, List<Sortables> availableSortables) {
        this.bug = bug;
        this.availableSortables = availableSortables;
        this.pickerPanel = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(new Insets(6, 6, 6, 6)));

        addFilterLikeCheckboxes(panel);
        addNotFilterOption(panel);

        return panel;
    }

    private void addFilterLikeCheckboxes(JPanel center) {
        for (Sortables sortable : availableSortables) {
            if (!FilterFactory.canFilter(sortable)) { continue; }

            JCheckBox checkBox = new JCheckBox(sortable.toString() + " is " + sortable.formatValue(sortable.getFrom(bug)));

            map.put(checkBox, sortable);
            center.add(checkBox);
        }
    }

    private void addNotFilterOption(JPanel center) {
        center.add(new JSeparator(SwingConstants.HORIZONTAL));
        center.add(notFilterCheck);
    }

    public JPanel pickerPanel() {
        return pickerPanel;
    }

    public Matcher makeMatcherFromSelection() {
        HashSet<Sortables> set = new HashSet<Sortables>();
        for (Map.Entry<JCheckBox, Sortables> e : map.entrySet()) {
            if (e.getKey().isSelected()) {
                set.add(e.getValue());
            }
        }
        Matcher matcher = null;
        if (!set.isEmpty()) {
            matcher = FilterFactory.makeMatcher(set, bug);

            if(notFilterCheck.isSelected()) {
                matcher = FilterFactory.invertMatcher(matcher);
            }
        }

        return matcher;
    }

}