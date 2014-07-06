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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.BugFilingStatus;
import edu.umd.cs.findbugs.util.LaunchBrowser;

/**
 * @author pugh
 * @author Keith Lea
 */
public class CommentsArea {
    private static final Logger LOGGER = Logger.getLogger(CommentsArea.class.getName());

    private JButton fileBug;

    private final MainFrame frame;

    private BugFilingStatus currentBugStatus;
    private CloudCommentsPaneSwing commentsPane;

    CommentsArea(MainFrame frame) {
        this.frame = frame;
    }

    JPanel createCommentsInputPanel() {
        JPanel mainPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();

        mainPanel.setLayout(layout);

        fileBug = new JButton(BugFilingStatus.FILE_BUG.toString());
        fileBug.setEnabled(false);
        fileBug.setToolTipText("Click to file bug for this issue");
        fileBug.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (frame.getCurrentSelectedBugLeaf() == null) {
                    return;
                }
                if (!canNavigateAway()) {
                    return;
                }
                BugInstance bug = frame.getCurrentSelectedBugLeaf().getBug();
                Cloud cloud1 = MainFrame.getInstance().getBugCollection().getCloud();
                if (!cloud1.supportsBugLinks()) {
                    return;
                }
                try {
                    URL u = cloud1.getBugLink(bug);
                    if (u != null) {
                        if (LaunchBrowser.showDocument(u)) {
                            cloud1.bugFiled(bug, null);
                            MainFrame.getInstance().syncBugInformation();
                        }
                    }
                } catch (Exception e1) {
                    LOGGER.log(Level.SEVERE, "Could not view/file bug", e1);
                    JOptionPane.showMessageDialog(MainFrame.getInstance(),
                            "Could not view/file bug:\n" + e1.getClass().getSimpleName()
                            + ": " + e1.getMessage());
                }
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        commentsPane = new CloudCommentsPaneSwing();
        mainPanel.add(new JScrollPane(commentsPane), c);

        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        mainPanel.add(fileBug, c);

        return mainPanel;
    }

    void updateCommentsFromLeafInformation(final BugLeafNode node) {
        if (node == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BugInstance bug = node.getBug();
                Cloud plugin = getCloud();

                if (plugin.supportsBugLinks()) {
                    currentBugStatus = plugin.getBugLinkStatus(bug);
                    fileBug.setText(currentBugStatus.toString());
                    fileBug.setToolTipText(currentBugStatus == BugFilingStatus.FILE_BUG ? "Click to file bug for this issue" : "");
                    fileBug.setEnabled(currentBugStatus.linkEnabled());
                    fileBug.setVisible(true);
                } else {
                    fileBug.setVisible(false);
                }
                commentsPane.setBugInstance( node.getBug());
            }
        });
    }

    private SortedBugCollection getBugCollection() {
        return (SortedBugCollection) MainFrame.getInstance().getBugCollection();
    }

    void updateCommentsFromNonLeafInformation(final BugAspects theAspects) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateCommentsFromNonLeafInformationFromSwingThread(theAspects);
            }
        });
    }


    public boolean canNavigateAway() {
        return commentsPane.canNavigateAway();
    }

    protected void updateCommentsFromNonLeafInformationFromSwingThread(BugAspects theAspects) {
        commentsPane.setBugAspects( theAspects);
        fileBug.setEnabled(false);
    }

    public boolean hasFocus() {
        return commentsPane.hasFocus();
    }

    private @CheckForNull Cloud getCloud() {
        MainFrame instance = MainFrame.getInstance();
        BugCollection bugCollection = instance.getBugCollection();
        if (bugCollection == null) {
            return null;
        }
        return bugCollection.getCloud();
    }

    public void updateBugCollection() {
        commentsPane.setBugCollection(getBugCollection());
    }

    public void refresh() {
        commentsPane.refresh();
    }


    public boolean canSetDesignations() {
        return commentsPane.canSetDesignations();
    }
    public void setDesignation(String designationKey) {
        commentsPane.setDesignation(designationKey);
    }

    public void updateCloud() {
        commentsPane.updateCloud();
    }
}
