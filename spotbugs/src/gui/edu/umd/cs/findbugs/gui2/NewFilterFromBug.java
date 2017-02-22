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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.filter.Matcher;


/**
 * Allows you to make a new Filter by right clicking (control clicking) on a bug
 * in the tree
 */
@SuppressWarnings("serial")
public class NewFilterFromBug extends FBDialog {

    private static final List<NewFilterFromBug> listOfAllFrames = new ArrayList<NewFilterFromBug>();

    public NewFilterFromBug(final FilterFromBugPicker filterFromBugPicker, final ApplyNewFilter applyNewFilter) {
        this.setModal(true);
        listOfAllFrames.add(this);
        setLayout(new BorderLayout());

        add(new JLabel("Filter out all bugs whose..."), BorderLayout.NORTH);

        JPanel center = filterFromBugPicker.pickerPanel();
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton okButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.ok_btn", "OK"));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Matcher matcherFromSelection = filterFromBugPicker.makeMatcherFromSelection();
                applyNewFilter.fromMatcher(matcherFromSelection);
                closeDialog();
            }
        });
        JButton cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                closeDialog();
            }
        });
        GuiUtil.addOkAndCancelButtons(south, okButton, cancelButton);
        add(south, BorderLayout.SOUTH);

        pack();
        setVisible(true);
    }

    private final void closeDialog() {
        NewFilterFromBug.this.dispose();
    }

    static void closeAll() {
        for (NewFilterFromBug frame : listOfAllFrames) {
            frame.dispose();
        }
        listOfAllFrames.clear();
    }
}
