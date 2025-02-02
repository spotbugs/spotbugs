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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.filechooser.FileFilter;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.util.LaunchBrowser;

/**
 * The User Interface for creating a Project and editing it after the fact.
 *
 * @author Reuven
 *
 */
@SuppressWarnings("serial")
public class NewProjectWizard extends FBDialog {
    private final EmptyBorder border = new EmptyBorder(5, 5, 5, 5);

    private Project project;

    private boolean projectChanged = false;

    private boolean projectNameChanged = false;

    private final FBFileChooser chooser = new FBFileChooser();

    private final FileFilter directoryOrArchive = new FileFilter() {

        @Override
        public boolean accept(File f) {
            String fileName = f.getName().toLowerCase();
            return f.isDirectory() || fileName.endsWith(".jar") || fileName.endsWith(".ear") || fileName.endsWith(".war")
                    || fileName.endsWith(".zip") || fileName.endsWith(".sar") || fileName.endsWith(".class");
        }

        @Override
        public String getDescription() {
            return edu.umd.cs.findbugs.L10N.getLocalString("file.accepted_extensions",
                    "Class archive files (*.class, *.[jwes]ar, *.zip)");
        }
    };

    private final JList<String> analyzeList = new JList<>();

    private final DefaultListModel<String> analyzeModel = new DefaultListModel<>();

    private final JTextField projectName = new JTextField();

    private final JList<String> auxList = new JList<>();

    private final DefaultListModel<String> auxModel = new DefaultListModel<>();

    private final JList<String> sourceList = new JList<>();

    private final DefaultListModel<String> sourceModel = new DefaultListModel<>();

    private final JButton finishButton = new JButton();

    private final JButton cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));

    private final JComponent[] wizardComponents = new JComponent[3];

    private boolean isNewProject;

    public NewProjectWizard() {
        this(null);
        finishButton.setEnabled(false);
    }

    /**
     * @param curProject
     *            the project to populate from, or null to start a new one
     */
    public NewProjectWizard(Project curProject) {
        project = curProject;
        if (project == null) {
            ProjectSettings.newInstance();
            project = new Project();
            isNewProject = true;
        }
        boolean temp = false;

        if (curProject == null) {
            setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.new_item", "New Project"));
        } else {
            setTitle(edu.umd.cs.findbugs.L10N.getLocalString("dlg.reconfig", "Reconfigure"));
            temp = true;
        }

        final boolean reconfig = temp;

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        wizardComponents[0] = createFilePanel(
                edu.umd.cs.findbugs.L10N.getLocalString("dlg.class_jars_dirs_lbl", "Class archives and directories to analyze:"),
                analyzeList, analyzeModel, JFileChooser.FILES_AND_DIRECTORIES, directoryOrArchive,
                "Choose Class Archives and Directories to Analyze", false,
                "http://findbugs.sourceforge.net/manual/gui.html#d0e1087");

        wizardComponents[1] = createFilePanel(
                edu.umd.cs.findbugs.L10N.getLocalString("dlg.aux_class_lbl", "Auxiliary class locations:"), auxList, auxModel,
                JFileChooser.FILES_AND_DIRECTORIES, directoryOrArchive, "Choose Auxiliary Class Archives and Directories", false,
                "http://findbugs.sourceforge.net/FAQ.html#q4");

        wizardComponents[2] = createFilePanel(
                edu.umd.cs.findbugs.L10N.getLocalString("dlg.source_dirs_lbl", "Source directories:"), sourceList, sourceModel,
                JFileChooser.FILES_AND_DIRECTORIES, null, "Choose Source Directories", true,
                "http://findbugs.sourceforge.net/manual/gui.html#d0e1087");

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        if (MainFrameHelper.isMacLookAndFeel()) {
            buttons.add(Box.createHorizontalStrut(5));
            buttons.add(cancelButton);
            buttons.add(Box.createHorizontalStrut(5));
            buttons.add(finishButton);
        } else {
            buttons.add(Box.createHorizontalStrut(5));
            buttons.add(finishButton);
            buttons.add(Box.createHorizontalStrut(5));
            buttons.add(cancelButton);
        }
        finishButton.addActionListener(new ActionListener() {
            boolean keepGoing = false;

            private boolean displayWarningAndAskIfWeShouldContinue(String msg, String title) {
                if (keepGoing) {
                    return true;
                }
                boolean result = JOptionPane.showConfirmDialog(NewProjectWizard.this, msg, title, JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
                if (result) {
                    keepGoing = true;
                }
                return result;

            }

            @Override
            public void actionPerformed(ActionEvent evt) {

                if (displayWarnings()) {
                    return;
                }
                Project p;
                p = project;
                p.setGuiCallback(MainFrame.getInstance().getGuiCallback());
                clearProjectSettings(p);


                // Now that p is cleared, we can add in all the correct files.
                for (int i = 0; i < analyzeModel.getSize(); i++) {
                    p.addFile(analyzeModel.get(i));
                }
                for (int i = 0; i < auxModel.getSize(); i++) {
                    p.addAuxClasspathEntry(auxModel.get(i));
                }
                List<String> sourceDirs = new ArrayList<>(sourceModel.getSize());
                for (int i = 0; i < sourceModel.getSize(); i++) {
                    sourceDirs.add(sourceModel.get(i));
                }
                p.addSourceDirs(sourceDirs);
                p.setProjectName(projectName.getText());

                MainFrame mainFrame = MainFrame.getInstance();
                if (keepGoing) {
                    mainFrame.setProject(p);
                }
                if (projectChanged && (isNewProject
                        || JOptionPane.showConfirmDialog(NewProjectWizard.this, edu.umd.cs.findbugs.L10N
                                .getLocalString("dlg.project_settings_changed_lbl",
                                        "Project settings have been changed.  Perform a new analysis with the changed files?"),
                                edu.umd.cs.findbugs.L10N.getLocalString("dlg.redo_analysis_question_lbl", "Redo analysis?"),
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                    AnalyzingDialog.show(p);
                }
                if (reconfig) {
                    mainFrame.setProjectChanged(true);
                }

                String name = p.getProjectName();
                if (name == null) {
                    name = Project.UNNAMED_PROJECT;
                    Debug.println("PROJECT NAME IS NULL!!");
                }
                if (projectNameChanged) {
                    mainFrame.updateTitle();
                }

                dispose();
            }

            private boolean displayWarnings() {
                for (int i = 0; i < analyzeModel.getSize(); i++) {
                    File temp = new File(analyzeModel.get(i));
                    if (!temp.exists() && directoryOrArchive.accept(temp)) {
                        if (!displayWarningAndAskIfWeShouldContinue(
                                temp.getName() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.invalid_txt", " is invalid."),
                                edu.umd.cs.findbugs.L10N.getLocalString("dlg.error_ttl", "Can't locate file"))) {
                            return true;
                        }

                    }
                }

                for (int i = 0; i < sourceModel.getSize(); i++) {
                    File temp = new File(sourceModel.get(i));
                    if (!temp.exists() && directoryOrArchive.accept(temp)) {
                        if (!displayWarningAndAskIfWeShouldContinue(
                                temp.getName() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.invalid_txt", " is invalid."),
                                edu.umd.cs.findbugs.L10N.getLocalString("dlg.error_ttl", "Can't locate file"))) {
                            return true;
                        }
                    }
                }
                for (int i = 0; i < auxModel.getSize(); i++) {
                    File temp = new File(auxModel.get(i));
                    if (!temp.exists() && directoryOrArchive.accept(temp)) {
                        if (!displayWarningAndAskIfWeShouldContinue(
                                temp.getName() + " " + edu.umd.cs.findbugs.L10N.getLocalString("dlg.invalid_txt", " is invalid."),
                                edu.umd.cs.findbugs.L10N.getLocalString("dlg.error_ttl", "Can't locate file"))) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        if (curProject == null) {
            finishButton.setText(edu.umd.cs.findbugs.L10N.getLocalString("dlg.analyze_btn", "Analyze"));
        } else {
            finishButton.setText(edu.umd.cs.findbugs.L10N.getLocalString("dlg.ok_btn", "OK"));
        }
        cancelButton.addActionListener(evt -> dispose());

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(new EmptyBorder(5, 5, 5, 5));
        south.add(new JSeparator(), BorderLayout.NORTH);
        south.add(buttons, BorderLayout.EAST);

        if (curProject != null) {
            for (String i : curProject.getFileList()) {
                analyzeModel.addElement(i);
            }
            // If the project had no classes in it, disable the finish button
            // until classes are added.
            // if (curProject.getFileList().size()==0)
            // this.finishButton.setEnabled(false);
            for (String i : curProject.getAuxClasspathEntryList()) {
                auxModel.addElement(i);
            }
            for (String i : curProject.getSourceDirList()) {
                sourceModel.addElement(i);
            }
            projectName.setText(curProject.getProjectName());
            projectName.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    projectNameChanged = true;
                }
            });
        } else {
            // If project is null, disable finish button until classes are added
            finishButton.setEnabled(false);
        }

        // loadPanel(0);
        loadAllPanels(mainPanel);
        add(createTextFieldPanel("Project name", projectName), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // pack();
        setModal(true);
        setVisible(true);
    }

    private void clearProjectSettings(Project p) {
        // First clear p's old files, otherwise we can't remove a file
        // once an analysis has been performed on it
        int numOldFiles = p.getFileCount();
        for (int x = 0; x < numOldFiles; x++) {
            p.removeFile(0);
        }

        int numOldAuxFiles = p.getNumAuxClasspathEntries();
        for (int x = 0; x < numOldAuxFiles; x++) {
            p.removeAuxClasspathEntry(0);
        }

        int numOldSrc = p.getNumSourceDirs();
        for (int x = 0; x < numOldSrc; x++) {
            p.removeSourceDir(0);
        }
    }

    private JComponent createTextFieldPanel(String label, JTextField textField) {
        JPanel myPanel = new JPanel(new BorderLayout());

        myPanel.add(new JLabel(label), BorderLayout.NORTH);
        myPanel.add(textField, BorderLayout.CENTER);
        myPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        return myPanel;
    }

    private JPanel createFilePanel(final String label, final JList<String> list, final DefaultListModel<String> listModel,
            final int fileSelectionMode, final FileFilter filter, final String dialogTitle,
            boolean wizard, final String helpUrl) {
        JPanel myPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        myPanel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JButton button = new JButton("<HTML><U>Help");
        button.setFont(button.getFont().deriveFont(Font.PLAIN));
        button.setForeground(Color.BLUE);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(false);
        button.addActionListener(e -> {
            try {
                LaunchBrowser.showDocument(new URL(helpUrl));
            } catch (MalformedURLException e1) {
                throw new IllegalStateException(e1);
            }
        });
        myPanel.add(button, gbc);

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
        final JButton addButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.add_btn", "Add"));
        myPanel.add(addButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 0, 0);
        final JButton removeButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.remove_btn", "Remove"));
        myPanel.add(removeButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        final JButton wizardButton = new JButton("Wizard");
        if (wizard) {
            final NewProjectWizard thisGUI = this;
            myPanel.add(wizardButton, gbc);
            wizardButton.addActionListener(evt -> {
                final Project tempProject = new Project();
                for (int i1 = 0; i1 < analyzeModel.getSize(); i1++) {
                    tempProject.addFile(analyzeModel.get(i1));
                }
                for (int i2 = 0; i2 < auxModel.getSize(); i2++) {
                    tempProject.addAuxClasspathEntry(auxModel.get(i2));
                }

                java.awt.EventQueue.invokeLater(() -> {
                    final SourceDirectoryWizard dialog = new SourceDirectoryWizard(new javax.swing.JFrame(), true,
                            tempProject, thisGUI);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            if (dialog.discover != null && dialog.discover.isAlive()) {
                                dialog.discover.interrupt();
                            }
                        }
                    });
                    dialog.setVisible(true);
                });
            });
        }
        gbc.insets = new Insets(0, 0, 0, 0);
        myPanel.add(Box.createGlue(), gbc);
        myPanel.setBorder(border);
        addButton.addActionListener(evt -> {
            chooser.setFileSelectionMode(fileSelectionMode);
            chooser.setMultiSelectionEnabled(true);
            chooser.setApproveButtonText("Choose");
            chooser.setDialogTitle(dialogTitle);

            // Removes all the file filters currently in the chooser.
            for (FileFilter ff : chooser.getChoosableFileFilters()) {
                chooser.removeChoosableFileFilter(ff);
            }

            chooser.setFileFilter(filter);

            if (chooser.showOpenDialog(NewProjectWizard.this) == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = chooser.getSelectedFiles();
                for (File selectedFile : selectedFiles) {
                    listModel.addElement(selectedFile.getAbsolutePath());
                }
                projectChanged = true;
                // If this is the primary class directories add button, set
                // it to enable the finish button of the main dialog
                if (label.equals(edu.umd.cs.findbugs.L10N.getLocalString("dlg.class_jars_dirs_lbl",
                        "Class archives and directories to analyze:"))) {
                    finishButton.setEnabled(true);
                }
            }
        });
        removeButton.addActionListener(evt -> {
            if (list.getSelectedValues().length > 0) {
                projectChanged = true;
            }
            for (Object i : list.getSelectedValues()) {
                listModel.removeElement(i);
                // If this is the primary class directories remove button, set
                // it to disable finish when there are no class files being
                // analyzed
                // if (listModel.size()==0 &&
                // label.equals(edu.umd.cs.findbugs.L10N.getLocalString("dlg.class_jars_dirs_lbl",
                // "Class archives and directories to analyze:")))
                // finishButton.setEnabled(false);
            }
        });
        return myPanel;
    }

    /*
     * private void loadPanel(final int index) { SwingUtilities.invokeLater(new
     * Runnable() { public void run() { remove(wizardPanels[currentPanel]);
     * currentPanel = index; add(wizardPanels[index], BorderLayout.CENTER);
     * backButton.setEnabled(index > 0); nextButton.setEnabled(index <
     * wizardPanels.length - 1); validate(); repaint(); } }); }
     */
    private void loadAllPanels(final JPanel mainPanel) {
        SwingUtilities.invokeLater(() -> {
            int numPanels = wizardComponents.length;
            for (int i1 = 0; i1 < numPanels; i1++) {
                mainPanel.remove(wizardComponents[i1]);
            }
            for (int i2 = 0; i2 < numPanels; i2++) {
                mainPanel.add(wizardComponents[i2]);
            }
            validate();
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();

        for (JComponent component : wizardComponents) {
            setFontSizeHelper(component.getComponents(), Driver.getFontSize());
        }

        pack();

        int width = super.getWidth();

        if (width < 600) {
            width = 600;
        }
        setSize(new Dimension(width, 500));
        setLocationRelativeTo(MainFrame.getInstance());
    }

    /**
     * @param foundModel
     */
    public void setSourceDirecs(DefaultListModel<String> foundModel) {
        for (int i = 0; i < foundModel.size(); i++) {
            this.sourceModel.addElement(foundModel.getElementAt(i));
        }
    }
}
