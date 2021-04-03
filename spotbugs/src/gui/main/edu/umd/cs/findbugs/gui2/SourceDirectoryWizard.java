/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008 David H. Hovemeyer <david.hovemeyer@gmail.com>
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

import edu.umd.cs.findbugs.DiscoverSourceDirectories;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Wizard dialog to automatically find and configure source directories for a project.
 *
 * @author David Hovemeyer
 */
public class SourceDirectoryWizard extends JDialog {

    private static final int MIN_STEP = 1;

    private static final int MAX_STEP = 2;

    /**
     * Creates new form SourceDirectoryWizard
     *
     * @param parentGUI
     */
    public SourceDirectoryWizard(
            java.awt.Frame parent, boolean modal, Project project, NewProjectWizard parentGUI) {
        super(parent, modal);
        this.parentGUI = parentGUI;
        this.project = project;
        initComponents();
        setStep(1);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */

    // <editor-fold defaultstate="collapsed"
    // desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        foundModel = new DefaultListModel<>();
        progressModel = new DefaultListModel<>();
        contentPanel = new JPanel();
        secondPanel = new JPanel();
        jScrollPane1 = new JScrollPane();
        jScrollPane2 = new JScrollPane();
        jList1 = new JList<>();
        jList2 = new JList<>();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        firstPanel = new JPanel();
        sourceRootLabel = new JLabel();
        sourceRootBox = new JTextField();
        srcFileIconLabel = new JLabel();
        card1TitleLabel = new JLabel();
        chooser = new JFileChooser();
        browseButton = new JButton();
        card1Explanation1Label = new JLabel();
        card1Explanation2Label = new JLabel();
        card1Explanation3Label = new JLabel();
        previousButton = new JButton();
        nextButton = new JButton();
        finshButton = new JButton();
        errorMessageLabel = new JLabel();
        Dimension d = new Dimension(600, 425);
        this.setPreferredSize(d);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SpotBugs Source Directory Configuration Wizard");
        getContentPane().setLayout(null);

        jList2.setModel(progressModel);

        contentPanel.setLayout(new CardLayout());

        secondPanel.setLayout(null);

        jScrollPane1.setViewportView(jList1);

        secondPanel.add(jScrollPane1);
        jScrollPane1.setBounds(250, 50, 258, 130);

        jLabel1.setFont(new java.awt.Font("SansSerif", 0, 14));
        jLabel1.setText("Source directories found:");
        secondPanel.add(jLabel1);
        jLabel1.setBounds(250, 30, 173, 17);

        jLabel2.setFont(new java.awt.Font("SansSerif", 0, 14));
        jLabel2.setText("Click Finish to accept this");
        secondPanel.add(jLabel2);
        jLabel2.setBounds(40, 90, 173, 17);

        jLabel3.setFont(new java.awt.Font("SansSerif", 0, 14));
        jLabel3.setText("list of source directories");
        secondPanel.add(jLabel3);
        jLabel3.setBounds(40, 110, 165, 17);

        contentPanel.add(secondPanel, "card2");

        jScrollPane2.setViewportView(jList2);

        firstPanel.add(jScrollPane2);
        jScrollPane2.setBounds(200, 250, 250, 75);

        firstPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        firstPanel.setLayout(null);

        sourceRootLabel.setText("Top-level source directory:");
        firstPanel.add(sourceRootLabel);
        sourceRootLabel.setBounds(30, 220, 168, 17);
        firstPanel.add(sourceRootBox);
        sourceRootBox.setBounds(200, 210, 250, 29);

        srcFileIconLabel.setIcon(
                new ImageIcon("/usr/share/icons/default.kde/128x128/mimetypes/source_java.png")); // NOI18N
        firstPanel.add(srcFileIconLabel);
        srcFileIconLabel.setBounds(50, 80, 128, 128);

        card1TitleLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        card1TitleLabel.setText("Where are your source files?");
        firstPanel.add(card1TitleLabel);
        card1TitleLabel.setBounds(150, 20, 353, 29);

        browseButton.setText("Browse...");
        firstPanel.add(browseButton);

        browseButton.addActionListener(
                evt -> {
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setMultiSelectionEnabled(true);
                    chooser.setApproveButtonText("Choose");
                    chooser.setDialogTitle("Choose the directory");

                    if (chooser.showOpenDialog(SourceDirectoryWizard.this) == JFileChooser.APPROVE_OPTION) {
                        File[] selectedFiles = chooser.getSelectedFiles();
                        for (File selectedFile : selectedFiles) {
                            sourceRootBox.setText(selectedFile.getAbsolutePath());
                        }
                        nextButton.setEnabled(!"".equals(sourceRootBox.getText()));
                    }
                });

        browseButton.setBounds(450, 210, 100, 28);

        card1Explanation1Label.setFont(new java.awt.Font("SansSerif", 0, 14));
        card1Explanation1Label.setText("Enter the top-level directory");
        firstPanel.add(card1Explanation1Label);
        card1Explanation1Label.setBounds(230, 80, 193, 17);

        card1Explanation2Label.setFont(new java.awt.Font("SansSerif", 0, 14));
        card1Explanation2Label.setText("containing your application's");
        firstPanel.add(card1Explanation2Label);
        card1Explanation2Label.setBounds(230, 100, 198, 17);

        card1Explanation3Label.setFont(new java.awt.Font("SansSerif", 0, 14));
        card1Explanation3Label.setText("source files.");
        firstPanel.add(card1Explanation3Label);
        card1Explanation3Label.setBounds(230, 120, 82, 17);

        contentPanel.add(firstPanel, "card1");

        getContentPane().add(contentPanel);
        contentPanel.setBounds(0, 0, 750, 300);

        previousButton.setText("<< Previous");
        previousButton.addActionListener(evt -> previousButtonActionPerformed(evt));
        getContentPane().add(previousButton);
        previousButton.setBounds(150, 350, 100, 29);

        nextButton.setText("Next >>");
        nextButton.addActionListener(evt -> nextButtonActionPerformed(evt));
        getContentPane().add(nextButton);
        nextButton.setBounds(250, 350, 100, 29);

        finshButton.setText("Finish");
        finshButton.addActionListener(evt -> finshButtonActionPerformed(evt));
        getContentPane().add(finshButton);
        finshButton.setBounds(350, 350, 100, 29);

        errorMessageLabel.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        errorMessageLabel.setForeground(new java.awt.Color(255, 0, 0));
        getContentPane().add(errorMessageLabel);
        errorMessageLabel.setBounds(0, 300, 500, 20);

        pack();
    } // </editor-fold>//GEN-END:initComponents

    private void previousButtonActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_previousButtonActionPerformed
        if (step > MIN_STEP) {
            setStep(step - 1);
        }
        progressModel.removeAllElements();
        foundModel.removeAllElements();
    } // GEN-LAST:event_previousButtonActionPerformed

    private void nextButtonActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_nextButtonActionPerformed

        discover =
                new Thread() {
                    @Override
                    public void run() {
                        IErrorLogger errorLogger =
                                new IErrorLogger() {
                                    @Override
                                    public void reportMissingClass(ClassNotFoundException ex) {
                                        String className = ClassNotFoundExceptionParser.getMissingClassName(ex);
                                        if (className != null) {
                                            logError("Missing class: " + className);
                                        } else {
                                            logError("Missing class: " + ex);
                                        }
                                    }

                                    @Override
                                    public void reportMissingClass(ClassDescriptor classDescriptor) {
                                        logError("Missing class: " + classDescriptor.toDottedClassName());
                                    }

                                    @Override
                                    public void logError(String message) {
                                        System.err.println("Error: " + message);
                                    }

                                    @Override
                                    public void logError(String message, Throwable e) {
                                        logError(message + ": " + e.getMessage());
                                    }

                                    @Override
                                    public void reportSkippedAnalysis(MethodDescriptor method) {
                                        logError("Skipped analysis of method " + method.toString());
                                    }
                                };

                        DiscoverSourceDirectories.Progress progress =
                                new DiscoverSourceDirectories.Progress() {

                                    @Override
                                    public void startRecursiveDirectorySearch() {
                                        progressModel.addElement("Scanning directories...");
                                    }

                                    @Override
                                    public void doneRecursiveDirectorySearch() {
                                    }

                                    @Override
                                    public void startScanningArchives(int numArchivesToScan) {
                                        progressModel.addElement("Scanning " + numArchivesToScan + " archives..");
                                    }

                                    @Override
                                    public void doneScanningArchives() {
                                    }

                                    @Override
                                    public void startScanningClasses(int numClassesToScan) {
                                        progressModel.addElement("Scanning " + numClassesToScan + " classes...");
                                    }

                                    @Override
                                    public void finishClass() {
                                    }

                                    @Override
                                    public void doneScanningClasses() {
                                    }

                                    @Override
                                    public void finishArchive() {
                                    }

                                    @Override
                                    public void startArchive(String name) {
                                    }
                                };
                        DiscoverSourceDirectories discoverSourceDirectories = new DiscoverSourceDirectories();
                        discoverSourceDirectories.setProject(project);
                        discoverSourceDirectories.setRootSourceDirectory(sourceRootBox.getText());
                        discoverSourceDirectories.setErrorLogger(errorLogger);
                        discoverSourceDirectories.setProgress(progress);

                        try {
                            discoverSourceDirectories.execute();
                        } catch (CheckedAnalysisException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }

                        jList1.setModel(foundModel);

                        for (String srcDir : discoverSourceDirectories.getDiscoveredSourceDirectoryList()) {
                            foundModel.addElement(srcDir);
                        }

                        if (step < MAX_STEP) {
                            setStep(step + 1);
                        }
                    }
                };
        discover.start();
    } // GEN-LAST:event_nextButtonActionPerformed

    private void finshButtonActionPerformed(
            java.awt.event.ActionEvent evt) { // GEN-FIRST:event_finshButtonActionPerformed
        if (parentGUI != null) {
            parentGUI.setSourceDirecs(foundModel);
        }
        if (discover != null && discover.isAlive()) {
            discover.stop();
        }
        dispose();
    } // GEN-LAST:event_finshButtonActionPerformed

    /** @param args the command line arguments */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(
                () -> {
                    final SourceDirectoryWizard dialog =
                            new SourceDirectoryWizard(new JFrame(), true, new Project(), null);
                    dialog.setVisible(true);
                });
    }

    private JFileChooser chooser;

    private final Project project;

    private final NewProjectWizard parentGUI;

    private DefaultListModel<String> foundModel;

    private DefaultListModel<String> progressModel;

    private JList<String> jList2;

    public Thread discover;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton browseButton;

    private JLabel card1Explanation1Label;

    private JLabel card1Explanation2Label;

    private JLabel card1Explanation3Label;

    private JLabel card1TitleLabel;

    private JPanel contentPanel;

    private JLabel errorMessageLabel;

    private JButton finshButton;

    private JPanel firstPanel;

    private JLabel jLabel1;

    private JLabel jLabel2;

    private JLabel jLabel3;

    private JList<String> jList1;

    private JScrollPane jScrollPane1;

    private JScrollPane jScrollPane2;

    private JButton nextButton;

    private JButton previousButton;

    private JPanel secondPanel;

    private JTextField sourceRootBox;

    private JLabel sourceRootLabel;

    private JLabel srcFileIconLabel;

    // End of variables declaration//GEN-END:variables

    private int step;

    private void setStep(int step) {
        if (step < MIN_STEP || step > MAX_STEP) {
            throw new IllegalArgumentException("Invalid step " + step);
        }
        this.step = step;
        previousButton.setEnabled(step != MIN_STEP);
        nextButton.setEnabled(step != MAX_STEP && !"".equals(sourceRootBox.getText()));

        CardLayout cards = (CardLayout) contentPanel.getLayout();
        cards.show(contentPanel, "card" + step);
    }
}
