package edu.umd.cs.findbugs.gui2;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A list of JCheckBoxes!  How convenient!
 * 
 * Adapted from: http://www.devx.com/tips/Tip/5342
 * @author Trevor Harmon
 */
@SuppressWarnings("serial")
public class CheckBoxList extends JList
{
	private static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public CheckBoxList()
	{
		setCellRenderer(new CellRenderer());

		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int index = locationToIndex(e.getPoint());

				if (index != -1)
				{
					JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
					checkbox.setSelected(!checkbox.isSelected());
					repaint();
				}
			}
		});

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public CheckBoxList(Object[] list)
	{
		this();
		setListData(list);
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		
		for (int i = 0; i < getModel().getSize(); i++)
			((JCheckBox) getModel().getElementAt(i)).setEnabled(enabled);
	}
	
	protected class CellRenderer implements ListCellRenderer
	{
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			JCheckBox checkbox = (JCheckBox) value;
			checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
			checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			return checkbox;
		}
	}
}