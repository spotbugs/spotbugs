/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius
 * Copyright (C) 2003,2004 University of Maryland
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


package edu.umd.cs.findbugs.gui;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

/**
 * This Table Model is a currently a noop (passthru) table model.
 * This will be embellished to build a white box sortable table model.
 * In this way, the base JTables that use it, need not change.
 */
public class DefaultSortedTableModel extends AbstractTableModel
{
	AbstractTableModel baseModel;
	JTableHeader baseHeader;
	
	public DefaultSortedTableModel( AbstractTableModel model )
	{
		baseModel = model;
	}
	
	// Base Model handling
	
	public TableModel getBaseTableModel()
	{
		return baseModel;
	}
	
	public int getBaseModelIndex( int viewIndex )
	{
		return viewIndex;
	}
	
	public void setBaseTableHeader( JTableHeader header )
	{
		baseHeader = header;
	}
	
	// Listener handling
	
	public void addTableModelListener( TableModelListener tml )
	{
		if (baseModel != null)
			return;
			
		baseModel.addTableModelListener(tml);
	}
	
	public void removeTableModelListener( TableModelListener tml )
	{
		if (baseModel != null)
			return;
			
		baseModel.removeTableModelListener(tml);
	}
	
	public TableModelListener[] getTableModelListeners()
	{
		if (baseModel == null)
			return new TableModelListener[0];
			
		return baseModel.getTableModelListeners();
	}
	
	// Event handling
	
	public void fireTableCellUpdated( int row, int col )
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableCellUpdated(row, col);
	}

	public void fireTableChanged( TableModelEvent e )
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableChanged(e);
	}

	public void fireTableDataChanged()
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableDataChanged();
	}
	
	public void fireTableRowsDeleted( int first, int last )
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsDeleted(first, last);
	}

	public void fireTableRowsInserted( int first, int last )
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsInserted(first, last);
	}

	public void fireTableRowsUpdated( int first, int last )
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableRowsUpdated(first, last);
	}

	public void fireTableStructureChanged()
	{
		if (baseModel == null)
			return;
			
		baseModel.fireTableStructureChanged();
	}
	
	// accessors
	
	public int findColumn( String columnName )
	{
		if (baseModel == null)
			return -1;
	
		return baseModel.findColumn(columnName);	
	}
	
	public int getColumnCount()
	{
		if (baseModel == null)
			return 0;
			
		return baseModel.getColumnCount();
	}
	
	public int getRowCount()
	{
		if (baseModel == null)
			return 0;
			
		return baseModel.getRowCount();
	}
	
	public Class getColumnClass( int column )
	{
		if (baseModel == null)
			return null;
			
		return baseModel.getColumnClass(column);
	}
	
	public String getColumnName( int column )
	{
		if (baseModel == null)
			return null;
			
		return baseModel.getColumnName(column);
	}
	
	public boolean isCellEditable( int row, int col )
	{
		if (baseModel == null)
			return false;
			
		return baseModel.isCellEditable( row, col );
	}
	
	public Object getValueAt( int row, int col )
	{
		if (baseModel == null)
			return null;
			
		return baseModel.getValueAt(row, col);
	}
	
	public void setValueAt( Object value, int row, int col )
	{
		if (baseModel == null)
			return;
			
		baseModel.setValueAt( value, row, col );
	}	
}