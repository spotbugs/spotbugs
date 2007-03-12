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
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.tree.TreeModel;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Allows you to make a new Filter by right clicking (control clicking) on a bug in the tree
 */
@SuppressWarnings("serial")
public class NewFilterFromBug extends FBDialog
{
	private HashMap<JRadioButton, Sortables> map = new HashMap<JRadioButton, Sortables>();
	private JRadioButton selectedRadioButton = null;
	static List<NewFilterFromBug> listOfAllFrames=new ArrayList<NewFilterFromBug>();
	
	public NewFilterFromBug(final BugInstance bug)
	{
		this.setModal(true);
		listOfAllFrames.add(this);
		setLayout(new BorderLayout());
		add(new JLabel("Filter out all bugs whose..."), BorderLayout.NORTH);
		
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		ButtonGroup group = new ButtonGroup();
		for (Sortables s : Sortables.values())
		{
			if (s.equals(Sortables.DIVIDER))
				continue;
			JRadioButton radio = new JRadioButton(s.toString() + " is " + s.formatValue(s.getFrom(bug)));
			radio.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt)
				{
					selectedRadioButton = (JRadioButton) evt.getSource();
				}
			});
			map.put(radio, s);
			group.add(radio);
			center.add(radio);
		}
		add(center, BorderLayout.CENTER);
		
		JPanel south = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (selectedRadioButton != null)
				{
					FilterMatcher newFilter=new FilterMatcher(map.get(selectedRadioButton), map.get(selectedRadioButton).getFrom(bug));
					ArrayList<FilterMatcher> filters=ProjectSettings.getInstance().getAllFilters();
					if (!filters.contains(newFilter))
					{	
						ProjectSettings.getInstance().addFilter(newFilter);
					}
					else //if filter is already there, turn it on
					{
						filters.get(filters.indexOf(newFilter)).setActive(true);
					}
					
					PreferencesFrame.getInstance().updateFilterPanel();
					NewFilterFromBug.this.dispose();
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				NewFilterFromBug.this.dispose();
			}
		});
		south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
		south.add(Box.createHorizontalGlue());
		south.add(okButton);
		south.add(Box.createHorizontalStrut(5));
		south.add(cancelButton);
		add(south, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
	}
	
	static void closeAll()
	{
		for(NewFilterFromBug frame: listOfAllFrames)
			frame.dispose();
		listOfAllFrames.clear();
	}
}
