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

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A Table model that sits between the JTable and the real model.
 * This model converts view row indexes, into sorted model row indexes.
 */
public class DefaultSortedTableModel extends AbstractTableModel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int SORT_NO_ORDER = 0;
	public static final int SORT_ASCENDING_ORDER = 1;
	public static final int SORT_DESCENDING_ORDER = 2;
	public static final int NUM_SORT_DIREECTIONS = 3;

	private AbstractTableModel baseModel;
	private List<Integer> viewToModelMapping;
	private int sortDirection = SORT_ASCENDING_ORDER;
	private int sortColumn = 0;
	private ImageIcon upIcon, downIcon;


	public DefaultSortedTableModel( AbstractTableModel model, JTableHeader header ) {
		baseModel = model;
		model.addTableModelListener(new BaseTableModelListener());

		final JTableHeader baseHeader = header;
		baseHeader.addMouseListener(new HeaderListener());
		final TableCellRenderer baseRenderer = baseHeader.getDefaultRenderer();
		baseHeader.setDefaultRenderer( new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel)baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (baseHeader.getTable().convertColumnIndexToModel(column) == sortColumn) {
					if (sortDirection != SORT_NO_ORDER) {
						label.setHorizontalTextPosition(SwingConstants.LEFT);
						label.setIcon( sortDirection == SORT_ASCENDING_ORDER ? downIcon : upIcon );
					} else {
						label.setIcon(null);
					}
				} else {
					label.setIcon(null);
				}
				return label;
			}
		});

		setupMapping();
		ClassLoader classLoader = this.getClass().getClassLoader();
		upIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/up.png"));
		downIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/down.png"));
	}

	// Base Model handling

	public TableModel getBaseTableModel() {
		return baseModel;
	}

	public int getBaseModelIndex( int viewIndex ) {
		return viewToModelMapping.get(viewIndex).intValue();
	}

	// Event handling

	@Override
	public void fireTableCellUpdated( int row, int col ) {
		if (baseModel != null)
			setupMapping();
		super.fireTableCellUpdated(row, col);
	}

	@Override
	public void fireTableChanged( TableModelEvent e ) {
		if (baseModel != null)
			setupMapping();
		super.fireTableChanged(e);
	}

	@Override
	public void fireTableDataChanged() {
		if (baseModel != null)
			setupMapping();
		super.fireTableDataChanged();
	}

	@Override
	public void fireTableRowsDeleted( int first, int last ) {
		if (baseModel != null)
			setupMapping();
		super.fireTableRowsDeleted(first,last);
	}

	@Override
	public void fireTableRowsInserted( int first, int last ) {
		if (baseModel != null)
			setupMapping();
		super.fireTableRowsInserted(first, last);
	}

	@Override
	public void fireTableRowsUpdated( int first, int last ) {
		if (baseModel != null)
			setupMapping();
		super.fireTableRowsUpdated(first, last);
	}

	@Override
	public void fireTableStructureChanged() {
		if (baseModel != null)
			setupMapping();
		super.fireTableStructureChanged();
	}

	// accessors

	@Override
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

	@Override
	public Class<?> getColumnClass( int column ) {
		if (baseModel == null)
			return null;

		return baseModel.getColumnClass(column);
	}

	@Override
	public String getColumnName( int column ) {
		if (baseModel == null)
			return null;

		return baseModel.getColumnName(column);
	}

	@Override
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

	@Override
	public void setValueAt( Object value, int row, int col ) {
		if (baseModel == null)
			return;

		baseModel.setValueAt( value, viewToModelMapping.get(row).intValue(), col );
		fireTableDataChanged();
	}

	private void setupMapping() {
		int numRows = baseModel.getRowCount();
		viewToModelMapping = new ArrayList<Integer>(numRows);
		for (int i = 0; i < numRows; i++)
			viewToModelMapping.add(i);

		Collections.sort( viewToModelMapping, new Comparator<Integer>() {
			@SuppressWarnings("unchecked")
			public int compare( Integer a, Integer b ) {
				if ((sortDirection == SORT_NO_ORDER) || (sortColumn == -1))
					return a.compareTo(b);

				Comparable<Object> first = (Comparable<Object>)baseModel.getValueAt( a.intValue(), sortColumn );
				Comparable<Object>  second = (Comparable<Object>)baseModel.getValueAt( b.intValue(), sortColumn );

				if (sortDirection == SORT_ASCENDING_ORDER) 
					return first.compareTo(second);
				else
					return second.compareTo(first);
			}
		});

	}

	private class BaseTableModelListener implements TableModelListener
	{
		public void tableChanged( TableModelEvent e ) {
			DefaultSortedTableModel.this.fireTableChanged(e);
		}
	}

	private class HeaderListener extends MouseAdapter
	{
		@Override
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
