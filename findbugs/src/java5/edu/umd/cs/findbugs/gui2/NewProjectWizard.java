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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import edu.umd.cs.findbugs.Project;

/**
 * The User Interface for creating a Project and editing it after the fact.  
 * @author Reuven
 *
 */
@SuppressWarnings("serial")
public class NewProjectWizard extends FBDialog
{
	private EmptyBorder border = new EmptyBorder(5, 5, 5, 5);
	
	private Project project;
	private boolean projectChanged = false;
	private JTextField directory = new JTextField();
	private JButton directoryBrowse = new JButton("Browse...");
	private FBFileChooser chooser = new FBFileChooser();
	private FileFilter directoryOrJar = new FileFilter()
	{

		@Override
		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(".jar") || f.getName().endsWith(".zip");
		}

		@Override
		public String getDescription()
		{
			return "JAR or ZIP files (*.jar, *.zip)";
		}
	};
	
//	private JList classList = new JList();
	
	private JList auxList = new JList();
	private DefaultListModel auxModel = new DefaultListModel();
	private JButton auxAdd = new JButton("Add...");
	private JButton auxRemove = new JButton("Remove");
	
	private JList sourceList = new JList();
	private DefaultListModel sourceModel = new DefaultListModel();
	private JButton sourceAdd = new JButton("Add...");
	private JButton sourceRemove = new JButton("Remove");
	
	private JButton backButton = new JButton("< Back");
	private JButton nextButton = new JButton("Next >");
	private JButton finishButton = new JButton("Finish");
	private JButton cancelButton = new JButton("Cancel");
	
//	private JPanel[] wizardPanels = new JPanel[4];
	private JPanel[] wizardPanels = new JPanel[3];
	private int currentPanel;
	
	public NewProjectWizard()
	{
		this(null);
	}
	
	/**
	 * @param curProject the project to populate from, or null to start a new one
	 */
	public NewProjectWizard(Project curProject)
	{
		project = curProject;
		
		setLayout(new BorderLayout());
		
		wizardPanels[0] = new JPanel(new BorderLayout());
		JPanel north0 = new JPanel();
		north0.setLayout(new BoxLayout(north0, BoxLayout.Y_AXIS));
		north0.add(Box.createVerticalStrut(20));
		north0.add(new JLabel("Main project directory or JAR:"));
		north0.add(Box.createVerticalStrut(5));
		north0.add(directory);
		directory.getDocument().addDocumentListener(new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e) {}
			public void insertUpdate(DocumentEvent e)
			{
				projectChanged = true;
			}

			public void removeUpdate(DocumentEvent e)
			{
				projectChanged = true;
			}
		});
		JPanel glueAndBrowseButton = new JPanel();
		glueAndBrowseButton.setLayout(new BoxLayout(glueAndBrowseButton, BoxLayout.X_AXIS));
		glueAndBrowseButton.add(Box.createHorizontalGlue());
		glueAndBrowseButton.add(directoryBrowse);
		directoryBrowse.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				chooser.setFileFilter(directoryOrJar);
				if (chooser.showOpenDialog(NewProjectWizard.this) == JFileChooser.APPROVE_OPTION)
					directory.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		north0.add(glueAndBrowseButton);
		north0.add(Box.createVerticalGlue());
		wizardPanels[0].add(north0, BorderLayout.NORTH);
		wizardPanels[0].setBorder(border);
		
//		wizardPanels[1] = new JPanel(new BorderLayout());
//		wizardPanels[1].add(new JLabel("Classes found:"), BorderLayout.NORTH);
//		wizardPanels[1].add(new JScrollPane(classList), BorderLayout.CENTER);
//		wizardPanels[1].setBorder(border);
		
//		wizardPanels[2] = new JPanel(new GridBagLayout());
		wizardPanels[1] = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
//		wizardPanels[2].add(new JLabel("Auxiliary class locations:"), gbc);
		wizardPanels[1].add(new JLabel("Auxiliary class locations:"), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
//		wizardPanels[2].add(new JScrollPane(auxList), gbc);
		wizardPanels[1].add(new JScrollPane(auxList), gbc);
		auxList.setModel(auxModel);
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
//		wizardPanels[2].add(auxAdd, gbc);
		wizardPanels[1].add(auxAdd, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(5, 0, 0, 0);
//		wizardPanels[2].add(auxRemove, gbc);
		wizardPanels[1].add(auxRemove, gbc);
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.insets = new Insets(0, 0, 0, 0);
//		wizardPanels[2].add(Box.createGlue(), gbc);
//		wizardPanels[2].setBorder(border);
		wizardPanels[1].add(Box.createGlue(), gbc);
		wizardPanels[1].setBorder(border);
		auxAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				chooser.setFileFilter(directoryOrJar);
				if (chooser.showOpenDialog(NewProjectWizard.this) == JFileChooser.APPROVE_OPTION)
				{
					auxModel.addElement(chooser.getSelectedFile().getAbsolutePath());
					projectChanged = true;
				}
			}
		});
		auxRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (auxList.getSelectedValues().length > 0)
					projectChanged = true;
				for (Object i : auxList.getSelectedValues())
					auxModel.removeElement(i);
			}
		});
		
//		wizardPanels[3] = new JPanel(new GridBagLayout());
		wizardPanels[2] = new JPanel(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
//		wizardPanels[3].add(new JLabel("Source directories:"), gbc);
		wizardPanels[2].add(new JLabel("Source directories:"), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
//		wizardPanels[3].add(new JScrollPane(sourceList), gbc);
		wizardPanels[2].add(new JScrollPane(sourceList), gbc);
		sourceList.setModel(sourceModel);
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
//		wizardPanels[3].add(sourceAdd, gbc);
		wizardPanels[2].add(sourceAdd, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(5, 0, 0, 0);
//		wizardPanels[3].add(sourceRemove, gbc);
		wizardPanels[2].add(sourceRemove, gbc);
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.insets = new Insets(0, 0, 0, 0);
//		wizardPanels[3].add(Box.createGlue(), gbc);
//		wizardPanels[3].setBorder(border);
		wizardPanels[2].add(Box.createGlue(), gbc);
		wizardPanels[2].setBorder(border);
		sourceAdd.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setFileFilter(null);
				if (chooser.showOpenDialog(NewProjectWizard.this) == JFileChooser.APPROVE_OPTION)
				{
					sourceModel.addElement(chooser.getSelectedFile().getAbsolutePath());
					projectChanged = true;
				}
			}
		});
		sourceRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (sourceList.getSelectedValues().length > 0)
					projectChanged = true;
				for (Object i : sourceList.getSelectedValues())
					sourceModel.removeElement(i);
			}
		});
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(backButton);
		buttons.add(nextButton);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(finishButton);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(cancelButton);
		
		backButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				loadPanel(currentPanel - 1);
			}
		});
		
		nextButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				switch (currentPanel)
				{
					case 0:
						File f = new File(directory.getText());
						if (!f.exists() || (!f.isDirectory() && !f.getName().endsWith(".jar")))
						{
							JOptionPane.showMessageDialog(NewProjectWizard.this, "Invalid path.", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						break;
					case 1:
						for (int i = 0; i < auxModel.getSize(); i++){
							File temp = new File((String)auxModel.get(i));
							if(!(temp.exists() && (temp.isDirectory() || 
									temp.getName().endsWith(".jar") || temp.getName().endsWith(".zip")))){
								JOptionPane.showMessageDialog(NewProjectWizard.this, 
										temp.getName()+" is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						break;
					case 2:
						// no need to check for this.
						break;
				}
				
				loadPanel(currentPanel + 1);
			}
		});
		finishButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				File f = new File(directory.getText());
				if (!f.exists() || (!f.isDirectory() && !f.getName().endsWith(".jar")))
				{
					JOptionPane.showMessageDialog(NewProjectWizard.this, "Invalid path.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				for (int i = 0; i < auxModel.getSize(); i++){
					File temp = new File((String)auxModel.get(i));
					if(!(temp.exists() && (temp.isDirectory() || 
							temp.getName().endsWith(".jar") || temp.getName().endsWith(".zip")))){
						JOptionPane.showMessageDialog(NewProjectWizard.this, 
								temp.getName()+" is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				Project p = (project == null ? new Project() : project);
				p.addFile(directory.getText());
				for (int i = 0; i < auxModel.getSize(); i++)
					p.addAuxClasspathEntry((String) auxModel.get(i));
				for (int i = 0; i < sourceModel.getSize(); i++)
					p.addSourceDir((String) sourceModel.get(i));
				
				if (project == null || (projectChanged && JOptionPane.showConfirmDialog(NewProjectWizard.this, "Project settings have been changed.  Perform a new analysis with the changed files?", "Redo analysis?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION))
					new AnalyzingDialog(p);
				
				dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				dispose();
			}
		});
		
		JPanel south = new JPanel(new BorderLayout());
		south.add(new JSeparator(), BorderLayout.NORTH);
		south.add(buttons, BorderLayout.EAST);
		
		if (curProject != null)
		{
			if (curProject.getFileCount() > 0)
				directory.setText(curProject.getFile(0));
			for (String i : curProject.getAuxClasspathEntryList())
				auxModel.addElement(i);
			for (String i : curProject.getSourceDirList())
				sourceModel.addElement(i);
		}
		
		loadPanel(0);
		
		add(south, BorderLayout.SOUTH);		
		//setSize(new Dimension(createWidth(), 300));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
//		pack();
		setModal(true);
		setVisible(true);
	}
	
	private void loadPanel(final int index)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				remove(wizardPanels[currentPanel]);
				currentPanel = index;
				add(wizardPanels[index], BorderLayout.CENTER);
				backButton.setEnabled(index > 0);
				nextButton.setEnabled(index < wizardPanels.length - 1);
				validate();
				repaint();
			}
		});
	}
	
	public void addNotify(){
		super.addNotify();
		
		for(JPanel panel : wizardPanels){
			setFontSizeHelper(panel.getComponents(), Driver.getFontSize());
		}
		
		pack();
		
		int width = super.getWidth();
		
		if(width < 400)
			width = 400;
		setSize(new Dimension(width, 300));
		setLocationRelativeTo(MainFrame.getInstance());
	}
}