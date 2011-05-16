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

import static edu.umd.cs.findbugs.util.Util.nullSafeEquals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.L10N;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.CloudPlugin;

@edu.umd.cs.findbugs.annotations.SuppressWarnings({"SE_TRANSIENT_FIELD_NOT_RESTORED", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE"})
public abstract class CloudCommentsPane extends JPanel {

    private static final String DEFAULT_COMMENT = "Click to add comment...";
    private static final String DEFAULT_VARIOUS_COMMENTS_COMMENT = "<bugs have various comments>";

    private JTextArea cloudReportPane;
    protected JComponent cancelLink;
    protected JComponent signInOutLink;
    private JTextArea commentBox;
    private JButton submitCommentButton;
    private JPanel commentEntryPanel;
    private WideComboBox designationCombo;
    private JPanel mainPanel;
    private JScrollPane _cloudReportScrollPane;
    protected JLabel titleLabel;
    protected JTextArea cloudDetailsLabel;
    private JPanel dumbPanelSignInOutLink;
    protected JTextArea clickToAddCommentTextArea;
    private JPanel clickToAddCommentPanel;

    protected BugCollection _bugCollection;
    protected BugInstance _bugInstance;
    private BugAspects _bugAspects;

    private boolean dontShowAnnotationConfirmation = false;
    private boolean addingComment = false;

    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    private final Cloud.CloudStatusListener _cloudStatusListener = new MyCloudStatusListener();
    private Cloud lastCloud = null;

    public CloudCommentsPane() {
        $$$setupUI$$$();

        cloudReportPane.setBackground(this.getBackground());
        cloudReportPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        _cloudReportScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

//        designationCombo.setPreferredSize(new Dimension(300, 20));
        clickToAddCommentTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                addCommentClicked();
            }
        });
        clickToAddCommentPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        dumbPanelSignInOutLink.setPreferredSize(null);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        designationCombo.removeAllItems();
        int i = 1;
        final List<String> userDesignationKeys = I18N.instance().getUserDesignationKeys(true);
        for (final String designation : userDesignationKeys) {
            designationCombo.addItem(I18N.instance().getUserDesignation(designation));
        }
        designationCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component real = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index == -1)
                    return real;
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
                Font font = label.getFont();
                label.setFont(font.deriveFont(font.getSize() - 2f));
                panel.add(label, gbc);
                panel.setBackground(real.getBackground());
                return panel;
            }
        });
        designationCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!updatingHeader)
                  setDesignation(userDesignationKeys.get(designationCombo.getSelectedIndex()));
            }
        });

        commentEntryPanel.setVisible(false);
        submitCommentButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                submitClicked();
            }
        });
        cloudDetailsLabel.setBackground(null);
        cloudDetailsLabel.setBorder(null);
        final Font font1 = cloudDetailsLabel.getFont().deriveFont(Font.PLAIN);
        cloudReportPane.setFont(font1);
//        cloudReportPane.setEditorKit(new HTMLEditorKit());
//        ((HTMLEditorKit) cloudReportPane.getDocument()).getStyleSheet().addRule("body { font-");

        commentBox.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                commentBox.setForeground(null);
                commentBox.setFont(font1);
                if (commentBox.getText().equals(DEFAULT_COMMENT)) {
                    commentBox.setText("");
                }
            }

            public void focusLost(FocusEvent e) {
                if (commentBox.getText().equals(DEFAULT_COMMENT) || commentBox.getText().trim().equals("")) {
                    commentBox.setText(DEFAULT_COMMENT);
                    commentBox.setForeground(Color.DARK_GRAY);
                    commentBox.setFont(font1.deriveFont(Font.ITALIC));
                } else {
                    commentBox.setFont(font1);
                    commentBox.setForeground(null);
                }
            }
        });
        commentBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelClicked();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    submitClicked();
                }
            }
        });
        submitCommentButton.setToolTipText("Submit comment [Shift+Enter]");
        cancelLink.setToolTipText("Cancel [Esc]");
        setCanAddComments(false, false);

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
            if (cloud.getPlugin().getId().equals("edu.umd.cs.findbugs.cloud.doNothingCloud")) {
                changeClicked();
            }
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
        setCanAddComments(false, true);
        CommentInfo commentInfo = new CommentInfo().invoke();
        boolean sameText = commentInfo.isSameText();
        String txt = commentInfo.getTxt();
        if (!sameText)
            txt = DEFAULT_VARIOUS_COMMENTS_COMMENT;
        if (txt == null || txt.trim().length() == 0)
            txt = DEFAULT_COMMENT;
        commentBox.setText(txt);
        commentBox.requestFocusInWindow();
        addingComment = true;
        invalidate();
        validate();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                Rectangle rect = new Rectangle(
                        commentEntryPanel.getX() + mainPanel.getX() + getX(),
                        commentEntryPanel.getY() + mainPanel.getY() + getY(),
                        commentEntryPanel.getWidth(),
                        commentEntryPanel.getHeight());
                scrollRectToVisible(rect);
            }
        });
    }

    private void submitClicked() {
        String comment = commentBox.getText();
        if (comment.equals(DEFAULT_COMMENT) || comment.equals(DEFAULT_VARIOUS_COMMENTS_COMMENT))
            comment = "";
        final int index = designationCombo.getSelectedIndex();
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

        commentEntryPanel.setVisible(false);
        setCanAddComments(true, false);
        commentBox.requestFocus();
        invalidate();
    }

    protected void cancelClicked() {
        addingComment = false;
        commentEntryPanel.setVisible(false);
        setCanAddComments(true, false);
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
    private boolean hasSelectedBugs() {
        if (_bugInstance != null)
            return true;
        if (_bugAspects != null) {
            return true;
        }
        return false;
    }

    protected void changeClicked() {
        final List<CloudPlugin> plugins = new ArrayList<CloudPlugin>();
        final List<String> descriptions = new ArrayList<String>();
        List<CloudPlugin> clouds = new ArrayList<CloudPlugin>(DetectorFactoryCollection.instance().getRegisteredClouds().values());
        Collections.sort(clouds, new Comparator<CloudPlugin>() {
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
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    _bugCollection.reinitializeCloud();
                    Cloud cloud = _bugCollection.getCloud();
                    if (cloud != null)
                        cloud.waitUntilIssueDataDownloaded();
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
        _bugInstance = null;;
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
        if (_bugInstance == bugInstance && _bugAspects == bugAspects)
            return;
        if (!canNavigateAway())
            return;

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
                commentBox.requestFocus();
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

    private void updateCloudListeners(BugCollection newBugCollection) {
        boolean isNewCloud = false;
        final Cloud newCloud = newBugCollection == null ? null : newBugCollection.getCloud();
        if (_bugCollection != null) {
            //noinspection ObjectEquality
            if (lastCloud != newCloud) {
                isNewCloud = true;
                if (lastCloud != null) {
                    lastCloud.removeStatusListener(_cloudStatusListener);
                }
            }
        } else {
            isNewCloud = true;
        }
        if (lastCloud != newCloud && newCloud != null) {
            lastCloud = newCloud;
            newCloud.addStatusListener(_cloudStatusListener);
        }
    }


    private void updateBugCommentsView() {
        
        List<BugInstance> bugs = getSelectedBugs();
        if (_bugCollection == null) {
            signInOutLink.setVisible(false);
            cloudDetailsLabel.setText("");
            cloudReportPane.setText("");
            titleLabel.setText("<html>Comments");
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
        if (bugs.size() > 1) {
            int count = 0;
            for (BugInstance bug : bugs) {
                count += cloud.getNumberReviewers(bug);
            }
            report = bugs.size() + " bugs selected\n";
            report += count + " comment" + (count == 1 ? "" : "s");
        } else {
            report = cloud.getCloudReport(bugs.get(0));
        }
        setCloudReportText(report);
        setCanAddComments(cloud.canStoreUserAnnotation(bugs.get(0)), false);
    }

    private boolean updatingHeader = false;
    private void updateHeader() {
        final Cloud cloud = _bugCollection.getCloud();
        CloudPlugin plugin = cloud.getPlugin();
        if (hasSelectedBugs()) {
            CommentInfo commentInfo = new CommentInfo().invoke();
            boolean sameDesignation = commentInfo.isSameDesignation();
            String designation = commentInfo.getDesignation();
            if (!sameDesignation)
                designation = UserDesignation.UNCLASSIFIED.name();
            updatingHeader = true;
            designationCombo.setSelectedIndex(I18N.instance().getUserDesignationKeys(true).indexOf(designation));
            updatingHeader = false;
            designationCombo.setEditable(true);
        } else {
            designationCombo.setEditable(false);
        }

        final Cloud.SigninState state = cloud.getSigninState();
        final String stateStr = state == Cloud.SigninState.NO_SIGNIN_REQUIRED ? "" : "" + state;
        final String userStr = cloud.getUser() == null ? "" : cloud.getUser();
        if (plugin.getId().equals("edu.umd.cs.findbugs.cloud.doNothingCloud"))
            titleLabel.setText("<html><b>Comments disabled");
        else
            titleLabel.setText("<html><b>Comments - " + cloud.getCloudName() + "</b>"
                    + "<br><font style='font-size: x-small;color:darkgray'>" + stateStr
                    + (userStr.length() > 0 ? " - " + userStr : ""));
        switch (state) {
            case NO_SIGNIN_REQUIRED:
            case SIGNING_IN:
                signInOutLink.setVisible(false);
                break;
            case SIGNED_OUT:
            case SIGNIN_FAILED:
            case UNAUTHENTICATED:
                setSignInOutText("sign in");
                signInOutLink.setVisible(true);
                break;
            case SIGNED_IN:
                setSignInOutText("sign out");
                signInOutLink.setVisible(true);
                break;
            default:
        }
        if (cloud.getPlugin().getId().equals("edu.umd.cs.findbugs.cloud.doNothingCloud")) {
            setSignInOutText("enable cloud plugin...");
            signInOutLink.setVisible(true);
        }
    }

    private void setCanAddComments(boolean canClick, boolean canEnter) {
        designationCombo.setVisible(canClick || canEnter);
        clickToAddCommentPanel.setVisible(canClick);
        commentEntryPanel.setVisible(canEnter);
        invalidate();
    }

    private void setCloudReportText(final String report) {
        cloudReportPane.setText(report);
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
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(spacer1, gbc);
        commentEntryPanel = new JPanel();
        commentEntryPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(commentEntryPanel, gbc);
        commentEntryPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null));
        submitCommentButton = new JButton();
        submitCommentButton.setText("Submit");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        commentEntryPanel.add(submitCommentButton, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        commentEntryPanel.add(scrollPane1, gbc);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        commentBox = new JTextArea();
        commentBox.setRows(5);
        commentBox.setText(" ");
        scrollPane1.setViewportView(commentBox);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        commentEntryPanel.add(cancelLink, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        commentEntryPanel.add(spacer2, gbc);
        _cloudReportScrollPane = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(_cloudReportScrollPane, gbc);
        cloudReportPane = new JTextArea();
        cloudReportPane.setEditable(false);
        cloudReportPane.setText("<html>\r\n  <head>\r\n    \r\n  </head>\r\n  <body>\r\n  </body>\r\n</html>\r\n");
        _cloudReportScrollPane.setViewportView(cloudReportPane);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setBackground(new Color(-3355444));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
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
        clickToAddCommentPanel = new JPanel();
        clickToAddCommentPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(clickToAddCommentPanel, gbc);
        clickToAddCommentTextArea = new JTextArea();
        clickToAddCommentTextArea.setBackground(new Color(-1));
        clickToAddCommentTextArea.setFont(new Font(clickToAddCommentTextArea.getFont().getName(), Font.ITALIC, clickToAddCommentTextArea.getFont().getSize()));
        clickToAddCommentTextArea.setForeground(new Color(-10066330));
        clickToAddCommentTextArea.setMinimumSize(new Dimension(6, 40));
        clickToAddCommentTextArea.setPreferredSize(new Dimension(128, 40));
        clickToAddCommentTextArea.setText("Click to add comment...");
        clickToAddCommentTextArea.setWrapStyleWord(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        clickToAddCommentPanel.add(clickToAddCommentTextArea, gbc);
        designationCombo = new WideComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 0, 0, 0);
        mainPanel.add(designationCombo, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private class MyCloudStatusListener implements Cloud.CloudStatusListener {
        public void handleIssueDataDownloadedEvent() {
        }


        public void handleStateChange(final Cloud.SigninState oldState, final Cloud.SigninState state) {
            updateHeader();
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
            return this;
        }
    }
}
