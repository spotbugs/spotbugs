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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreePath;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class CommentsArea {

	private JTextArea userCommentsText = new JTextArea();

	private Color userCommentsTextUnenabledColor;

	private JComboBox designationComboBox;

	private ArrayList<String> designationKeys;

	LinkedList<String> prevCommentsList = new LinkedList<String>();

	final static private int prevCommentsMaxSize = 10;

	private JComboBox prevCommentsComboBox = new JComboBox();

	private boolean dontShowAnnotationConfirmation = false;

	private boolean changed;
	final MainFrame frame;

	CommentsArea(MainFrame frame) {
		this.frame = frame;
	}

	/**
	 * Create center panel that holds the user input combo boxes and TextArea.
	 */
	JPanel createCommentsInputPanel() {
		JPanel centerPanel = new JPanel();
		BorderLayout centerLayout = new BorderLayout();
		centerLayout.setVgap(10);
		centerPanel.setLayout(centerLayout);

		userCommentsText.getDocument().addDocumentListener(
				new DocumentListener() {

					public void insertUpdate(DocumentEvent e) {
						frame.setProjectChanged(true);
						changed = true;
					}

					public void removeUpdate(DocumentEvent e) {
						frame.setProjectChanged(true);
						changed = true;
					}

					public void changedUpdate(DocumentEvent e) {
						changed = true;
					}

				});

		userCommentsTextUnenabledColor = centerPanel.getBackground();

		userCommentsText.setLineWrap(true);
		userCommentsText
				.setToolTipText(L10N.getLocalString("tooltip.enter_comments", "Enter your comments about this bug here"));
		userCommentsText.setWrapStyleWord(true);
		userCommentsText.setEnabled(false);
		userCommentsText.setBackground(userCommentsTextUnenabledColor);
		JScrollPane commentsScrollP = new JScrollPane(userCommentsText);

		prevCommentsComboBox.setEnabled(false);
		prevCommentsComboBox
				.setToolTipText(L10N.getLocalString("tooltip.reuse_comments", "Use this to reuse a previous textual comment for this bug"));
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
				.setToolTipText(L10N.getLocalString("tooltip.select_designation", "Select a user designation for this bug"));
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
								.getNonnullUserDesignation()
								.getDesignationKey());
			}
		});

		designationKeys.add("");
		designationComboBox.addItem("");
		for (String s : I18N.instance().getUserDesignationKeys(true)) {
			designationKeys.add(s);
			designationComboBox.addItem(Sortables.DESIGNATION.formatValue(s));
		}
		setUnknownDesignation();

		centerPanel.add(designationComboBox, BorderLayout.NORTH);
		centerPanel.add(commentsScrollP, BorderLayout.CENTER);
		centerPanel.add(prevCommentsComboBox, BorderLayout.SOUTH);

		return centerPanel;
	}

	void setUnknownDesignation() {
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
			userCommentsText.setText("");
			// WARNING: this is hard coded in here, but needed
			// so when not enabled shows default setting of designation
			setUnknownDesignation();
			userCommentsText.setBackground(userCommentsTextUnenabledColor);
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
				BugInstance bug = node.getBug();
				setCurrentUserCommentsText(bug.getAnnotationText());
				designationComboBox.setSelectedIndex(designationKeys
						.indexOf(bug.getNonnullUserDesignation()
								.getDesignationKey()));
				setUserCommentInputEnableFromSwingThread(true);
				changed = false;
			}
		});
	}

	void updateCommentsFromNonLeafInformation(final BugAspects theAspects) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateCommentsFromNonLeafInformationFromSwingThread(theAspects);
				setUserCommentInputEnableFromSwingThread(true);
				changed = false;
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

		String comments = getCurrentUserCommentsText();
		if (node.getBug().getAnnotationText().equals(comments))
			return;

		node.getBug().setAnnotationText(comments);
		setProjectChanged(true);
		changed = false;
		addToPrevComments(comments);
	}

	private boolean confirmAnnotation() {

		String[] options = { L10N.getLocalString("dlg.yes_btn", "Yes"), L10N.getLocalString("dlg.no_btn", "No"), L10N.getLocalString("dlg.yes_dont_ask_btn", "Yes, and don't ask me this again")};
		if (dontShowAnnotationConfirmation)
			return true;
		int choice = JOptionPane
				.showOptionDialog(
						frame,
						L10N.getLocalString("dlg.changing_text_lbl", "Changing this text box will overwrite the annotations associated with all bugs in this folder and subfolders. Are you sure?"),
						L10N.getLocalString("dlg.annotation_change_ttl", "Annotation Change"), JOptionPane.DEFAULT_OPTION,
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
			for (int i = 0; i < ary.length; i++)
				temp[i] = ary[i];
		}

		for (String str : temp)
			prevCommentsList.add(str);

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
		JMenuItem toggleItem = MainFrame.newJMenuItem(menuName);

		toggleItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (frame.currentSelectedBugLeaf == null)
					setDesignationNonLeaf(menuName);
				else
					setDesignation(menuName);
			}
		});
		MainFrame.attachAccelaratorKey(toggleItem, keyEvent);
		menu.add(toggleItem);
	}

	void setDesignation(String designationName) {
		if (frame.currentSelectedBugLeaf == null)
			return;
		String designationKey = convertDesignationNameToDesignationKey(designationName);
		if (designationKey == null)
			return;
		if (changeDesignationOfBug(frame.currentSelectedBugLeaf, designationKey))
			changed = true;
		setDesignationComboBox(designationName);
	}

	protected void setDesignationNonLeaf(String designationName) {
		String designationKey = convertDesignationNameToDesignationKey(designationName);
		if (designationKey == null || frame.currentSelectedBugAspects == null)
			return;

		BugSet filteredSet = frame.currentSelectedBugAspects
				.getMatchingBugs(BugSet.getMainBugSet());
		for (BugLeafNode nextNode : filteredSet)
			if (changeDesignationOfBug(nextNode, designationKey)) changed = true;
		setDesignationComboBox(designationName);
	}

	protected boolean changeDesignationOfBug(BugLeafNode theNode, String selection) {
		BugDesignation userDesignation = theNode.getBug().getNonnullUserDesignation();
		if (userDesignation.getDesignationKey().equals(selection)) return false;
		userDesignation.setDesignationKey(selection);
		return true;
	}

	protected void updateDesignationComboBox() {
		if (frame.currentSelectedBugLeaf == null)
			updateCommentsFromNonLeafInformationFromSwingThread(frame.currentSelectedBugAspects);
		else {
			int selectedIndex = designationComboBox
								.getSelectedIndex();
			if (selectedIndex >= 0) setDesignationComboBox(designationKeys.get(selectedIndex));
			else
				Debug.println("Couldn't find selected index in designationComboBox: " + designationComboBox.getSelectedItem());
		}
	}

	protected void updateCommentsFromNonLeafInformationFromSwingThread(BugAspects theAspects) {
		if (theAspects == null)
			return;
		BugSet filteredSet = theAspects.getMatchingBugs(BugSet.getMainBugSet());
		boolean allSame = true;
		int first = -1;
		for (BugLeafNode nextNode : filteredSet) {

			int designationIndex = designationKeys.indexOf(nextNode.getBug()
					.getNonnullUserDesignation().getDesignationKey());
			if (first == -1) {
				first = designationIndex;
			} else {
				if (designationIndex != first)
					allSame = false;
			}
		}
		;
		if (allSame) {
			designationComboBox.setSelectedIndex(first);
		} else {
			designationComboBox.setSelectedIndex(0);
		}
		userCommentsText.setText(getNonLeafCommentsText(theAspects));
		changed = false;
		// setUserCommentInputEnableFromSwingThread(true);
	}
	
	protected String getNonLeafCommentsText(BugAspects theAspects)
	{	if (theAspects == null)
			return "";
		BugSet filteredSet = theAspects.getMatchingBugs(BugSet.getMainBugSet());
		Iterator<BugLeafNode> filteredIter = filteredSet.iterator();
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
		if((comments == null) || (allSame == false))
			return "";
		else return comments;
	}

	protected void setDesignationComboBox(String selection) {
		int numItems = designationComboBox.getItemCount();
		for (int i = 0; i < numItems; i++)
			if (selection.equals(designationKeys.get(i)))
				designationComboBox.setSelectedIndex(i);
	}

	public void moveNodeAccordingToDesignation(BugLeafNode theNode,
			String selection) {

		if (!getSorter().getOrder().contains(Sortables.DESIGNATION)) {
			// designation not sorted on at all

			theNode.getBug().getNonnullUserDesignation().setDesignationKey(
					selection);

		} else if (getSorter().getOrderBeforeDivider().contains(
				Sortables.DESIGNATION)) {

			BugTreeModel model = getModel();
			TreePath path = model.getPathToBug(theNode.getBug());
			if (path == null) {
				theNode.getBug().getNonnullUserDesignation().setDesignationKey(
						selection);
				return;
			}
			Object[] objPath = path.getParentPath().getPath();
			ArrayList<Object> reconstruct = new ArrayList<Object>();
			ArrayList<TreePath> listOfNodesToReconstruct = new ArrayList<TreePath>();
			for (int x = 0; x < objPath.length; x++) {
				Object o = objPath[x];
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

			theNode.getBug().getNonnullUserDesignation().setDesignationKey(
					selection);
			model.suppressBug(path);
			TreePath unsuppressPath = model.getPathToBug(theNode.getBug());
			if (unsuppressPath != null)// If choosing their designation has not
										// moved the bug under any filters
			{
				model.unsuppressBug(unsuppressPath);
				// tree.setSelectionPath(unsuppressPath);
			}
			for (TreePath pathToNode : listOfNodesToReconstruct) {
				model.treeNodeChanged(pathToNode);
			}
			setProjectChanged(true);
		} else if (getSorter().getOrderAfterDivider().contains(
				Sortables.DESIGNATION)) {

			theNode.getBug().getNonnullUserDesignation().setDesignationKey(
					selection);
			BugTreeModel model = getModel();
			TreePath path = model.getPathToBug(theNode.getBug());
			if (path != null)
				model.sortBranch(path.getParentPath());

		}
	}

	protected @CheckForNull
	String convertDesignationNameToDesignationKey(String name) {
		/*
		 * This converts a designation name from human-readable format ("mostly
		 * harmless", "critical") to the program's internal format
		 * ("MOSTLY_HARMLESS", "CRITICAL") etc. This uses the
		 * DesignationComboBox (this should probably be changed)
		 */
		int itemCount = designationComboBox.getItemCount();
		for (int i = 1; i < itemCount; i++)
			if (name.equals(designationComboBox.getItemAt(i)))
				return designationKeys.get(i);
		return null;
	}

	private void setProjectChanged(boolean b) {
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
}
