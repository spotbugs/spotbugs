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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * This is the properties dialog of the GUI. It allows the user to set the 
 * size of the tabs and font size. If the user changes the font size they
 * will be told to restart the computer before the new size takes affect.
 * @author Kristin Stephens
 */
public class PropertiesDialog extends FBDialog {
	private static PropertiesDialog instance;
	private JTextField tabTextField;
	private JTextField fontTextField;
	
	public static PropertiesDialog getInstance()
	{
		if(instance == null)
			instance = new PropertiesDialog();
		return instance;
	}
	
	private PropertiesDialog()
	{
		JPanel contentPanel = new JPanel(new BorderLayout());
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(2,2));
		mainPanel.add(new JLabel("Tab Size"));
		tabTextField = new JTextField(Integer.toString(GUISaveState.getInstance().getTabSize()));
		mainPanel.add(tabTextField);
		
		mainPanel.add(new JLabel("Font Size"));
		fontTextField = new JTextField(Float.toString(GUISaveState.getInstance().getFontSize()));
		mainPanel.add(fontTextField);
		
		contentPanel.add(mainPanel, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JButton(new AbstractAction("Apply")
		{
			public void actionPerformed(ActionEvent evt)
			{
				if(Integer.decode(tabTextField.getText()).intValue() != GUISaveState.getInstance().getTabSize()){
					GUISaveState.getInstance().setTabSize(Integer.decode(tabTextField.getText()).intValue());
					MainFrame.getInstance().displayer.clearCache();
					MainFrame.getInstance().syncBugInformation(); //This causes the GUI to redisplay the current code
				}
				
				if(Float.parseFloat(fontTextField.getText()) != GUISaveState.getInstance().getFontSize()){
					GUISaveState.getInstance().setFontSize(Float.parseFloat(fontTextField.getText()));
					JOptionPane.showMessageDialog(PropertiesDialog.getInstance(),
							"To implement the new font size, please restart FindBugs.",
							"Changing Font", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}));
		
		bottomPanel.add(new JButton(new AbstractAction("Reset")
		{
			public void actionPerformed(ActionEvent evt)
			{
				tabTextField.setText(Integer.toString(GUISaveState.getInstance().getTabSize()));
				fontTextField.setText(Float.toString(GUISaveState.getInstance().getFontSize()));
			}
		}));
		
		contentPanel.add(bottomPanel, BorderLayout.SOUTH);
		setContentPane(contentPanel);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setModal(true);
		pack();
		
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(WindowEvent e) {
				if(Integer.decode(tabTextField.getText()).intValue() != GUISaveState.getInstance().getTabSize())
					tabTextField.setText(Integer.toString(GUISaveState.getInstance().getTabSize()));
				
				if(Float.parseFloat(fontTextField.getText()) != GUISaveState.getInstance().getFontSize()){
					fontTextField.setText(Float.toString(GUISaveState.getInstance().getFontSize()));
				}
			}
		});
	}
}
