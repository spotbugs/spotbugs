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
