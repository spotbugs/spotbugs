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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;

/**
 * This is the window that pops up when the user double clicks on the sorting table
 * Its also available from the menu if they remove all Sortables.  
 * 
 * The user can choose what Sortables he wants to sort by, 
 * sort them into the order he wants to see
 * and then click apply to move 
 * his choices onto the sorting table
 * @author Dan
 *
 */
public class SorterDialog extends FBDialog {

	private JLabel previewLabel=new JLabel("Preview:");
	private JTableHeader preview;
	private ArrayList<JCheckBox> checkBoxSortList = new ArrayList<JCheckBox>();
	private CheckBoxList chBList;
	JButton sortApply;
	private static SorterDialog instance;
	
	public static SorterDialog getInstance()
	{
		if (instance==null)
			instance=new SorterDialog();
		return instance;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		
		if (visible)
			((SorterTableColumnModel)(preview.getColumnModel())).createFrom(MainFrame.getInstance().getSorter());
	}
	
	private SorterDialog()
	{
		setTitle("Sort By...");
		add(createSorterPane());
		pack();
		setLocationByPlatform(true);
		setResizable(false);
		preview.setColumnModel(new SorterTableColumnModel(MainFrame.getInstance().getSorter().getOrder()));
	}
	/**
	 * Creates JPanel with checkboxes of different things to
	 * sort by. List is: priority, class, package, category,
	 * bugcode, status, and type.
	 * @return
	 */
	private JPanel createSorterPane() {
		JPanel sorter = new JPanel();
		JPanel insidePanel = new JPanel();		
		insidePanel.setLayout(new BorderLayout());
		sorter.setLayout(new BorderLayout());
		preview=new JTableHeader();
		preview.setColumnModel(new SorterTableColumnModel(Sortables.values()));
		
		Sortables[] sortList = Sortables.values();
		
		for(Sortables s : Sortables.values()){
			if (s == Sortables.DIVIDER)
				checkBoxSortList.add(new JCheckBox(L10N.getLocalString("sort.divider", "[divider]")));
			else
				checkBoxSortList.add(new JCheckBox(s.toString()));
		}
		
		setSorterCheckBoxes();
		
		for(int i = 0; i < sortList.length; i++){
			checkBoxSortList.get(i).addChangeListener(new CheckBoxChangedListener(i));
		}		
		
		chBList = new CheckBoxList(checkBoxSortList.toArray(
				new JCheckBox[checkBoxSortList.size()]));
				
		insidePanel.add(chBList, BorderLayout.NORTH);
		
		//insidePanel.add(sorterInfoLabel(), BorderLayout.CENTER);
		
		
		JPanel bottomPanel=new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		//bottomPanel.add(previewLabel,BorderLayout.NORTH);
		bottomPanel.add(preview, BorderLayout.CENTER);

		insidePanel.add(bottomPanel,BorderLayout.SOUTH);
		
		
		sortApply=new JButton(L10N.getLocalString("dlg.apply_btn", "Apply"));
		sortApply.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				MainFrame.getInstance().getSorter().createFrom((SorterTableColumnModel)preview.getColumnModel());
				((BugTreeModel)MainFrame.getInstance().getTree().getModel()).checkSorter();
				instance.setVisible(false); //close window
			}
		});
		bottomPanel.add(sortApply,BorderLayout.SOUTH);
		sorter.add(new JScrollPane(insidePanel), BorderLayout.CENTER);
		
		return sorter;
	}
	
	private class CheckBoxChangedListener implements ChangeListener{

		int indexOfCheckBox;
		
		public CheckBoxChangedListener(int index){
			indexOfCheckBox = index;
		}
		
		public void stateChanged(ChangeEvent e) {
				((SorterTableColumnModel)preview.getColumnModel()).setIndexChanged(indexOfCheckBox);
		}
	}	
	
	/**
	 * Sets the checkboxes in the sorter panel to what is shown in 
	 * the MainFrame. This assumes that sorterTableColumnModel will 
	 * return the list of which box is checked in the same order as
	 * the order that sorter panel has.
	 */
	private void setSorterCheckBoxes() {
		boolean[] chBoxSorterBooleans = MainFrame.getInstance().getSorter().getVisibleColumns();
		if(chBoxSorterBooleans.length != checkBoxSortList.size())
			return;
		
		for(int i = 0; i < checkBoxSortList.size(); i++){
			checkBoxSortList.get(i).setSelected(chBoxSorterBooleans[i]);
		}
	}

	void freeze()
	{
		sortApply.setEnabled(false);
	}
	
	void thaw()
	{
		sortApply.setEnabled(true);
	}
}
