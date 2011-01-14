/*
 * Copyright 2010 Keith Lea
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.CloudPlugin;

import static edu.umd.cs.findbugs.util.Util.nullSafeEquals;

@edu.umd.cs.findbugs.annotations.SuppressWarnings({"SE_TRANSIENT_FIELD_NOT_RESTORED", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE"})
public abstract class CloudCommentsPane extends JPanel {

    private static final String DEFAULT_COMMENT = "Type your comment here";
    private static final String DEFAULT_VARIOUS_COMMENTS_COMMENT = "<bugs have various comments>";

    private JEditorPane _cloudReportPane;
    protected JComponent _addCommentLink;
    protected JComponent _cancelLink;
    protected JComponent _signInOutLink;
    protected JComponent _changeLink;
    private JTextArea _commentBox;
    private JButton _submitCommentButton;
    private JPanel _commentEntryPanel;
    private JComboBox _classificationCombo;
    private JPanel _mainPanel;
    private JScrollPane _cloudReportScrollPane;
    protected JLabel _titleLabel;
    protected JTextArea _cloudDetailsLabel;

    protected SortedBugCollection _bugCollection;
    protected BugInstance _bugInstance;
    private BugAspects _bugAspects;

    private boolean dontShowAnnotationConfirmation = false;
    private boolean addingComment = false;

    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    private final Cloud.CloudStatusListener _cloudStatusListener = new MyCloudStatusListener();

    public CloudCommentsPane() {
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(_mainPanel, BorderLayout.CENTER);

        _classificationCombo.removeAllItems();
        for (final String designation : I18N.instance().getUserDesignationKeys(true)) {
            _classificationCombo.addItem(I18N.instance().getUserDesignation(designation));
        }

        _commentEntryPanel.setVisible(false);
        _submitCommentButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                submitClicked();
            }
        });
        _cloudDetailsLabel.setBackground(null);
        _cloudDetailsLabel.setBorder(null);

        _commentBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelClicked();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    submitClicked();
                }
            }
        });
        _submitCommentButton.setToolTipText("Submit comment [Shift+Enter]");
        _cancelLink.setToolTipText("Cancel [Esc]");
        _addCommentLink.setEnabled(false);

        updateBugCommentsView();
    }

    private void createUIComponents() {
        setupLinksOrButtons();
    }

    protected abstract void setupLinksOrButtons();


    private void applyToBugs(boolean background, final BugAction bugAction) {
        Executor executor = background ? backgroundExecutor : new NowExecutor();

        final AtomicInteger shownErrorMessages = new AtomicInteger(0);
        for (final BugInstance bug : getSelectedBugs())
            executor.execute(new Runnable() {
                public void run() {
                    if (shownErrorMessages.get() > 5) {
                        // 5 errors? let's just stop trying.
                        return;
                    }
                    try {
                        bugAction.execute(bug);
                    } catch (Throwable e) {
                        if (shownErrorMessages.addAndGet(1) > 5) {
                            return;
                        }
                        JOptionPane.showMessageDialog(CloudCommentsPane.this,
                                "Error while submitting cloud comments:\n"
                                + e.getClass().getSimpleName() + ": " + e.getMessage(),
                                "Comment Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
    }

    protected void signInOrOutClicked() {
        if (_bugCollection != null) {
            final Cloud cloud = _bugCollection.getCloud();
            switch (cloud.getSigninState()) {
                case SIGNED_OUT:
                case SIGNIN_FAILED:
                case UNAUTHENTICATED:
                    try {
                        cloud.signIn();
                    } catch (Exception e) {
                        _bugCollection.getProject().getGuiCallback().showMessageDialog(
                                "The FindBugs Cloud could not be contacted at this time.\n\n" + e.getMessage());
                    }
                    break;
                case SIGNED_IN:
                    cloud.signOut();
                    break;
                default:
            }
        }
    }

    protected void addCommentClicked() {
        _commentEntryPanel.setVisible(true);
        _addCommentLink.setVisible(false);
        String txt = null;
        boolean sameText = true;
        String designation = null;
        boolean sameDesignation = true;
        for (BugInstance bug : getSelectedBugs()) {
            String newText = bug.getAnnotationText();
            if (txt == null)
                txt = newText;
            else {
                if (!nullSafeEquals(txt, newText))
                    sameText = false;
            }

            String newDesignation = bug.getUserDesignationKey();
            if (designation == null)
                designation = newDesignation;
            else {
                if (!nullSafeEquals(designation, newDesignation))
                    sameDesignation = false;
            }
        }
        if (!sameText)
            txt = DEFAULT_VARIOUS_COMMENTS_COMMENT;
        if (!sameDesignation)
            designation = UserDesignation.UNCLASSIFIED.name();
        _classificationCombo.setSelectedIndex(I18N.instance().getUserDesignationKeys(true).indexOf(designation));
        if (txt == null || txt.trim().length() == 0)
            txt = DEFAULT_COMMENT;
        _commentBox.setText(txt);
        _commentBox.requestFocusInWindow();
        _commentBox.setSelectionStart(0);
        _commentBox.setSelectionEnd(_commentBox.getText().length());
        addingComment = true;
        invalidate();
    }

    private void submitClicked() {
        String comment = _commentBox.getText();
        if (comment.equals(DEFAULT_COMMENT) || comment.equals(DEFAULT_VARIOUS_COMMENTS_COMMENT))
            comment = "";
        final int index = _classificationCombo.getSelectedIndex();
        final String choice;
        if (index == -1) {
            choice = UserDesignation.UNCLASSIFIED.name();
        } else {
            choice = I18N.instance().getUserDesignationKeys(true).get(index);
        }
        if (getSelectedBugs().size() > 1)
            if (!confirmAnnotation())
                return;
        setDesignation(choice);
        final String finalComment = comment;
        applyToBugs(true, new BugAction() {
            public void execute(BugInstance bug) {
                bug.setAnnotationText(finalComment, _bugCollection);
                refresh();
            }
        });

        addingComment = false;
        refresh();

        _commentEntryPanel.setVisible(false);
        _addCommentLink.setVisible(true);
        _commentBox.requestFocus();
        invalidate();
    }

    protected void cancelClicked() {
        addingComment = false;
        _commentEntryPanel.setVisible(false);
        _addCommentLink.setVisible(true);
        invalidate();
    }

    private List<BugInstance> getSelectedBugs() {
        if (_bugInstance != null)
            return Collections.singletonList(_bugInstance);
        if (_bugAspects != null) {
            List<BugInstance> set = new ArrayList<BugInstance>();
            for (BugLeafNode node : _bugAspects.getMatchingBugs(BugSet.getMainBugSet())) {
                set.add(node.getBug());
            }
            return set;
        }
        return Collections.emptyList();
    }

    protected void changeClicked() {
        final List<CloudPlugin> plugins = new ArrayList<CloudPlugin>();
        final List<String> descriptions = new ArrayList<String>();
        for (final CloudPlugin plugin : DetectorFactoryCollection.instance().getRegisteredClouds().values()) {
            final boolean disabled = isDisabled(plugin);
            if (!disabled && !plugin.isHidden()) {
                descriptions.add(plugin.getDescription());
                plugins.add(plugin);
            }
        }
        showCloudChooser(plugins, descriptions);
    }

    protected abstract boolean isDisabled(CloudPlugin plugin);

    protected abstract void showCloudChooser(List<CloudPlugin> plugins, List<String> descriptions);

    protected void changeCloud(String newCloudId) {
        final String oldCloudId = _bugCollection.getCloud().getPlugin().getId();
        if (!oldCloudId.equals(newCloudId)) {
            _bugCollection.getProject().setCloudId(newCloudId);
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    _bugCollection.reinitializeCloud();
                    refresh();
                }
            });
            refresh();
        }
    }

    public void setBugInstance(final SortedBugCollection bugCollection, final BugInstance bugInstance) {
        setBugs(bugCollection, bugInstance, null);
    }

    public void setBugAspects(SortedBugCollection bugCollection, BugAspects aspects) {
        setBugs(bugCollection, null, aspects);
    }

    private void setBugs(SortedBugCollection bugCollection, BugInstance bugInstance, BugAspects bugAspects) {
        if (_bugCollection == bugCollection && _bugInstance == bugInstance && _bugAspects == bugAspects)
            return;
        if (!canNavigateAway())
            return;

        updateCloudListeners(bugCollection);
        _bugCollection = bugCollection;
        _bugInstance = bugInstance;
        _bugAspects = bugAspects;
        refresh();
    }

    public void setDesignation(final String designationKey) {
        final AtomicBoolean stop = new AtomicBoolean(false);
        applyToBugs(false, new BugAction() {
            public void execute(BugInstance bug) {
                if (stop.get())
                    return;
                String oldValue = bug.getUserDesignationKey();
                String key = designationKey;
                if (key.equals(oldValue))
                    return;
                Cloud plugin = _bugCollection != null ? _bugCollection.getCloud() : null;
                if (plugin != null && key.equals("I_WILL_FIX") && plugin.supportsClaims()) {
                    String claimedBy = plugin.claimedBy(bug);
                    if (claimedBy != null && !plugin.getUser().equals(claimedBy)) {
                        int result = JOptionPane.showConfirmDialog(null,
                                bug.getMessage() + "\n"
                                        + claimedBy + " has already said they will fix this issue\n"
                                        + "Do you want to also be listed as fixing this issue?\n"
                                        + "If so, please coordinate with " + claimedBy,
                                "Issue already claimed", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (result == JOptionPane.CANCEL_OPTION) {
                            stop.set(true);
                            return;
                        }
                        if (result != JOptionPane.YES_OPTION)
                            key = "MUST_FIX";
                    }
                }
                changeDesignationOfBug(bug, key);
                refresh();
            }
        });
    }

    public boolean canNavigateAway() {
        if (addingComment) {
            int result = JOptionPane.showOptionDialog(this, "You have unsaved comments.", "Unsaved Comments",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[]{"Continue Editing", "Discard"},
                    "Continue Editing");
            boolean discard = result == 1;
            if (discard)
                cancelClicked();
            else
                _commentBox.requestFocus();
            return discard; // return true if user clicked "Discard"
        }
        return true;
    }

    private boolean confirmAnnotation() {

        String[] options = {L10N.getLocalString("dlg.yes_btn", "Yes"),
                L10N.getLocalString("dlg.no_btn", "No"),
                L10N.getLocalString("dlg.yes_dont_ask_btn", "Yes, and don't ask again")};
        if (dontShowAnnotationConfirmation)
            return true;
        int choice = JOptionPane
                .showOptionDialog(
                        this,
                        L10N
                                .getLocalString("dlg.changing_text_lbl",
                                "This will overwrite the comments associated\n" +
                                        "with all bugs in this folder and subfolders. \nAre you sure?"),
                        L10N.getLocalString("dlg.annotation_change_ttl", "Are you sure?"),
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        switch (choice) {
            case 0:
                return true;
            case 1:
                return false;
            case 2:
                dontShowAnnotationConfirmation = true;
                return true;
            default:
                return true;
        }

    }

    protected boolean changeDesignationOfBug(final BugInstance bug, final String designationKey) {
        String oldValue = bug.getUserDesignationKey();
        if (designationKey.equals(oldValue))
            return false;
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                bug.setUserDesignationKey(designationKey, _bugCollection);
                refresh();
            }
        });
        return true;
    }

    public void refresh() {
        updateBugCommentsView();
    }

    private void updateCloudListeners(final SortedBugCollection newBugCollection) {
        boolean isNewCloud = false;
        final Cloud newCloud = newBugCollection == null ? null : newBugCollection.getCloud();
        if (_bugCollection != null) {
            final Cloud oldCloud = _bugCollection.getCloud();
            //noinspection ObjectEquality
            if (oldCloud != newCloud) {
                isNewCloud = true;
                if (oldCloud != null) {
                    oldCloud.removeStatusListener(_cloudStatusListener);
                }
            }
        } else {
            isNewCloud = true;
        }
        if (isNewCloud && newCloud != null) {
            newCloud.addStatusListener(_cloudStatusListener);
        }
    }


    private void updateBugCommentsView() {
        List<BugInstance> bugs = getSelectedBugs();
        if (_bugCollection == null || bugs.isEmpty()) {
            _signInOutLink.setVisible(false);
            _changeLink.setVisible(false);
            _cloudDetailsLabel.setText("");
            _cloudReportPane.setText("");
            _titleLabel.setText("<html>Comments");
            return;
        }

        _addCommentLink.setEnabled(_bugCollection != null && !bugs.isEmpty());
        _changeLink.setVisible(true);
        final Cloud cloud = _bugCollection.getCloud();
        String report;
        if (bugs.size() > 1)
            report = bugs.size() + " bugs selected";
        else
            report = cloud.getCloudReport(bugs.get(0));
        setCloudReportText(report);
        final CloudPlugin plugin = cloud.getPlugin();
        _cloudDetailsLabel.setText(plugin.getDetails());
        final Cloud.SigninState state = cloud.getSigninState();
        final String stateStr = state == Cloud.SigninState.NO_SIGNIN_REQUIRED ? "" : "" + state;
        final String userStr = cloud.getUser() == null ? "" : cloud.getUser();
        _titleLabel.setText("<html><b>Comments - " + cloud.getCloudName() + "</b>"
                + "<br><font style='font-size: x-small;color:darkgray'>" + stateStr
                + (userStr.length() > 0 ? " - " + userStr : ""));
        _addCommentLink.setVisible(cloud.canStoreUserAnnotation(bugs.get(0)));
        switch (state) {
            case NO_SIGNIN_REQUIRED:
            case SIGNING_IN:
                _signInOutLink.setVisible(false);
                break;
            case SIGNED_OUT:
            case SIGNIN_FAILED:
            case UNAUTHENTICATED:
                setSignInOutText("sign in");
                _signInOutLink.setVisible(true);
                break;
            case SIGNED_IN:
                setSignInOutText("sign out");
                _signInOutLink.setVisible(true);
                break;
            default:
        }
    }

    private void setCloudReportText(final String report) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final HTMLDocument doc = (HTMLDocument) _cloudReportPane.getDocument();
                try {
                    doc.remove(0, doc.getLength());
                    doc.insertString(0, report, null);
                } catch (BadLocationException e) {
                    // probably won't happen
                }
            }
        });
    }

    protected abstract void setSignInOutText(String buttonText);

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        _mainPanel = new JPanel();
        _mainPanel.setLayout(new GridBagLayout());
        _mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), null));
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        _mainPanel.add(spacer1, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.ipadx = 5;
        gbc.ipady = 5;
        _mainPanel.add(_addCommentLink, gbc);
        _commentEntryPanel = new JPanel();
        _commentEntryPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        _mainPanel.add(_commentEntryPanel, gbc);
        _commentEntryPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null));
        _submitCommentButton = new JButton();
        _submitCommentButton.setText("Submit");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        _commentEntryPanel.add(_submitCommentButton, gbc);
        _classificationCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Unclassified");
        defaultComboBoxModel1.addElement("Not a bug");
        defaultComboBoxModel1.addElement("Etc");
        _classificationCombo.setModel(defaultComboBoxModel1);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        _commentEntryPanel.add(_classificationCombo, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Classification:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        _commentEntryPanel.add(label1, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        _commentEntryPanel.add(scrollPane1, gbc);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        _commentBox = new JTextArea();
        _commentBox.setFont(UIManager.getFont("Label.font"));
        _commentBox.setRows(5);
        _commentBox.setText("Enter comment here");
        scrollPane1.setViewportView(_commentBox);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        _commentEntryPanel.add(_cancelLink, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        _commentEntryPanel.add(spacer2, gbc);
        _cloudReportScrollPane = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        _mainPanel.add(_cloudReportScrollPane, gbc);
        _cloudReportScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null));
        _cloudReportPane = new JEditorPane();
        _cloudReportPane.setContentType("text/html");
        _cloudReportPane.setEditable(false);
        _cloudReportPane.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \r\n    </p>\r\n  </body>\r\n</html>\r\n");
        _cloudReportScrollPane.setViewportView(_cloudReportPane);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setBackground(new Color(-3355444));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        _mainPanel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16751002)), null));
        _titleLabel = new JLabel();
        _titleLabel.setFont(new Font(_titleLabel.getFont().getName(), Font.BOLD, 14));
        _titleLabel.setForeground(new Color(-16777216));
        _titleLabel.setText("FindBugs Cloud - signed in");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel1.add(_titleLabel, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel1.add(_signInOutLink, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        _mainPanel.add(panel2, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel2.add(_changeLink, gbc);
        _cloudDetailsLabel = new JTextArea();
        _cloudDetailsLabel.setEditable(false);
        _cloudDetailsLabel.setFont(new Font(_cloudDetailsLabel.getFont().getName(), Font.ITALIC, 10));
        _cloudDetailsLabel.setForeground(new Color(-10066330));
        _cloudDetailsLabel.setLineWrap(true);
        _cloudDetailsLabel.setMaximumSize(new Dimension(100, 50));
        _cloudDetailsLabel.setMinimumSize(new Dimension(50, 16));
        _cloudDetailsLabel.setOpaque(false);
        _cloudDetailsLabel.setPreferredSize(new Dimension(100, 31));
        _cloudDetailsLabel.setText("Comments are stored on the FindBugs Cloud at http://findbugs-cloud.appspot.com");
        _cloudDetailsLabel.setWrapStyleWord(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(_cloudDetailsLabel, gbc);
        label1.setLabelFor(_classificationCombo);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return _mainPanel;
    }

    private class MyCloudStatusListener implements Cloud.CloudStatusListener {
        public void handleIssueDataDownloadedEvent() {
        }


        public void handleStateChange(final Cloud.SigninState oldState, final Cloud.SigninState state) {
            refresh();
        }


    }

    private interface BugAction {
        void execute(BugInstance bug);
    }

    private static class NowExecutor implements Executor {
        public void execute(Runnable command) {
            command.run();
        }
    }
}
