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
import java.awt.GridLayout;
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
			return edu.umd.cs.findbugs.gui.L10N.getLocalString("file.jar_or_zip", "JAR or ZIP files (*.jar, *.zip)");
		}
	};
	
	private JList analyzeList = new JList();
	private DefaultListModel analyzeModel = new DefaultListModel();

	private JList auxList = new JList();
	private DefaultListModel auxModel = new DefaultListModel();
	
	private JList sourceList = new JList();
	private DefaultListModel sourceModel = new DefaultListModel();
	
	private JButton finishButton = new JButton(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.finish_btn", "Finish"));
	private JButton cancelButton = new JButton(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
	
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
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(3,1));
		
		
		wizardPanels[0] = createFilePanel(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.class_jars_dirs_lbl", "Class jars and directories to analyze:"), 
				analyzeList, analyzeModel, JFileChooser.FILES_AND_DIRECTORIES, directoryOrJar);
		
		wizardPanels[1] = createFilePanel(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.aux_class_lbl", "Auxiliary class locations:"), 
				auxList, auxModel, JFileChooser.FILES_AND_DIRECTORIES, directoryOrJar);
		
		wizardPanels[2] = createFilePanel(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.source_dirs_lbl", "Source directories:"), sourceList, sourceModel, JFileChooser.FILES_AND_DIRECTORIES, null);
				
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(finishButton);
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(cancelButton);
		finishButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
					for (int i = 0; i < analyzeModel.getSize(); i++){
					File temp = new File((String)analyzeModel.get(i));
					if(!(temp.exists() && (temp.isDirectory() || 
							temp.getName().endsWith(".jar") || temp.getName().endsWith(".zip")))){
						JOptionPane.showMessageDialog(NewProjectWizard.this, 
								temp.getName()+edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.invalid_txt", " is invalid."), edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.error_ttl", "Error"), JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			
				
				for (int i = 0; i < auxModel.getSize(); i++){
					File temp = new File((String)auxModel.get(i));
					if(!(temp.exists() && (temp.isDirectory() || 
							temp.getName().endsWith(".jar") || temp.getName().endsWith(".zip")))){
						JOptionPane.showMessageDialog(NewProjectWizard.this, 
								temp.getName()+edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.invalid_txt", " is invalid."), edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.error_ttl", "Error"), JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				Project p = (project == null ? new Project() : project);
				for (int i = 0; i < analyzeModel.getSize(); i++)
					p.addFile((String) analyzeModel.get(i));
				for (int i = 0; i < auxModel.getSize(); i++)
					p.addAuxClasspathEntry((String) auxModel.get(i));
				for (int i = 0; i < sourceModel.getSize(); i++)
					p.addSourceDir((String) sourceModel.get(i));
				
				if (project == null || (projectChanged && JOptionPane.showConfirmDialog(NewProjectWizard.this, edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.project_settings_changed_lbl", "Project settings have been changed.  Perform a new analysis with the changed files?"), edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.redo_analysis_question_lbl", "Redo analysis?"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION))
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
			for (String i : curProject.getFileList()) 
				analyzeModel.addElement(i);
			for (String i : curProject.getAuxClasspathEntryList())
				auxModel.addElement(i);
			for (String i : curProject.getSourceDirList())
				sourceModel.addElement(i);
		}
		
		//loadPanel(0);
		loadAllPanels(mainPanel);
		add(mainPanel, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
//		pack();
		setTitle(edu.umd.cs.findbugs.gui.L10N.getLocalString("menu.new_item", "New Project"));
		setModal(true);
		setVisible(true);
	}

	/**
	 * @param label TODO
	 * 
	 */
	private JPanel createFilePanel(String label, final JList list, 
			final DefaultListModel listModel, final int fileSelectionMode, final FileFilter filter) {
		JPanel myPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		myPanel.add(new JLabel(label), gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		myPanel.add(new JScrollPane(list), gbc);
		list.setModel(listModel);
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		final JButton addButton = new JButton(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.add_btn", "Add"));
		myPanel.add(addButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(5, 0, 0, 0);
		final JButton removeButton = new JButton(edu.umd.cs.findbugs.gui.L10N.getLocalString("dlg.remove_btn", "Remove"));
		myPanel.add(removeButton, gbc);
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.insets = new Insets(0, 0, 0, 0);
		myPanel.add(Box.createGlue(), gbc);
		myPanel.setBorder(border);
		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				chooser.setFileSelectionMode(fileSelectionMode);
				chooser.setFileFilter(filter);
				if (chooser.showOpenDialog(NewProjectWizard.this) == JFileChooser.APPROVE_OPTION)
				{
					listModel.addElement(chooser.getSelectedFile().getAbsolutePath());
					projectChanged = true;
				}
			}
		});
		removeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (list.getSelectedValues().length > 0)
					projectChanged = true;
				for (Object i : list.getSelectedValues())
					listModel.removeElement(i);
			}
		});
		return myPanel;
	}
	/*
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
	*/
	private void loadAllPanels(final JPanel mainPanel)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				int numPanels = wizardPanels.length;
				for(int i=0; i<numPanels; i++)
					mainPanel.remove(wizardPanels[i]);
				for(int i=0; i<numPanels; i++)
					mainPanel.add(wizardPanels[i]);
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
		
		if(width < 600)
			width = 600;
		setSize(new Dimension(width, 500));
		setLocationRelativeTo(MainFrame.getInstance());
	}
}