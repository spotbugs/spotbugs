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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;

/**
 * Handles the sorting order and informs the treeModel when changes are necessary
 * @author Dan
 *
 */
public class SorterTableColumnModel implements TableColumnModel{

	private ArrayList<Sortables> order=new ArrayList<Sortables>();
	private boolean[] showOrder=new boolean[Sortables.values().length];
	private ArrayList<TableColumn> columnList=new ArrayList<TableColumn>();
	private DefaultListSelectionModel dlsm;
	private ArrayList<TableColumnModelListener> watchers=new ArrayList<TableColumnModelListener>();
	private boolean frozen=false;

	public SorterTableColumnModel(Sortables[] columnHeaders){

		for(int x = 0; x < columnHeaders.length; x++)
		{
			Sortables c=columnHeaders[x];
			//System.out.println(c);
			for (int y=0; y<Sortables.values().length;y++)
			{
				if (c.equals(Sortables.values()[y])) 
					showOrder[y]=true;
			}

			TableColumn tc=new TableColumn(x);
			FBTableCellRenderer temp = new FBTableCellRenderer(); 
			tc.setHeaderRenderer(temp);
			tc.setIdentifier(c);
			tc.setHeaderValue(c);
			tc.setResizable(false);
			tc.sizeWidthToFit();
			columnList.add(tc);
		}
		dlsm=new DefaultListSelectionModel();
		dlsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		orderUpdate();
	}

	public void createFrom(SorterTableColumnModel other)
	{
		if (this.getOrder().equals(other.getOrder()))
			return;
		columnList.clear();
		for (int x=0; x<order.size(); x++)
		{
			for (TableColumnModelListener l: watchers)
				l.columnRemoved(new TableColumnModelEvent(this,x,x));
		}

		//First, empty showOrder
		for(int x=0; x<showOrder.length;x++)
			showOrder[x]=false; 

		for(int x = 0; x < other.order.size(); x++)
		{
			Sortables c=other.order.get(x);
			for (int y=0; y<Sortables.values().length;y++)
			{
				if (c.equals(Sortables.values()[y])) 
					showOrder[y]=true;//Then refill it, this allows sorterDialog to keep track of whats open
			}

			TableColumn tc=new TableColumn(x);
			tc.setHeaderRenderer(new FBTableCellRenderer());
			tc.setIdentifier(c);
			tc.setHeaderValue(c);
			tc.setResizable(false);
			tc.sizeWidthToFit();
			columnList.add(tc);
			for (TableColumnModelListener l: watchers)
				l.columnAdded(new TableColumnModelEvent(this,x,x));
		}
		dlsm=new DefaultListSelectionModel();
		dlsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		orderUpdate();

	}

	public SorterTableColumnModel(ArrayList<Sortables> columnHeaders)
	{
		this(columnHeaders.toArray(new Sortables[columnHeaders.size()]));
	}


	static class FBTableCellRenderer implements TableCellRenderer {

		private TableCellRenderer defaultRenderer = new JTableHeader().getDefaultRenderer();

		public Component getTableCellRendererComponent(JTable table, 
				Object value, boolean isSelected, boolean hasFocus, int row, int column){

			Component comp = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (comp instanceof JLabel) {
				JLabel cell = (JLabel)comp;
				cell.setFont(cell.getFont().deriveFont(Driver.getFontSize()));
				cell.setFont(cell.getFont().deriveFont(Font.BOLD));
				cell.setBorder(BorderFactory.createCompoundBorder(cell.getBorder(), BorderFactory.createEmptyBorder(0, 6, 0, 6)));
				cell.setHorizontalAlignment(SwingConstants.CENTER);
				if (value == Sortables.DIVIDER)
				{
					cell.setText("");
					cell.setIcon(new ImageIcon(MainFrame.class.getResource("arrows.png")));
					new ImageIcon("a");
				}
			}
			return comp;
		}
	}

	public void addColumn(TableColumn arg0) 
	{
		throw new UnsupportedOperationException("Can't change sorter table columns using addColumn");
	}

	public void removeColumn(TableColumn arg0) 
	{
		throw new UnsupportedOperationException("Can't change sorter table columns using removeColumn");
	}

	boolean[] getVisibleColumns()
	{
		return showOrder;
	}

	void setIndexChanged(int index)
	{
		showOrder[index]=!showOrder[index];
		Sortables s=Sortables.values()[index];

		boolean on=showOrder[index];

		if (on)
		{
			TableColumn tc=new TableColumn(columnList.size());
			tc.setHeaderRenderer(new FBTableCellRenderer());
			tc.setIdentifier(s);
			tc.setHeaderValue(s);
			tc.setResizable(false);
			tc.sizeWidthToFit();
			columnList.add(tc);
			for (int x=0;x<columnList.size();x++)
			{
				columnList.get(x).setModelIndex(x);
			}
			orderUpdate();
			for (TableColumnModelListener l: watchers)
			{
				l.columnAdded(new TableColumnModelEvent(this,columnList.size()-1,columnList.size()-1));			
			}
		}
		else if (!on)
		{
			for (int x=0;x<columnList.size();x++)
			{
				columnList.get(x).setModelIndex(x);
			}

			for (int counter=0;counter<columnList.size();counter++)
			{
				TableColumn tc=columnList.get(counter);
				if (tc.getIdentifier().equals(s))
				{
					columnList.remove(counter);
					for (int x=counter;x<columnList.size();x++)
					{
						columnList.get(x).setModelIndex(x);
					}

					orderUpdate();
					for (TableColumnModelListener l: watchers)
						l.columnRemoved(new TableColumnModelEvent(this,counter,counter));

				}
			}
		}

	}

	public void moveColumn(int fromIndex, int toIndex) {

		MainFrame.getInstance().updateDesignationDisplay();
		MainFrame.getInstance().saveComments();
		TableColumn from=columnList.get(fromIndex);
		TableColumn to=columnList.get(toIndex);

		columnList.set(fromIndex,to);
		to.setModelIndex(fromIndex);

		columnList.set(toIndex,from);		
		from.setModelIndex(toIndex);

		orderUpdate();

		for (TableColumnModelListener w:(ArrayList<TableColumnModelListener>)watchers.clone())
		{
			w.columnMoved(new TableColumnModelEvent(this,fromIndex,toIndex));
		}
	}

	public void setColumnMargin(int arg0) {
		throw new UnsupportedOperationException("NoBah");
	}

	public int getColumnCount() {
		return columnList.size();
	}

	public Enumeration<TableColumn> getColumns() {
		return Collections.enumeration(columnList);
	}

	public int getColumnIndex(Object columnIdentifier) {

		if (columnIdentifier==null)
			throw new IllegalArgumentException("Dont send null to getColumnIndex, null shouldn't be in the sorting table.");

		for(int x=0;x<columnList.size();x++)
		{	
			if (columnList.get(x).getIdentifier().equals(columnIdentifier))
				return x;
		}

		throw new IllegalArgumentException();		
	}

	public TableColumn getColumn(int x) {
		return columnList.get(x);
	}

	public int getColumnMargin() {
		return 0;
	}

	public int getColumnIndexAtX(int XPosition) {

		for (TableColumn tc:columnList)
		{
			XPosition-=tc.getWidth();
			if (XPosition < 0)
				return tc.getModelIndex();
		}
		return -1;
	}

	public int getTotalColumnWidth() {
		int total=0;
		for (TableColumn tc: columnList)
		{
			total+=tc.getWidth();
		}
		return total;
	}

	public void setColumnSelectionAllowed(boolean arg0) {
		throw new UnsupportedOperationException("BAH");//BAH
	}

	public boolean getColumnSelectionAllowed() {
		return true;
	}

	public int[] getSelectedColumns() {
		int index=dlsm.getMinSelectionIndex();
		if (index==-1)
			return new int[]{};
		return new int[]{index};
	}

	public int getSelectedColumnCount() {

		if (dlsm.getMinSelectionIndex()==-1)
			return 0;
		return 1;
	}

	public void setSelectionModel(ListSelectionModel arg0) {
		throw new UnsupportedOperationException("No... NO NO NO NO");
	}

	public ListSelectionModel getSelectionModel() {
		return dlsm;
	}

	public void addColumnModelListener(TableColumnModelListener listener) {
		watchers.add(listener);
	}

	public void removeColumnModelListener(TableColumnModelListener listener) {
		watchers.remove(listener);
	}

	public void columnSelectionChanged(ListSelectionEvent arg0) {
		throw new UnsupportedOperationException("columnSelectionChangedBAH");
	}

	ArrayList<Sortables> getOrder()
	{
		return order;
	}

	List<Sortables> getOrderBeforeDivider()
	{
		if (!order.contains(Sortables.DIVIDER))
			return order;

		return order.subList(0, order.indexOf(Sortables.DIVIDER));
	}

	List<Sortables> getOrderAfterDivider()
	{
		if (!order.contains(Sortables.DIVIDER) || order.indexOf(Sortables.DIVIDER) == order.size() - 1)
			return new ArrayList<Sortables>();

		return order.subList(order.indexOf(Sortables.DIVIDER) + 1, order.size());
	}

	private void orderUpdate()
	{
		//order.clear();
		if (!frozen)
		{
			order=new ArrayList<Sortables>();
			for (int x=0;x<columnList.size();x++)
			{
				order.add((Sortables)columnList.get(x).getIdentifier());
			}
		}
	}

	public void freezeOrder() {
		frozen=true;
	}

	public void thawOrder() {
		frozen=false;
		orderUpdate();
		TreeModel model=MainFrame.getInstance().getTree().getModel();
		if (model instanceof BugTreeModel)
		{
			((BugTreeModel) model).checkSorter();
		}
	}
}
