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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.junit.runner.manipulation.Sortable;

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

	private JTableHeader preview;
	private ArrayList<SortableCheckBox> checkBoxSortList = new ArrayList<SortableCheckBox>();
	JButton sortApply;
	
	public static SorterDialog getInstance()
	{
	 return new SorterDialog();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);

		if (visible){
			((SorterTableColumnModel)(preview.getColumnModel())).createFrom(MainFrame.getInstance().getSorter());
			setSorterCheckBoxes();
		}
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
	
	 class SortableCheckBox extends JCheckBox {
		final Sortables sortable;
		SortableCheckBox(Sortables s) {
			super(s == Sortables.DIVIDER ? edu.umd.cs.findbugs.L10N.getLocalString("sort.divider", "[divider]") : s.toString());
			this.sortable = s;
			addChangeListener(new ChangeListener() { 
				public void stateChanged(ChangeEvent e) {
						((SorterTableColumnModel)preview.getColumnModel()).setVisible(sortable, isSelected());
				}
			})	;
		}
		
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
		Sortables[] sortables = MainFrame.getInstance().getAvailableSortables();
		preview.setColumnModel(new SorterTableColumnModel(sortables));

		
		for(Sortables s : sortables){
			checkBoxSortList.add(new SortableCheckBox(s));
		}

		setSorterCheckBoxes();

	

		insidePanel.add( new CheckBoxList(checkBoxSortList.toArray(
				new JCheckBox[checkBoxSortList.size()])), BorderLayout.NORTH);

		//insidePanel.add(sorterInfoLabel(), BorderLayout.CENTER);


		JPanel bottomPanel=new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		//bottomPanel.add(previewLabel,BorderLayout.NORTH);
		//New code to fix problem in Windows
		JTable t = new JTable(new DefaultTableModel(0, sortables.length));
		t.setTableHeader(preview);
		JScrollPane sp = new JScrollPane(t);
		//This sets the height of the scrollpane so it is dependent on the fontsize.
		int num = (int) (Driver.getFontSize()*1.2);
		sp.setPreferredSize(new Dimension(670, 10+num));
		//End of new code.
		//Changed code.
		bottomPanel.add(sp, BorderLayout.CENTER);
		//bottomPanel.add(preview, BorderLayout.CENTER);
		//End of changed code.

		insidePanel.add(bottomPanel,BorderLayout.SOUTH);


		sortApply=new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.apply_btn", "Apply"));
		sortApply.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				MainFrame.getInstance().getSorter().createFrom((SorterTableColumnModel)preview.getColumnModel());
				((BugTreeModel)MainFrame.getInstance().getTree().getModel()).checkSorter();
				SorterDialog.this.dispose();
			}
		});
		bottomPanel.add(sortApply,BorderLayout.SOUTH);
		sorter.add(new JScrollPane(insidePanel), BorderLayout.CENTER);

		return sorter;
	}


	/**
	 * Sets the checkboxes in the sorter panel to what is shown in 
	 * the MainFrame. This assumes that sorterTableColumnModel will 
	 * return the list of which box is checked in the same order as
	 * the order that sorter panel has.
	 */
	private void setSorterCheckBoxes() {
		
		SorterTableColumnModel sorter = MainFrame.getInstance().getSorter();
		
		for(SortableCheckBox c : checkBoxSortList)
			c.setSelected(sorter.isShown(c.sortable));
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
