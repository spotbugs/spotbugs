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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;


/**
 * User Preferences
 */

/*
 * 
 * Preferences, which should really be renamed Filters And Suppressions (fas, like fas file!)
 * since thats all that's actually here now
 *
 */
@SuppressWarnings("serial")
public class PreferencesFrame extends FBDialog {

	private static int PROPERTIES_TAB = 0;
	private static int FILTERS_TAB = 1;
	private static int SUPPRESSIONS_TAB = 2;

	JTabbedPane mainTabPane;

	private static PreferencesFrame instance;
	private CheckBoxList filterCheckBoxList = new CheckBoxList();
	private JButton addButton;
	
	JButton removeButton;
	JButton removeAllButton;
	boolean frozen=false;

	//Variables for Properties tab.
	private JTextField tabTextField;
	private JTextField fontTextField;
	private static int TAB_MIN = 1;
	private static int TAB_MAX = 20;
	private static int FONT_MIN = 10;
	private static int FONT_MAX = 99;

	public static PreferencesFrame getInstance()
	{
//		MainFrame.getInstance().getSorter().freezeOrder();
		if(instance == null)
			instance = new PreferencesFrame();

		return instance;
	}

	private PreferencesFrame(){
		setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.fil_sup_ttl", "Filters/Suppressions"));
		setModal(true);

		mainTabPane = new JTabbedPane();

		mainTabPane.add("Properties", createPropertiesPane());

		mainTabPane.add(edu.umd.cs.findbugs.L10N.getLocalString("pref.filters", "Filters"), createFilterPane());		

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
		bottom.add(new JButton(new AbstractAction(edu.umd.cs.findbugs.L10N.getLocalString("pref.close", "Close"))
		{
			public void actionPerformed(ActionEvent evt)
			{
//				MainFrame.getInstance().getSorter().thawOrder();
				PreferencesFrame.this.setVisible(false);
				TreeModel bt= (MainFrame.getInstance().getTree().getModel());
				if (bt instanceof BugTreeModel)		
					((BugTreeModel)bt).checkSorter();

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

//	/**
//	 * Creates the general tab. This lets the user change the font
//	 * size and the total number of possible previous comments.
//	 * @return
//	 */
//	private JPanel createGeneralPane() {
//		JPanel generalPanel = new JPanel();
//		
//		generalPanel.add(new JLabel("Clear Recent Projects List"));
//		JButton clearButton=new JButton("Clear");
//		clearButton.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				GUISaveState.getInstance().clear();
//			}
//		});
//		return generalPanel;
//	}
//	

	private JPanel createPropertiesPane()
	{
		JPanel contentPanel = new JPanel(new BorderLayout());
		JPanel mainPanel = new JPanel();

		float currFS = Driver.getFontSize();

		JPanel temp = new JPanel();
		temp.add(new JLabel("Tab Size"));
		tabTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getTabSize()));
		tabTextField.setPreferredSize(new Dimension((int)(currFS*2), (int)(currFS*1.3)));
		temp.add(tabTextField);

		mainPanel.add(temp);
		mainPanel.add(Box.createVerticalStrut(5));

		temp = new JPanel();
		temp.add(new JLabel("Font Size"));
		fontTextField = new JTextField(Float.toString(GUISaveState.getInstance().getFontSize()));
		fontTextField.setPreferredSize(new Dimension((int)(currFS*3), (int)(currFS*1.3)));
		temp.add(fontTextField);

		mainPanel.add(temp);
		mainPanel.add(Box.createVerticalGlue());

		contentPanel.add(mainPanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JButton(new AbstractAction("Apply")
		{
			public void actionPerformed(ActionEvent evt)
			{
				changeTabSize();
				changeFontSize();
			}
		}));

		bottomPanel.add(new JButton(new AbstractAction("Reset")
		{
			public void actionPerformed(ActionEvent evt)
			{
				resetPropertiesPane();
			}
		}));

		contentPanel.add(bottomPanel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(WindowEvent e) {
				resetPropertiesPane();
			}
		});

		return contentPanel;
	}

	private void changeTabSize(){
		int tabSize = 0;

		try{
			tabSize = Integer.decode(tabTextField.getText()).intValue();					
		}
		catch(NumberFormatException exc){
			JOptionPane.showMessageDialog(instance,	"Error in tab size field.",
					"Tab Size Error", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if(tabSize < TAB_MIN || tabSize > TAB_MAX){
			JOptionPane.showMessageDialog(instance,	"Tab size excedes range ("+TAB_MIN+" - "+TAB_MAX+").",
					"Tab Size Excedes Range", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if(tabSize != GUISaveState.getInstance().getTabSize()){
			GUISaveState.getInstance().setTabSize(tabSize);
			MainFrame.getInstance().displayer.clearCache();
			//This causes the GUI to redisplay the current code
			MainFrame.getInstance().syncBugInformation();
		}
	}

	private void changeFontSize(){
		float fontSize = 0;

		try{
			fontSize = Float.parseFloat(fontTextField.getText());				
		}
		catch(NumberFormatException exc){
			JOptionPane.showMessageDialog(instance,	"Error in font size field.",
					"Font Size Error", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if(fontSize < FONT_MIN || fontSize > FONT_MAX){
			JOptionPane.showMessageDialog(instance,	"Font size excedes range ("+FONT_MIN+" - "+FONT_MAX+").",
					"Font Size Excedes Range", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if(fontSize != GUISaveState.getInstance().getFontSize()){
			GUISaveState.getInstance().setFontSize(fontSize);
			JOptionPane.showMessageDialog(instance,	"To implement the new font size please restart FindBugs.",
					"Changing Font", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void resetPropertiesPane()
	{
		tabTextField.setText(Integer.toString(GUISaveState.getInstance().getTabSize()));
		fontTextField.setText(Float.toString(GUISaveState.getInstance().getFontSize()));
	}

	
	/**
	 * Create a JPanel to display the filtering controls.
	 */
	private JPanel createFilterPane()
	{
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
		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				NewFilterFrame.open();
			}
		});

		gbc.gridy = 1;
		gbc.insets = new Insets(5, 0, 0, 0);
		filterPanel.add(removeButton, gbc);
		removeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Object[] selected=filterCheckBoxList.getSelectedValues();
				boolean needsRebuild=false;
				if (selected.length == 0)
					return;
				for (Object o: selected)
				{
					MatchBox box=(MatchBox) o;
					if (MainFrame.getInstance().getProject().getSuppressionFilter().isEnabled(box.getMatcher()))
						needsRebuild=true;
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
		removeAllButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent evt)
					{
						boolean needsRebuild=false;
						Filter suppressionFilter = MainFrame.getInstance().getProject().getSuppressionFilter();
						if (!suppressionFilter.isEmpty())
							needsRebuild=true;
						suppressionFilter.clear();

						if (needsRebuild)//TODO This will rebuild even if all the filters being cleared were disabled
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

	private class MatchBox extends JCheckBox{
		Matcher m;
		MatchBox(String text, Matcher m){
			super(text);
			this.m=m;
		}
		Matcher getMatcher(){return m;}
	}
	
	void updateFilterPanel()
	{
		ArrayList<MatchBox> boxes = new ArrayList<MatchBox>();
		final Filter f = MainFrame.getInstance().getProject().getSuppressionFilter();
		
		for(Iterator<Matcher> i = f.childIterator(); i.hasNext(); ) {
			final Matcher m = i.next();
			MatchBox box = new MatchBox(m.toString(),m);
			box.addItemListener(new ItemListener(){
				public void itemStateChanged(ItemEvent evt) {
					boolean isSelected = ((JCheckBox) evt.getSource()).isSelected();
					boolean wasSelected = f.isEnabled(m);
					if (isSelected == wasSelected) return;
					f.setEnabled(m, isSelected);
					FilterActivity.notifyListeners(isSelected ? FilterListener.Action.FILTERING :  FilterListener.Action.UNFILTERING, null);
					MainFrame.getInstance().updateStatusBar();
					MainFrame.getInstance().setProjectChanged(true);
	                
                }});
			box.setSelected(f.isEnabled(m));
			boxes.add(box);
		}

		filterCheckBoxList.setListData(boxes.toArray(new MatchBox[boxes.size()]));
	}

	private static class UneditableTableModel extends DefaultTableModel
	{
		public UneditableTableModel(Object[][] tableData, String[] strings) {
			super(tableData,strings);
		}
		@Override
		public boolean isCellEditable(int x, int y)
		{
			return false;
		}
	}

	private static class FilterCheckBoxListener implements ItemListener
	{
		FilterMatcher filter;

		FilterCheckBoxListener(FilterMatcher filter)
		{
			this.filter = filter;
		}

		public void itemStateChanged(ItemEvent evt)
		{
//			RebuildThreadMonitor.waitForRebuild();
			filter.setActive(((JCheckBox) evt.getSource()).isSelected());
			MainFrame.getInstance().updateStatusBar();
			MainFrame.getInstance().setProjectChanged(true);
		}	
	}


	void freeze()
	{
		frozen=true;
		filterCheckBoxList.setEnabled(false);
		addButton.setEnabled(false);
		removeButton.setEnabled(false);
	}

	void thaw()
	{
		filterCheckBoxList.setEnabled(true);
		addButton.setEnabled(true);
		removeButton.setEnabled(true);
		frozen=false;
	}

	void setSelectedTab(int index)
	{
		if(index > 0 && index <= mainTabPane.getTabCount())
			mainTabPane.setSelectedIndex(index);
	}
}
