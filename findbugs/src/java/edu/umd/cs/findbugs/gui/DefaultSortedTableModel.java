/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius
 * Copyright (C) 2004,2005 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA	 02111-1307	 USA
 */


package edu.umd.cs.findbugs.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

/**
 * A Table model that sits between the JTable and the real model.
 * This model converts view row indexes, into sorted model row indexes.
 */
public class DefaultSortedTableModel extends AbstractTableModel
{
	public static final int SORT_NO_ORDER = 0;
	public static final int SORT_ASCENDING_ORDER = 1;
	public static final int SORT_DESCENDING_ORDER = 2;
	public static final int NUM_SORT_DIREECTIONS = 3;
	
	private AbstractTableModel baseModel;
	private JTableHeader baseHeader = null;
	private MouseListener headerListener = new HeaderListener();
	private List<Integer> viewToModelMapping;
	private int sortDirection = SORT_ASCENDING_ORDER;
	private int sortColumn = 0;
	
	
	public DefaultSortedTableModel( AbstractTableModel model ) {
		baseModel = model;
		setupMapping();
	}
	
	// Base Model handling
	
	public TableModel getBaseTableModel() {
		return baseModel;
	}
	
	public int getBaseModelIndex( int viewIndex ) {
		return viewToModelMapping.get(viewIndex).intValue();
	}
	
	public void setBaseTableHeader( JTableHeader header ) {
		if (baseHeader != null)
			baseHeader.removeMouseListener(headerListener);
		baseHeader = header;
		baseHeader.addMouseListener(headerListener);	
	}
	
	// Listener handling
	
	public void addTableModelListener( TableModelListener tml ) {
		if (baseModel != null)
			return;
			
		baseModel.addTableModelListener(tml);
	}
	
	public void removeTableModelListener( TableModelListener tml ) {
		if (baseModel != null)
			return;
			
		baseModel.removeTableModelListener(tml);
	}
	
	public TableModelListener[] getTableModelListeners() {
		if (baseModel == null)
			return new TableModelListener[0];
			
		return baseModel.getTableModelListeners();
	}
	
	// Event handling
	
	public void fireTableCellUpdated( int row, int col ) {
		if (baseModel == null)
			return;
			
		baseModel.fireTableCellUpdated(row, col);
		setupMapping();
	}

	public void fireTableChanged( TableModelEvent e ) {
		if (baseModel == null)
			return;
			
		baseModel.fireTableChanged(e);
		setupMapping();
	}

	public void fireTableDataChanged() {
		if (baseModel == null)
			return;
			
		baseModel.fireTableDataChanged();
		setupMapping();
	}
	
	public void fireTableRowsDeleted( int first, int last ) {
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsDeleted(first, last);
		setupMapping();
	}

	public void fireTableRowsInserted( int first, int last ) {
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsInserted(first, last);
		setupMapping();
	}

	public void fireTableRowsUpdated( int first, int last ) {
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsUpdated(first, last);
		setupMapping();
	}

	public void fireTableStructureChanged() {
		if (baseModel == null)
			return;
			
		baseModel.fireTableStructureChanged();
		setupMapping();
	}
	
	// accessors
	
	public int findColumn( String columnName ) {
		if (baseModel == null)
			return -1;
	
		return baseModel.findColumn(columnName);	
	}
	
	public int getColumnCount() {
		if (baseModel == null)
			return 0;
			
		return baseModel.getColumnCount();
	}
	
	public int getRowCount() {
		if (baseModel == null)
			return 0;
			
		return baseModel.getRowCount();
	}
	
	public Class getColumnClass( int column ) {
		if (baseModel == null)
			return null;
			
		return baseModel.getColumnClass(column);
	}
	
	public String getColumnName( int column ) {
		if (baseModel == null)
			return null;
			
		return baseModel.getColumnName(column);
	}
	
	public boolean isCellEditable( int row, int col ) {
		if (baseModel == null)
			return false;
			
		return baseModel.isCellEditable( row, col );
	}
	
	public Object getValueAt( int row, int col ) {
		if (baseModel == null)
			return null;
			
		return baseModel.getValueAt(viewToModelMapping.get(row).intValue(), col);
	}
	
	public void setValueAt( Object value, int row, int col ) {
		if (baseModel == null)
			return;
			
		baseModel.setValueAt( value, viewToModelMapping.get(row).intValue(), col );
	}
	
	private void setupMapping() {
		int numRows = baseModel.getRowCount();
		viewToModelMapping = new ArrayList(numRows);
		for (int i = 0; i < numRows; i++)
			viewToModelMapping.add(new Integer(i));
		
		Collections.sort( viewToModelMapping, new Comparator<Integer>() {
			public int compare( Integer a, Integer b ) {
				if ((sortDirection == SORT_NO_ORDER) || (sortColumn == -1))
					return a.compareTo(b);
				
				Comparable first = (Comparable)baseModel.getValueAt( a.intValue(), sortColumn );
				Comparable second = (Comparable)baseModel.getValueAt( b.intValue(), sortColumn );
				
				if (sortDirection == SORT_ASCENDING_ORDER) 
					return first.compareTo(second);
				else
					return second.compareTo(first);
			}
		});
		
	}
	
	private class HeaderListener extends MouseAdapter
	{
		public void mouseClicked(MouseEvent e) {
			JTableHeader header = (JTableHeader)e.getSource();
			int column = header.columnAtPoint(e.getPoint());
			column = header.getTable().convertColumnIndexToModel(column);
			if (column != sortColumn) {
				sortColumn = column;
				sortDirection = SORT_ASCENDING_ORDER;
			} else {
				sortDirection = (sortDirection + 1) % NUM_SORT_DIREECTIONS;
			}
			super.mouseClicked(e);
			DefaultSortedTableModel.this.fireTableDataChanged();
		}
	}
}