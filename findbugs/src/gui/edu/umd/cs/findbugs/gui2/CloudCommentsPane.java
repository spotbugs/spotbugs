/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2010-2013 University of Maryland
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

import static edu.umd.cs.findbugs.util.Util.nullSafeEquals;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.util.Util;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SE_TRANSIENT_FIELD_NOT_RESTORED", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE"})
public abstract class CloudCommentsPane extends JPanel {

    private static final String MSG_REVIEW = L10N.getLocalString("dlg.cloud.add_review", "Click to add review...");
    private static final String MSG_REVIEW_MULTI = L10N.getLocalString("dlg.cloud.add_review_multi",
            "Click to add review to {0} bugs...");
    private static final String MSG_OVERWRITE_REVIEW = L10N.getLocalString("dlg.cloud.ovwrt_review_multi",
            "Click to overwrite {0} reviews...");

    private JTextArea cloudReportPane;
    protected JComponent cancelLink;
    protected JComponent signInOutLink;
    private JTextArea commentBox;
    private JButton submitCommentButton;
    private WideComboBox<String> designationCombo;
    private JPanel mainPanel;
    private JScrollPane _cloudReportScrollPane;
    protected JLabel titleLabel;
    protected JTextArea cloudDetailsLabel;
    private JPanel dumbPanelSignInOutLink;
    private JLabel lastSavedLabel;
    private JPanel cards;
    private JButton bulkReviewButton;
    private JLabel warningLabel;

    protected BugCollection _bugCollection;
    protected BugInstance _bugInstance;
    private BugAspects _bugAspects;

    private final Executor backgroundExecutor = Executors.newCachedThreadPool();

    private final Cloud.CloudStatusListener _cloudStatusListener = new MyCloudStatusListener();
    private Cloud lastCloud = null;
    private Font plainCommentFont;
    private String lastCommentText = null;
    private Set<BugInstance> lastBugsEdited = Collections.emptySet();
    private boolean clickedBulkReview = false;


    private void addNotInCloudCard() {
        final JPanel panel5 = new JPanel();
        cards.add(panel5, "NOT_IN_CLOUD");
    }

    public CloudCommentsPane() {
        $$$setupUI$$$();
        addNotInCloudCard();
        cloudReportPane.setBackground(this.getBackground());
        cloudReportPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        _cloudReportScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        //        designationCombo.setPreferredSize(new Dimension(300, 20));
        commentBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                commentBoxClicked();
            }
        });
        commentBox.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                updateSaveButton();
            }
        });
        commentBox.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        dumbPanelSignInOutLink.setPreferredSize(null);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        designationCombo.removeAllItems();
        final List<String> userDesignationKeys = I18N.instance().getUserDesignationKeys(true);
        for (final String designation : userDesignationKeys) {
            designationCombo.addItem(I18N.instance().getUserDesignation(designation));
        }
        designationCombo.addItem(null);
        designationCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component real = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    return real;
                }
                if (index == -1) {
                    return real;
                }
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(new EmptyBorder(3, 3, 3, 3));
                int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.anchor = GridBagConstraints.WEST;
                panel.add(real, gbc);

                gbc.weightx = 0;
                gbc.anchor = GridBagConstraints.EAST;
                gbc.insets = new Insets(0, 10, 0, 0);
                JLabel label = new JLabel(KeyEvent.getKeyModifiersText(mask) + "-" + (index + 1));
                label.setForeground(Color.GRAY);
                //                Font font = label.getFont();
                //                label.setFont(font.deriveFont(font.getSize() - 2f));
                panel.add(label, gbc);
                panel.setBackground(real.getBackground());
                return panel;
            }
        });
        designationCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!updatingHeader) {
                    int selectedIndex = designationCombo.getSelectedIndex();
                    if (selectedIndex >= 0) {
                        setDesignation(userDesignationKeys.get(selectedIndex));
                    }
                }
            }
        });

        //        commentEntryPanel.setVisible(false);
        submitCommentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                submitComment(CloudCommentsPane.this.getSelectedBugs());
            }
        });
        cloudDetailsLabel.setBackground(null);
        cloudDetailsLabel.setBorder(null);
        plainCommentFont = commentBox.getFont().deriveFont(Font.PLAIN);
        cloudReportPane.setFont(plainCommentFont);
        //        cloudReportPane.setEditorKit(new HTMLEditorKit());
        //        ((HTMLEditorKit) cloudReportPane.getDocument()).getStyleSheet().addRule("body { font-");

        setDefaultComment(MSG_REVIEW);
        commentBox.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                commentBox.setForeground(null);
                commentBox.setFont(plainCommentFont);
                if (isDefaultComment(commentBox.getText())) {
                    resetCommentBoxFont();
                    setCommentText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String text = commentBox.getText();
                if (isDefaultComment(text)) {
                    refresh();
                } else if (text.equals(lastCommentText)) {
                    if (text.trim().length() == 0) {
                        refresh();
                    }
                } else {
                    submitComment(CloudCommentsPane.this.getSelectedBugs());
                    resetCommentBoxFont();
                }
            }
        });
        commentBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelClicked();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    submitComment(CloudCommentsPane.this.getSelectedBugs());
                }
            }
        });
        submitCommentButton.setToolTipText("Submit review [Enter]");
        cancelLink.setToolTipText("Cancel [Esc]");

        bulkReviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickedBulkReview = true;
                refresh();
            }
        });

        setCanAddComments(false, false);
        setLastSaved(0);

        updateBugCommentsView();
    }

    private boolean isDefaultComment(String text) {
        if (text.equals(MSG_REVIEW)) {
            return true;
        }
        try {
            new MessageFormat(MSG_REVIEW_MULTI).parse(text);
            return true; // didn't throw an exception
        } catch (ParseException e) {
        }
        try {
            new MessageFormat(MSG_OVERWRITE_REVIEW).parse(text);
            return true; // didn't throw an exception
        } catch (ParseException e) {
        }
        return false;
    }

    private void updateSaveButton() {
        boolean changed = commentWasChanged();
        submitCommentButton.setEnabled(changed);
        submitCommentButton.setText(changed
                ? L10N.getLocalString("dlg.save_btn", "Save")
                        : L10N.getLocalString("dlg.saved_btn", "Saved"));
        cancelLink.setEnabled(false/*changed*/);
    }

    private void setCommentText(String t) {
        lastCommentText = t;
        if (!commentBox.getText().equals(t)) {
            commentBox.setText(t);
        }
    }

    private void resetCommentBoxFont() {
        commentBox.setFont(plainCommentFont);
        commentBox.setForeground(null);
    }

    private void setDefaultComment(String defaultComment) {
        setCommentText(defaultComment);
        commentBox.setForeground(Color.DARK_GRAY);
        commentBox.setFont(plainCommentFont.deriveFont(Font.ITALIC));
    }

    private void createUIComponents() {
        setupLinksOrButtons();
    }

    protected abstract void setupLinksOrButtons();


    private void applyToBugs(final BugAction bugAction) {
        Executor executor = backgroundExecutor;

        final AtomicInteger shownErrorMessages = new AtomicInteger(0);
        for (final BugInstance bug : getSelectedBugs()) {
            executor.execute(new Runnable() {
                @Override
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
                                "Error while submitting cloud reviews:\n"
                                        + e.getClass().getSimpleName() + ": " + e.getMessage(),
                                        "Review Submission Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
    }

    protected void signInOrOutClicked() {
        if (_bugCollection != null) {
            final Cloud cloud = _bugCollection.getCloud();
            if ("edu.umd.cs.findbugs.cloud.doNothingCloud".equals(cloud.getPlugin().getId())) {
                changeClicked();
            }
            SigninState state = cloud.getSigninState();
            if (state == SigninState.SIGNED_IN) {
                backgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        cloud.signOut();
                        refresh();
                    }
                });
                refresh();
            } else if (state.couldSignIn()) {
                backgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cloud.signIn();
                        } catch (Exception e) {
                            _bugCollection
                            .getProject()
                            .getGuiCallback()
                            .showMessageDialog(
                                    "The FindBugs Cloud could not be contacted at this time.\n\n"
                                            + Util.getNetworkErrorMessage(e));
                        }
                        refresh();
                    }
                });
                refresh();

            }
        }
    }

    protected void commentBoxClicked() {
        if (commentWasChanged()) {
            return;
        }
        setCanAddComments(false, true);
        CommentInfo commentInfo = new CommentInfo().invoke();
        boolean sameText = commentInfo.isSameText();
        String txt = commentInfo.getTxt();
        if (!sameText) {
            txt = "";
        }
        if (txt == null || txt.trim().length() == 0) {
            txt = "";
        }
        resetCommentBoxFont();
        boolean sameTextInBox = commentBox.getText().equals(txt);
        setCommentText(txt);
        int start = commentBox.getSelectionStart();
        int end = commentBox.getSelectionEnd();
        if (!commentBox.hasFocus() && (!sameTextInBox || start != 0 || end != txt.length())) {
            commentBox.setSelectionStart(0);
            commentBox.setSelectionEnd(txt.length());
        }
        updateSaveButton();
    }

    private boolean commentWasChanged() {
        String text = commentBox.getText();
        boolean b = !isDefaultComment(text);
        //        boolean b1 = text.trim().equals("");
        boolean b3 = text.equals(lastCommentText);
        return b && !b3;
    }

    public boolean canSetDesignations() {
        List<BugInstance> bugs = getSelectedBugs();
        if (bugs.isEmpty()) {
            return true;
        }
        Cloud plugin = _bugCollection != null ? _bugCollection.getCloud() : null;
        if (plugin == null) {
            return false;
        }
        for(BugInstance b : bugs) {
            if (plugin.canStoreUserAnnotation(b)) {
                return true;
            }
        }
        return false;
    }

    public void setDesignation(final String designationKey) {

        //        List<BugInstance> selectedBugs = getSelectedBugs();
        //        if (selectedBugs.size() > 1)
        //            if (!confirmAnnotation(selectedBugs))
        //                return;
        final AtomicBoolean stop = new AtomicBoolean(false);
        applyToBugs(new BugAction() {
            @Override
            public void execute(BugInstance bug) {
                if (stop.get()) {
                    return;
                }
                String oldValue = bug.getUserDesignationKey();
                String key = designationKey;
                if (key.equals(oldValue)) {
                    return;
                }
                Cloud plugin = _bugCollection != null ? _bugCollection.getCloud() : null;
                if (plugin != null && "I_WILL_FIX".equals(key) && plugin.supportsClaims()) {
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
                        if (result != JOptionPane.YES_OPTION) {
                            key = "MUST_FIX";
                        }
                    }
                }
                changeDesignationOfBugRightNow(bug, key);
                refresh();
            }
        });
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void submitComment(List<BugInstance> selectedBugs) {
        String comment = commentBox.getText();
        if (isDefaultComment(comment)) {
            comment = "";
        }
        //        if (selectedBugs.size() > 1)
        //            if (!confirmAnnotation(selectedBugs))
        //                return;
        if (designationCombo.getSelectedItem() != null) {
            final int index = designationCombo.getSelectedIndex();
            final String choice;
            if (index == -1) {
                choice = UserDesignation.UNCLASSIFIED.name();
            } else {
                choice = I18N.instance().getUserDesignationKeys(true).get(index);
            }
            setDesignation(choice);
        }
        final String finalComment = comment;
        applyToBugs(new BugAction() {
            @Override
            public void execute(BugInstance bug) {
                bug.setAnnotationText(finalComment, _bugCollection);
                refresh();
                setLastSaved(System.currentTimeMillis());
            }
        });

        refresh();

        setCanAddComments(true, false);
        commentBox.requestFocus();
    }

    private void setLastSaved(long date) {
        if (date > 0) {
            lastSavedLabel.setText("saved " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(date)));
        } else {
            lastSavedLabel.setText("");
        }
    }

    protected void cancelClicked() {
        setDefaultComment(lastCommentText);
        //        commentEntryPanel.setVisible(false);
        setCanAddComments(true, false);
    }

    private List<BugInstance> getSelectedBugs() {
        if (_bugInstance != null) {
            return Collections.singletonList(_bugInstance);
        }
        if (_bugAspects != null) {
            List<BugInstance> set = new ArrayList<BugInstance>();
            for (BugLeafNode node : _bugAspects.getMatchingBugs(BugSet.getMainBugSet())) {
                if (!BugSet.suppress(node)) {
                    set.add(node.getBug());
                }
            }
            return set;
        }
        return Collections.emptyList();
    }

    private boolean hasSelectedBugs() {
        return _bugInstance != null || _bugAspects != null;
    }

    protected void changeClicked() {
        final List<CloudPlugin> plugins = new ArrayList<CloudPlugin>();
        final List<String> descriptions = new ArrayList<String>();
        List<CloudPlugin> clouds = new ArrayList<CloudPlugin>(DetectorFactoryCollection.instance().getRegisteredClouds().values());
        Collections.sort(clouds, new Comparator<CloudPlugin>() {
            @Override
            public int compare(CloudPlugin o1, CloudPlugin o2) {
                return o1.getDescription().compareToIgnoreCase(o2.getDescription());
            }
        });
        for (final CloudPlugin plugin : clouds) {
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
            MainFrame.getInstance().setProjectChanged(true);
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    _bugCollection.reinitializeCloud();
                    Cloud cloud = _bugCollection.getCloud();
                    if (cloud != null) {
                        cloud.waitUntilIssueDataDownloaded();
                    }
                    updateCloudListeners(_bugCollection);
                    refresh();
                }
            });
            refresh();
        }
    }

    public void setBugCollection(BugCollection bugCollection) {
        updateCloudListeners(bugCollection);
        _bugCollection = bugCollection;
        _bugInstance = null;
        _bugAspects = null;
        refresh();
    }

    public void setBugInstance(final BugInstance bugInstance) {
        setBugs(bugInstance, null);
    }

    public void setBugAspects(BugAspects aspects) {
        setBugs(null, aspects);
    }

    private void setBugs(BugInstance bugInstance, BugAspects bugAspects) {
        if (_bugInstance == bugInstance && _bugAspects == bugAspects) {
            return;
        }
        if (!canNavigateAway()) {
            return;
        }

        _bugInstance = bugInstance;
        _bugAspects = bugAspects;
        refresh();
    }

    public boolean canNavigateAway() {
        if (commentWasChanged()) {
            submitComment(getSelectedBugs());
            return true;
        } else {
            return true;
        }
    }

    protected void changeDesignationOfBugRightNow(final BugInstance bug, final String designationKey) {
        String oldValue = bug.getUserDesignationKey();
        if (designationKey.equals(oldValue)) {
            return;
        }
        bug.setUserDesignationKey(designationKey, _bugCollection);
    }

    public void refresh() {
        updateBugCommentsView();
    }

    public void updateCloud() {
        updateCloudListeners(_bugCollection);
        refresh();
    }

    private void updateCloudListeners(BugCollection newBugCollection) {
        final Cloud newCloud = newBugCollection == null ? null : newBugCollection.getCloud();
        if (_bugCollection != null) {
            //noinspection ObjectEquality
            if (lastCloud != newCloud) {
                if (lastCloud != null) {
                    lastCloud.removeStatusListener(_cloudStatusListener);
                }
            }
        }
        if (lastCloud != newCloud && newCloud != null) {
            lastCloud = newCloud;
            newCloud.addStatusListener(_cloudStatusListener);
        }
    }


    private boolean inCloud(Collection<BugInstance> bugs) {
        final Cloud cloud = _bugCollection.getCloud();

        for (BugInstance b : bugs) {
            if (cloud.isInCloud(b)) {
                return true;
            }
        }
        return false;

    }

    private void updateBugCommentsView() {

        //TODO: fix cancel button
        List<BugInstance> bugs = getSelectedBugs();
        if (_bugCollection == null) {
            signInOutLink.setVisible(false);
            cloudDetailsLabel.setText("");
            cloudReportPane.setText("");
            titleLabel.setText("<html>Reviews");
            return;
        }
        updateHeader();
        final Cloud cloud = _bugCollection.getCloud();
        final CloudPlugin plugin = cloud.getPlugin();
        String details = plugin.getDetails();
        cloudDetailsLabel.setText(details);

        if (bugs.isEmpty()) {
            setCanAddComments(false, false);
            return;
        }

        String report;
        long lastSaved = -1;
        if (bugs.size() > 1) {
            int totalReviews = 0;
            int bugsWithReviews = 0;
            for (BugInstance bug : bugs) {
                long newTs = cloud.getUserTimestamp(bug);
                if (bug.hasSomeUserAnnotation() && newTs > 0 && (lastSaved == -1 || lastSaved < newTs)) {
                    lastSaved = newTs;
                }
                int reviewers = cloud.getNumberReviewers(bug);
                if (reviewers > 0) {
                    bugsWithReviews++;
                }
                totalReviews += reviewers;
            }
            report = bugs.size() + " bug" + (bugs.size() == 1 ? "" : "s") + " selected\n";
            report += bugsWithReviews + " reviewed bug" + (bugsWithReviews == 1 ? "" : "s")
                    + " / " + totalReviews + " total review" + (totalReviews == 1 ? "" : "s");
        } else {
            BugInstance bug = bugs.get(0);
            if (bug.hasSomeUserAnnotation()) {
                lastSaved = bug.getUserTimestamp();
            }
            report = cloud.getCloudReportWithoutMe(bug);
        }
        setLastSaved(lastSaved);
        cloudReportPane.setText(report);
        CommentInfo commentInfo = new CommentInfo().invoke();
        boolean sameText = commentInfo.isSameText();
        String txt = commentInfo.getTxt();
        CardLayout cl = (CardLayout) (cards.getLayout());
        HashSet<BugInstance> newBugSet = new HashSet<BugInstance>(bugs);
        boolean sameBugs = newBugSet.equals(lastBugsEdited);
        if (!sameBugs) {
            lastBugsEdited = newBugSet;
            clickedBulkReview = false;
        }
        if (!inCloud(bugs)) {
            cl.show(cards, "NOT_IN_CLOUD");
        } else if (bugs.size() > 1 && !clickedBulkReview) {
            warningLabel.setText("<HTML>" + bugs.size() + " bugs are selected.<BR>Click to review them all at once.");
            cl.show(cards, "WARNING");
        } else {
            cl.show(cards, "COMMENTS");
        }
        if (!sameText) {
            txt = MessageFormat.format(MSG_OVERWRITE_REVIEW, bugs.size());
            setDefaultComment(txt);
        } else {
            if (txt == null || txt.trim().length() == 0) {
                txt = bugs.size() > 1 ? MessageFormat.format(MSG_REVIEW_MULTI, bugs.size()) : MSG_REVIEW;
                setDefaultComment(txt);
            } else {
                resetCommentBoxFont();
                setCommentText(txt);
            }
        }

        setCanAddComments(cloud.canStoreUserAnnotation(bugs.get(0)), false);
        updateSaveButton();
    }

    private boolean updatingHeader = false;

    private void updateHeader() {
        final Cloud cloud = _bugCollection.getCloud();
        CloudPlugin plugin = cloud.getPlugin();
        if (hasSelectedBugs()) {
            CommentInfo commentInfo = new CommentInfo().invoke();
            boolean sameDesignation = commentInfo.isSameDesignation();
            String designation = commentInfo.getDesignation();
            if (!sameDesignation) {
                designation = null;
            }
            updatingHeader = true;
            designationCombo.setSelectedIndex(I18N.instance().getUserDesignationKeys(true).indexOf(designation));
            updatingHeader = false;
            setCanAddComments(true, true);
        } else {
            setCanAddComments(false, false);
        }

        final Cloud.SigninState state = cloud.getSigninState();
        final String stateStr = state == Cloud.SigninState.NO_SIGNIN_REQUIRED ? "" : "" + state;
        final String userStr = cloud.getUser() == null ? "" : cloud.getUser();
        if ("edu.umd.cs.findbugs.cloud.doNothingCloud".equals(plugin.getId())) {
            titleLabel.setText("<html><b>No cloud selected");
        } else {
            titleLabel.setText("<html><b>Reviews - " + cloud.getCloudName() + "</b>"
                    + "<br><font style='font-size: x-small;color:darkgray'>" + stateStr
                    + (userStr.length() > 0 ? " - " + userStr : ""));
        }
        switch (state) {
        case NO_SIGNIN_REQUIRED:
        case SIGNING_IN:
            signInOutLink.setVisible(false);
            break;
        case SIGNED_IN:
            setSignInOutText("sign out");
            signInOutLink.setVisible(true);
            break;
        default:
            if (state.couldSignIn()) {
                setSignInOutText("sign in");
                signInOutLink.setVisible(true);
            }
            break;
        }
        if ("edu.umd.cs.findbugs.cloud.doNothingCloud".equals(cloud.getPlugin().getId())) {
            setSignInOutText("enable cloud plugin...");
            signInOutLink.setVisible(true);
        }
    }

    private void setCanAddComments(boolean canClick, boolean canEnter) {
        submitCommentButton.setEnabled(canClick || canEnter);
        designationCombo.setEnabled(canClick || canEnter);
        commentBox.setEnabled(canClick || canEnter);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), null));
        _cloudReportScrollPane = new JScrollPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(_cloudReportScrollPane, gbc);
        cloudReportPane = new JTextArea();
        cloudReportPane.setEditable(false);
        cloudReportPane.setLineWrap(true);
        cloudReportPane.setText("<html>\r\n  <head>\r\n    \r\n  </head>\r\n  <body>\r\n  </body>\r\n</html>\r\n");
        cloudReportPane.setWrapStyleWord(true);
        _cloudReportScrollPane.setViewportView(cloudReportPane);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setBackground(new Color(-3355444));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-16751002)), null));
        titleLabel = new JLabel();
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 14));
        titleLabel.setForeground(new Color(-16777216));
        titleLabel.setText("FindBugs Cloud - signed in");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel1.add(titleLabel, gbc);
        dumbPanelSignInOutLink = new JPanel();
        dumbPanelSignInOutLink.setLayout(new GridBagLayout());
        dumbPanelSignInOutLink.setOpaque(false);
        dumbPanelSignInOutLink.setPreferredSize(new Dimension(50, 10));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(dumbPanelSignInOutLink, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        dumbPanelSignInOutLink.add(signInOutLink, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel2.setVisible(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(panel2, gbc);
        cloudDetailsLabel = new JTextArea();
        cloudDetailsLabel.setEditable(false);
        cloudDetailsLabel.setFont(new Font(cloudDetailsLabel.getFont().getName(), Font.ITALIC, 10));
        cloudDetailsLabel.setForeground(new Color(-10066330));
        cloudDetailsLabel.setLineWrap(true);
        cloudDetailsLabel.setMaximumSize(new Dimension(100, 50));
        cloudDetailsLabel.setMinimumSize(new Dimension(50, 16));
        cloudDetailsLabel.setOpaque(false);
        cloudDetailsLabel.setPreferredSize(new Dimension(100, 31));
        cloudDetailsLabel.setText("Comments are stored on the FindBugs Cloud at http://findbugs-cloud.appspot.com");
        cloudDetailsLabel.setWrapStyleWord(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(cloudDetailsLabel, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 5;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(panel3, gbc);
        cards = new JPanel();
        cards.setLayout(new CardLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(cards, gbc);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        cards.add(panel4, "COMMENTS");
        designationCombo = new WideComboBox<>();
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 0, 0, 0);
        panel4.add(designationCombo, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.gridheight = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(scrollPane1, gbc);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        commentBox = new JTextArea();
        commentBox.setLineWrap(true);
        commentBox.setRows(5);
        commentBox.setText(" ");
        commentBox.setWrapStyleWord(true);
        scrollPane1.setViewportView(commentBox);
        submitCommentButton = new JButton();
        submitCommentButton.setText("Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel4.add(submitCommentButton, gbc);
        lastSavedLabel = new JLabel();
        lastSavedLabel.setFont(new Font(lastSavedLabel.getFont().getName(), Font.ITALIC, 9));
        lastSavedLabel.setText("saved at");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel4.add(lastSavedLabel, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        panel4.add(cancelLink, gbc);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        cards.add(panel5, "WARNING");
        warningLabel = new JLabel();
        warningLabel.setHorizontalAlignment(0);
        warningLabel.setHorizontalTextPosition(0);
        warningLabel.setText("<HTML>Multiple bugs are selected.<BR>Click to review them all at once.");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel5.add(warningLabel, gbc);
        bulkReviewButton = new JButton();
        bulkReviewButton.setText("Bulk Review");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel5.add(bulkReviewButton, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private class MyCloudStatusListener implements Cloud.CloudStatusListener {
        @Override
        public void handleIssueDataDownloadedEvent() {
            refresh();
        }


        @Override
        public void handleStateChange(final Cloud.SigninState oldState, final Cloud.SigninState state) {
            updateHeader();
            refresh();
        }


    }

    private interface BugAction {
        void execute(BugInstance bug);
    }

    private class CommentInfo {
        private String txt;
        private boolean sameText;
        private String designation;
        private boolean sameDesignation;

        public String getTxt() {
            return txt;
        }

        public boolean isSameText() {
            return sameText;
        }

        public String getDesignation() {
            return designation;
        }

        public boolean isSameDesignation() {
            return sameDesignation;
        }

        public CommentInfo invoke() {
            txt = null;
            sameText = true;
            designation = null;
            sameDesignation = true;
            for (BugInstance bug : getSelectedBugs()) {
                String newText = bug.getAnnotationText();
                if (txt == null) {
                    txt = newText;
                } else {
                    if (!nullSafeEquals(txt, newText)) {
                        sameText = false;
                    }
                }

                String newDesignation = bug.getUserDesignationKey();
                if (designation == null) {
                    designation = newDesignation;
                } else {
                    if (!nullSafeEquals(designation, newDesignation)) {
                        sameDesignation = false;
                    }
                }
            }
            return this;
        }
    }
}
