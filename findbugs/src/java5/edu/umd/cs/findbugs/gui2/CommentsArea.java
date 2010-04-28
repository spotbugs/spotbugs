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

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugCollectionStorageCloud;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.BugFilingStatus;
import edu.umd.cs.findbugs.util.LaunchBrowser;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author pugh
 */
public class CommentsArea {
    private static final Logger LOGGER = Logger.getLogger(CommentsArea.class.getName());

	private JTextArea userCommentsText = new JTextArea();
	private JTextArea reportText = new JTextArea();

	private Color userCommentsTextUnenabledColor;

	private JComboBox designationComboBox;

	private ArrayList<String> designationKeys;

	private JButton fileBug; 

	LinkedList<String> prevCommentsList = new LinkedList<String>();

	final static private int prevCommentsMaxSize = 10;

	private JComboBox prevCommentsComboBox; 

	private boolean dontShowAnnotationConfirmation = false;

	private boolean changed;
	final MainFrame frame;
	
	private Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private BugFilingStatus currentBugStatus;

    CommentsArea(MainFrame frame) {
		this.frame = frame;
	}

	/**
	 * Create center panel that holds the user input combo boxes and TextArea.
	 */
	JPanel createCommentsInputPanel() {
		BugCollection bc = getMainFrame().bugCollection;
		
		Cloud cloud = bc == null ? null : bc.getCloud();
		
		JPanel centerPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();

		centerPanel.setLayout(layout);

		userCommentsText.getDocument().addDocumentListener(
				new DocumentListener() {

					public void insertUpdate(DocumentEvent e) {
						setCommentsChanged(true);
						changed = true;
					}

					public void removeUpdate(DocumentEvent e) {
						setCommentsChanged(true);
						changed = true;
					}

					public void changedUpdate(DocumentEvent e) {
						changed = true;
					}

				});

		userCommentsTextUnenabledColor = centerPanel.getBackground();

		userCommentsText.setLineWrap(true);
		userCommentsText
				.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.enter_comments", "Enter your comments about this bug here"));
		userCommentsText.setWrapStyleWord(true);
		userCommentsText.setEnabled(false);
		userCommentsText.setBackground(userCommentsTextUnenabledColor);
		JScrollPane commentsScrollP = new JScrollPane(userCommentsText);

		reportText.setLineWrap(true);
		reportText
				.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.report", "Information about the bug here"));
		reportText.setWrapStyleWord(true);
		reportText.setEditable(false);
		
		JScrollPane reportScrollP = new JScrollPane(reportText);
		fileBug = new JButton(BugFilingStatus.FILE_BUG.toString());
		fileBug.setEnabled(false);
		fileBug.setToolTipText("Click to file bug for this issue");
		fileBug.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
                if (frame.currentSelectedBugLeaf == null) {
                    return;
                }
                saveComments();
                BugInstance bug = frame.currentSelectedBugLeaf.getBug();
                Cloud cloud1 = getMainFrame().bugCollection.getCloud();
                if (!cloud1.supportsBugLinks())
                    return;
                try {
                    URL u = cloud1.getBugLink(bug);
                    if (u != null) {
                        if (LaunchBrowser.showDocument(u)) {
                            cloud1.bugFiled(bug, null);
                            getMainFrame().syncBugInformation();
                        }
                    }
                } catch (Exception e1) {
                    LOGGER.log(Level.SEVERE, "Could not view/file bug", e1);
                    JOptionPane.showMessageDialog(getMainFrame(),
                                                  "Could not view/file bug:\n"
                                                  + e1.getClass().getSimpleName() + "\n" + e1.getMessage());
                }
            }});
		
		prevCommentsComboBox = new JComboBox();
		prevCommentsComboBox.setEnabled(false);
		prevCommentsComboBox
				.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.reuse_comments", "Use this to reuse a previous textual comment for this bug"));
		prevCommentsComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED
						&& prevCommentsComboBox.getSelectedIndex() != 0) {
					setCurrentUserCommentsText(getCurrentPrevCommentsSelection());

					prevCommentsComboBox.setSelectedIndex(0);
				}
			}
		});

		designationComboBox = new JComboBox();
		designationKeys = new ArrayList<String>();

		designationComboBox.setEnabled(false);
		designationComboBox
				.setToolTipText(edu.umd.cs.findbugs.L10N.getLocalString("tooltip.select_designation", "Select a user designation for this bug"));
		designationComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (frame.userInputEnabled
						&& e.getStateChange() == ItemEvent.SELECTED) {
					if (frame.currentSelectedBugLeaf == null)
						setDesignationNonLeaf(designationComboBox
								.getSelectedItem().toString());
					else if (!alreadySelected())
						setDesignation(designationComboBox.getSelectedItem()
								.toString());
				}
			}

			/*
			 * Checks to see if the designation is already selected as that.
			 * This was created because it was found the itemStateChanged method
			 * is called when the combo box is set when a bug is clicked.
			 */
			private boolean alreadySelected() {
				return designationKeys.get(
						designationComboBox.getSelectedIndex()).equals(
						frame.currentSelectedBugLeaf.getBug()
								.getUserDesignationKey());
			}
		});

		designationKeys.add("");
		designationComboBox.addItem("");
		for (String s : I18N.instance().getUserDesignationKeys(true)) {
			designationKeys.add(s);
			designationComboBox.addItem(Sortables.DESIGNATION.formatValue(s));
		}
		setUnknownDesignation();

		//JPanel comments = new JPanel();
		// comments.setLayout(new FlowLayout(FlowLayout.LEFT));
		// comments.add(designationComboBox);
		// comments.add(whoWhen);
		
        JPanel myStuffPanel = new JPanel(new GridBagLayout());
        myStuffPanel.setBorder(new TitledBorder("My Evaluation"));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;

		c.fill=GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		
		myStuffPanel.add(designationComboBox, c);
		
		
		c.gridx = 0;
		c.gridy++;
		c.weightx = 1;
		c.weighty = 2;
		c.gridwidth = 2;
		c.fill=GridBagConstraints.BOTH;
		myStuffPanel.add(commentsScrollP, c);

        centerPanel.add(myStuffPanel, c);
		
		if (cloud != null && cloud.supportsCloudReports()) {
			c.gridy++;
			c.weightx = 1;
			c.weighty = 1;
			c.fill=GridBagConstraints.BOTH;
		
			centerPanel.add(reportScrollP, c);
		}
		
		if (cloud != null && cloud.supportsBugLinks()) {
			c.gridy++;
			c.weightx = 0;
			c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
			centerPanel.add(fileBug, c);
		}

		return centerPanel;
	}

    void setUnknownDesignation() {
		assert designationComboBox.getItemCount() == designationKeys.size();
		designationComboBox.setSelectedIndex(0); // WARNING: this is hard
													// coded in here.
	}

	/**
	 * Sets the user comment panel to whether or not it is enabled. If isEnabled
	 * is false will clear the user comments text pane.
	 * 
	 * @param isEnabled
	 */
	void setUserCommentInputEnable(final boolean isEnabled) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setUserCommentInputEnableFromSwingThread(isEnabled);
			}
		});
	}

	/**
	 * Sets the user comment panel to whether or not it is enabled. If isEnabled
	 * is false will clear the user comments text pane.
	 * 
	 * @param isEnabled
	 */
	void setUserCommentInputEnableFromSwingThread(final boolean isEnabled) {
		frame.userInputEnabled = isEnabled;
		if (!isEnabled) {
//			This so if already saved doesn't make it seem project changed
			boolean b = frame.getProjectChanged();
			userCommentsText.setText("");
			// WARNING: this is hard coded in here, but needed
			// so when not enabled shows default setting of designation
			setUnknownDesignation();
			userCommentsText.setBackground(userCommentsTextUnenabledColor);
			setCommentsChanged(b);
		} else
			userCommentsText.setBackground(Color.WHITE);
		userCommentsText.setEnabled(isEnabled);
		prevCommentsComboBox.setEnabled(isEnabled);
		designationComboBox.setEnabled(isEnabled);
	}


	/**
	 * Updates comments tab. Takes node passed and sets the designation and
	 * comments.
	 * 
	 * @param node
	 */
	void updateCommentsFromLeafInformation(final BugLeafNode node) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				//This so if already saved doesn't make it seem project changed
				boolean b = frame.getProjectChanged();
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
				if (!plugin.canStoreUserAnnotation(bug)) {
					designationComboBox.setSelectedIndex(0);
					setCurrentUserCommentsText("");
					reportText.setText("Issue not persisted to database");
					setUserCommentInputEnableFromSwingThread(false);
				} else {
					setCurrentUserCommentsText(bug.getAnnotationText());
					if (plugin.supportsCloudReports()) {
						String report = plugin.getCloudReport(bug);
						reportText.setText(report);
					}
					designationComboBox.setSelectedIndex(designationKeys.indexOf(bug.getUserDesignationKey()));
					setUserCommentInputEnableFromSwingThread(plugin.canStoreUserAnnotation(bug));
				}
				changed = false;
				setCommentsChanged(b);
			}
		});
	}

	void updateCommentsFromNonLeafInformation(final BugAspects theAspects) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//This so if already saved doesn't make it seem project changed
				boolean b = frame.getProjectChanged();
				updateCommentsFromNonLeafInformationFromSwingThread(theAspects);
				setUserCommentInputEnableFromSwingThread(true);
				changed = false;
				setCommentsChanged(b);
			}
		});
	}

	/**
	 * Saves the current comments to the BugLeafNode passed in. If the passed in
	 * node's annotation is already equal to the current user comment then will
	 * not do anything so setProjectedChanged is not made true. Will also add
	 * the comment if it is new to the previous comments list.
	 * 
	 * @param node
	 */
	private void saveCommentsToBug(BugLeafNode node) {
		if (node == null)
			return;

		final String comments = getCurrentUserCommentsText();
		final BugInstance bug = node.getBug();
		if (bug.getAnnotationText().equals(comments))
			return;

		// may talk to server - should run in background
		backgroundExecutor.execute(new Runnable() {
	        public void run() {
                bug.setAnnotationText(comments, MainFrame.getInstance().bugCollection);
                setCommentsChanged(true);
		        changed = false;
		        addToPrevComments(comments);
	        }
        });
	}

	private boolean confirmAnnotation() {

		String[] options = { edu.umd.cs.findbugs.L10N.getLocalString("dlg.yes_btn", "Yes"), edu.umd.cs.findbugs.L10N.getLocalString("dlg.no_btn", "No"), edu.umd.cs.findbugs.L10N.getLocalString("dlg.yes_dont_ask_btn", "Yes, and don't ask me this again")};
		if (dontShowAnnotationConfirmation)
			return true;
		int choice = JOptionPane
				.showOptionDialog(
						frame,
						edu.umd.cs.findbugs.L10N.getLocalString("dlg.changing_text_lbl", "Changing this text box will overwrite the annotations associated with all bugs in this folder and subfolders. Are you sure?"),
						edu.umd.cs.findbugs.L10N.getLocalString("dlg.annotation_change_ttl", "Annotation Change"), JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
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

	private void saveCommentsToNonLeaf(BugAspects aspects) {
		if (aspects == null)
			return;
		if (!changed) return;
		String newComment = getCurrentUserCommentsText();
		if (newComment.equals(getNonLeafCommentsText(aspects)))
			return;
		else if (confirmAnnotation()) {
			BugSet filteredSet = aspects
					.getMatchingBugs(BugSet.getMainBugSet());
			for (BugLeafNode nextNode : filteredSet) {
				saveCommentsToBug(nextNode);
			}
		}
		changed = false;

	}

	/**
	 * Saves comments to the current selected bug.
	 * 
	 */

	public void saveComments() {
		saveComments(frame.currentSelectedBugLeaf,
				frame.currentSelectedBugAspects);
	}

	public void saveComments(BugLeafNode theNode, BugAspects theAspects) {
		if (theNode != null)
			saveCommentsToBug(theNode);
		else
			saveCommentsToNonLeaf(theAspects);
	}

	/**
	 * Deletes the list have already. Then loads from list. Will load from the
	 * list until run out of room in the prevCommentsList.
	 * 
	 * @param list
	 */
	void loadPrevCommentsList(String[] list) {
		int count = 0;
		for (String str : list) {
			if (str.equals(""))
				count++;
		}

		String[] ary = new String[list.length - count];
		int j = 0;
		for (String str : list) {
			if (!str.equals("")) {
				ary[j] = str;
				j++;
			}
		}

		String[] temp;
		prevCommentsList = new LinkedList<String>();
		if ((ary.length) > prevCommentsMaxSize) {
			temp = new String[prevCommentsMaxSize];
			for (int i = 0; i < temp.length && i < ary.length; i++)
				temp[i] = ary[i];
		} else {
			temp = new String[ary.length];
            System.arraycopy(ary, 0, temp, 0, ary.length);
		}

        prevCommentsList.addAll(Arrays.asList(temp));

		resetPrevCommentsComboBox();
	}

	/**
	 * Adds the comment into the list. If the comment is already in the list
	 * then simply moves to the front. If the list is too big when adding the
	 * comment then deletes the last comment on the list.
	 * 
	 * @param comment
	 */
	private void addToPrevComments(String comment) {
		if (comment.equals(""))
			return;

		if (prevCommentsList.contains(comment)) {
			int index = prevCommentsList.indexOf(comment);
			if (index == 0)
				return;
			prevCommentsList.remove(index);
		}

		prevCommentsList.addFirst(comment);

		while (prevCommentsList.size() > prevCommentsMaxSize)
			prevCommentsList.removeLast();

		resetPrevCommentsComboBox();
	}

	/**
	 * Removes all items in the comboBox for previous comments. Then refills it
	 * using prevCommentsList.
	 * 
	 */
	private void resetPrevCommentsComboBox() {
		prevCommentsComboBox.removeAllItems();

		prevCommentsComboBox.addItem("");

		for (String str : prevCommentsList) {
			if (str.length() < 20)
				prevCommentsComboBox.addItem(str);
			else
				prevCommentsComboBox.addItem(str.substring(0, 17) + "...");
		}
	}

	/**
	 * Returns the text in the current user comments textArea.
	 * 
	 * @return
	 */
	private String getCurrentUserCommentsText() {
		return userCommentsText.getText();
	}

	/**
	 * Sets the current user comments text area to comment.
	 * 
	 * @param comment
	 */
	private void setCurrentUserCommentsText(String comment) {
		changed = true;
		userCommentsText.setText(comment);
	}

	/**
	 * Returns the current selected previous comments. Returns as an object.
	 */
	private String getCurrentPrevCommentsSelection() {
		return prevCommentsList
				.get(prevCommentsComboBox.getSelectedIndex() - 1);
	}

	void addDesignationItem(JMenu menu, final String menuName, int keyEvent) {
		JMenuItem toggleItem = new JMenuItem(menuName);

		toggleItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (frame.currentSelectedBugLeaf == null)
					setDesignationNonLeaf(menuName);
				else
					setDesignation(menuName);
			}
		});
		MainFrame.attachAcceleratorKey(toggleItem, keyEvent);
		menu.add(toggleItem);
	}

	void setDesignation(String designationName) {
		if (frame.currentSelectedBugLeaf == null)
			return;
		String designationKey = convertDesignationNameToDesignationKey(designationName);
		if (designationKey == null)
			return;
		BugCollection bugCollection = MainFrame.getInstance().bugCollection;
		BugInstance bug = frame.currentSelectedBugLeaf.getBug();
		String oldValue = bug.getUserDesignationKey();
		if (designationKey.equals(oldValue))
			return;
		Cloud plugin = bugCollection != null? bugCollection.getCloud() : null;
		if (plugin != null && designationKey.equals("I_WILL_FIX") && plugin.supportsClaims()) {
			String claimedBy = plugin.claimedBy(bug);
			if (claimedBy != null && !plugin.getUser().equals(claimedBy)) {
				int result = JOptionPane.showConfirmDialog(null, 
						claimedBy + " has already said they will fix this issue\n"
						+ "Do you want to also be listed as fixing this issue?\n"
						+ "If so, please coordinate with " + claimedBy,
					 "Issue already claimed", JOptionPane.YES_NO_CANCEL_OPTION);
				if (result == JOptionPane.CANCEL_OPTION)
					return;
				if (result != JOptionPane.YES_OPTION)
					designationKey = "MUST_FIX";	
			}
		}
		if (changeDesignationOfBug(frame.currentSelectedBugLeaf, designationKey)){
			if (plugin != null && plugin.supportsCloudReports()) {
				String report = plugin.getCloudReport(bug);
				reportText.setText(report);
			}
			changed = true;
			setCommentsChanged(true);
		}
		setDesignationComboBox(designationKey);
	}

	protected void setDesignationNonLeaf(String designationName) {
		if (nonleafUpdateDepth > 0)
			return;
		String designationKey = convertDesignationNameToDesignationKey(designationName);
		if (designationKey == null || frame.currentSelectedBugAspects == null)
			return;
		Cloud cloud = getMainFrame().bugCollection.getCloud();
		if (cloud.getMode() == Cloud.Mode.VOTING) {
			JOptionPane.showMessageDialog(frame, "FindBugs is configured in voting mode; no mass updates allowed");
			return;
		}
		

		BugSet filteredSet = frame.currentSelectedBugAspects
				.getMatchingBugs(BugSet.getMainBugSet());
		for (BugLeafNode nextNode : filteredSet)
			if (changeDesignationOfBug(nextNode, designationKey)){
				changed = true;
				setCommentsChanged(true);
			}
		setDesignationComboBox(designationKey);
	}

	protected boolean changeDesignationOfBug(BugLeafNode theNode, final String selection) {
		saveComments();
		final BugInstance bug = theNode.getBug();
		String oldValue = bug.getUserDesignationKey();
		if (selection.equals(oldValue))
			return false;
		backgroundExecutor.execute(new Runnable() {
	        public void run() {
                bug.setUserDesignationKey(selection, MainFrame.getInstance().bugCollection);
            }
        });
		return true;
	}

	protected void updateDesignationComboBox() {
		if (frame.currentSelectedBugLeaf == null)
			updateCommentsFromNonLeafInformationFromSwingThread(frame.currentSelectedBugAspects);
		else {
			Cloud cloud = getMainFrame().bugCollection.getCloud();
			BugInstance bug = frame.currentSelectedBugLeaf.getBug();
			if (!cloud.canStoreUserAnnotation(bug)) {
				designationComboBox.setEnabled(false);
				designationComboBox.setSelectedIndex(0);
				return;
			}
			
			designationComboBox.setEnabled(true);
			int selectedIndex = designationComboBox
								.getSelectedIndex();
			if (selectedIndex >= 0) 
				setDesignationComboBox(designationKeys.get(selectedIndex));
			else
				Debug.println("Couldn't find selected index in designationComboBox: " + designationComboBox.getSelectedItem());
		}
	}

	int nonleafUpdateDepth = 0;
	protected void updateCommentsFromNonLeafInformationFromSwingThread(BugAspects theAspects) {
		if (theAspects == null)
			return;
		BugSet filteredSet = theAspects.getMatchingBugs(BugSet.getMainBugSet());
		boolean allSame = true;
		int first = -1;
		for (BugLeafNode nextNode : filteredSet) {

			int designationIndex = designationKeys.indexOf(nextNode.getBug()
					.getUserDesignationKey());
			if (first == -1) {
				first = designationIndex;
			} else {
				if (designationIndex != first)
					allSame = false;
			}
		}
		nonleafUpdateDepth++;
		try {
		if (allSame) {
			designationComboBox.setSelectedIndex(first);
		} else {
			designationComboBox.setSelectedIndex(0);
		}
		userCommentsText.setText(getNonLeafCommentsText(theAspects));
		Cloud cloud = getCloud();
		if (cloud != null && cloud.getMode() == Cloud.Mode.VOTING) {
			userCommentsText.setEnabled(false);
		}
		fileBug.setEnabled(false);
		changed = false;
		} finally {
			nonleafUpdateDepth--;
		}
	}

	protected String getNonLeafCommentsText(BugAspects theAspects)
	{	if (theAspects == null)
			return "";
		BugSet filteredSet = theAspects.getMatchingBugs(BugSet.getMainBugSet());
		boolean allSame = true;
		String comments = null;
		for (BugLeafNode nextNode : filteredSet) {
		String commentsOnThisBug = nextNode.getBug().getAnnotationText();
			if (comments == null) {
				comments = commentsOnThisBug;
			} else {
				if (!commentsOnThisBug.equals(comments))
					allSame = false;
			}
		}
		if(comments == null || !allSame)
			return "";
		else return comments;
	}

	protected void setDesignationComboBox(String designationKey) {
		assert designationComboBox.getItemCount() == designationKeys.size();
		
		int numItems = designationComboBox.getItemCount();
		for (int i = 0; i < numItems; i++) {
			String value = designationKeys.get(i);
			if (designationKey.equals(value)) {
				designationComboBox.setSelectedIndex(i);
				return;
				}
		}
		if (MainFrame.DEBUG) 
			System.out.println("Couldn't find combo box for " + designationKey);
	}

	@SuppressWarnings({"deprecation"})
    public void moveNodeAccordingToDesignation(BugLeafNode theNode,
			String selection) {

		if (!getSorter().getOrder().contains(Sortables.DESIGNATION)) {
			// designation not sorted on at all

            theNode.getBug().setUserDesignationKey(
                    selection,  MainFrame.getInstance().bugCollection);

        } else if (getSorter().getOrderBeforeDivider().contains(
				Sortables.DESIGNATION)) {

			BugTreeModel model = getModel();
			TreePath path = model.getPathToBug(theNode.getBug());
			if (path == null) {
                theNode.getBug().setUserDesignationKey(
                        selection,  MainFrame.getInstance().bugCollection);
                return;
			}
			Object[] objPath = path.getParentPath().getPath();
			ArrayList<Object> reconstruct = new ArrayList<Object>();
			ArrayList<TreePath> listOfNodesToReconstruct = new ArrayList<TreePath>();
            for (Object o : objPath) {
                reconstruct.add(o);
                if (o instanceof BugAspects) {
                    if (((BugAspects) o).getCount() == 1) {
                        // Debug.println((BugAspects)(o));
                        break;
                    }
                }
                TreePath pathToNode = new TreePath(reconstruct.toArray());
                listOfNodesToReconstruct.add(pathToNode);
            }

            theNode.getBug().setUserDesignationKey(
                    selection,  MainFrame.getInstance().bugCollection);
            model.bugTreeFilterListener.suppressBug(path);
			TreePath unsuppressPath = model.getPathToBug(theNode.getBug());
			if (unsuppressPath != null)// If choosing their designation has not
										// moved the bug under any filters
			{
				model.bugTreeFilterListener.unsuppressBug(unsuppressPath);
				// tree.setSelectionPath(unsuppressPath);
			}
			for (TreePath pathToNode : listOfNodesToReconstruct) {
				model.treeNodeChanged(pathToNode);
			}
			setCommentsChanged(true);
		} else if (getSorter().getOrderAfterDivider().contains(
				Sortables.DESIGNATION)) {

            theNode.getBug().setUserDesignationKey(
                    selection,  MainFrame.getInstance().bugCollection);
            BugTreeModel model = getModel();
			TreePath path = model.getPathToBug(theNode.getBug());
			if (path != null)
				model.sortBranch(path.getParentPath());

		}
	}
	

	protected @CheckForNull String convertDesignationNameToDesignationKey(String name) {
		/*
		 * This converts a designation name from human-readable format ("mostly
		 * harmless", "critical") to the program's internal format
		 * ("MOSTLY_HARMLESS", "CRITICAL") etc. This uses the
		 * DesignationComboBox (this should probably be changed)
		 */
		assert designationComboBox.getItemCount() == designationKeys.size();
		int itemCount = designationComboBox.getItemCount();
		for (int i = 1; i < itemCount; i++)
			if (name.equals(designationComboBox.getItemAt(i)))
				return designationKeys.get(i);
		return null;
	}

	private void setCommentsChanged(boolean b) {
		Cloud cloud = getCloud();
		if (cloud == null || cloud instanceof BugCollectionStorageCloud)
		  frame.setProjectChanged(b);
	}

	/**
	 * Returns the SorterTableColumnModel of the MainFrame.
	 * 
	 * @return
	 */
	SorterTableColumnModel getSorter() {
		return frame.getSorter();
	}

	public void resized() {
		resetPrevCommentsComboBox();
		userCommentsText.validate();
	}

	BugTreeModel getModel() {
		return (BugTreeModel) frame.tree.getModel();
	}

	public boolean hasFocus() {
		return userCommentsText.hasFocus();
	}

	/**
     * @return
     */
    private @CheckForNull Cloud getCloud() {
	    MainFrame instance = MainFrame.getInstance();
		BugCollection bugCollection = instance.bugCollection;
		if (bugCollection == null)
			return null;
		return bugCollection.getCloud();
    }

	private MainFrame getMainFrame() {
	    return MainFrame.getInstance();
    }
	
	public void configureForCurrentCloud() {
		Cloud cloud = getCloud();
		if (fileBug != null) 
			fileBug.setEnabled(cloud.supportsBugLinks());
		
		MainFrame.getInstance().resetCommentsInputPane();
	}
}
