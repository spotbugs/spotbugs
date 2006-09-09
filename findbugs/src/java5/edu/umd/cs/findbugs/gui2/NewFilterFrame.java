package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import edu.umd.cs.findbugs.gui2.BugAspects.StringPair;

/**
 * 
 * Lets you choose your new filter, shouldn't let you choose filters that wouldn't filter anything out
 * including filters that you already have
 *
 */
@SuppressWarnings("serial")
public class NewFilterFrame extends FBDialog
{
	
	private JList list = new JList();
	
	private static NewFilterFrame instance;
	public static void open()
	{
		if (instance == null)
		{
			instance = new NewFilterFrame();
			instance.setVisible(true);
		}
		else
		{
			instance.toFront();
		}
	}
	public static void close()
	{
		instance.dispose();
		instance = null;
	}
	
	private NewFilterFrame()
	{
		super(PreferencesFrame.getInstance());
		setContentPane(new JPanel()
		{
			public Insets getInsets()
			{
				return new Insets(3, 3, 3, 3);
			}
		});
		setLayout(new BorderLayout());
		
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
		north.add(Box.createHorizontalStrut(3));
		north.add(new JLabel("Filter out all bugs whose "));
		
		//Argh divider
		Sortables[] valuesWithoutDivider=new Sortables[Sortables.values().length-1];
		int index=0;

		for (int x=0; x<Sortables.values().length;x++)
		{
			if (Sortables.values()[x]!=Sortables.DIVIDER)
			{
				valuesWithoutDivider[index]=Sortables.values()[x];
				index++;
			}
		}
		
		final JComboBox comboBox = new JComboBox(valuesWithoutDivider);
		comboBox.setRenderer(new ListCellRenderer()
		{
			final Color SELECTED_BACKGROUND = new Color(183, 184, 204);
			
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				JLabel result = new JLabel();
				result.setFont(result.getFont().deriveFont(Driver.getFontSize()));
				result.setOpaque(true);
				result.setText(value.toString().toLowerCase());
				if (isSelected)
					result.setBackground(SELECTED_BACKGROUND);
				return result;
			}
		});
		comboBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Sortables filterBy = (Sortables) comboBox.getSelectedItem();
				String[] listData = filterBy.getAllSorted();
				for (int i = 0; i < listData.length; i++)
					listData[i] = filterBy.formatValue(listData[i]);
				list.setListData(listData);
			}
		});
		comboBox.validate();
		north.add(comboBox);
		north.add(new JLabel(" is:"));
		north.add(Box.createHorizontalGlue());
		JPanel south = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Sortables key = (Sortables) comboBox.getSelectedItem();
				String[] values = key.getAllSorted();
//				for (int i : list.getSelectedIndices())
//				{
//					FilterMatcher fm=new FilterMatcher(key,values[i]);
//					if (!ProjectSettings.getInstance().getAllMatchers().contains(fm))
//						ProjectSettings.getInstance().addFilter(fm);
//				}
				FilterMatcher[] newFilters = new FilterMatcher[list.getSelectedIndices().length];
				for (int i = 0; i < newFilters.length; i++)
					newFilters[i] = new FilterMatcher(key, values[list.getSelectedIndices()[i]]);
				ProjectSettings.getInstance().addFilters(newFilters);
				PreferencesFrame.getInstance().updateFilterPanel();
				MainFrame.getInstance().setProjectChanged(newFilters.length > 0);
				close();
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				close();
			}
		});
		south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
		south.add(Box.createHorizontalGlue());
		south.add(okButton);
		south.add(Box.createHorizontalStrut(2));
		south.add(cancelButton);
		south.add(Box.createHorizontalStrut(3));
		
		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				if (evt.getClickCount() == 2)
				{
					// Dupe from OK button's ActionListener
					Sortables key = (Sortables) comboBox.getSelectedItem();
					String[] values = key.getAllSorted();
//					for (int i : list.getSelectedIndices())
//					{
//						FilterMatcher fm=new FilterMatcher(key,values[i]);
//						if (!ProjectSettings.getInstance().getAllMatchers().contains(fm))
//							ProjectSettings.getInstance().addFilter(fm);
//					}
					FilterMatcher[] newFilters = new FilterMatcher[list.getSelectedIndices().length];
					for (int i = 0; i < newFilters.length; i++)
						newFilters[i] = new FilterMatcher(key, values[list.getSelectedIndices()[i]]);
					ProjectSettings.getInstance().addFilters(newFilters);
					PreferencesFrame.getInstance().updateFilterPanel();
					close();
				}
			}
		});
		
		add(north, BorderLayout.NORTH);
		add(Box.createHorizontalStrut(2), BorderLayout.WEST);
		add(new JScrollPane(list), BorderLayout.CENTER);
		add(Box.createHorizontalStrut(2), BorderLayout.EAST);
		add(south, BorderLayout.SOUTH);
		
		// Populate the box with initial values
		comboBox.getActionListeners()[0].actionPerformed(null);
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent arg0)
			{
				close();
			}
		});
		
		setTitle("Create New Filter");
		pack();
		setResizable(false);
	}
	
	public static void main(String[] args)
	{
		new NewFilterFrame().setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}
