/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

/*
 * FindBugsFrame.java
 *
 * Created on March 30, 2003, 12:05 PM
 */

package edu.umd.cs.findbugs.gui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import edu.umd.cs.daveho.ba.SourceFinder;
import edu.umd.cs.findbugs.*;

/**
 * The main GUI frame for FindBugs.
 *
 * @author David Hovemeyer
 */
public class FindBugsFrame extends javax.swing.JFrame {
    
    /* ----------------------------------------------------------------------
     * Helper classes
     * ---------------------------------------------------------------------- */
    
    /**
     * Custom cell renderer for the bug tree.
     */
    private static class BugCellRenderer extends DefaultTreeCellRenderer {
        private ImageIcon bugGroupIcon;
        private ImageIcon packageIcon;
        private ImageIcon bugIcon;
        private ImageIcon classIcon;
        private ImageIcon methodIcon;
        private ImageIcon fieldIcon;
        private ImageIcon sourceFileIcon;
        
        public BugCellRenderer() {
            ClassLoader classLoader = this.getClass().getClassLoader();
            bugGroupIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug.png"));
            packageIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/package.png"));
            bugIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug2.png"));
            classIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/class.png"));
            methodIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/method.png"));
            fieldIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/field.png"));
            sourceFileIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/sourcefile.png"));
        }
        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
        boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            // Set the icon, depending on what kind of node it is
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object obj = node.getUserObject();
            if (obj instanceof BugInstance) {
                setIcon(bugIcon);
            } else if (obj instanceof ClassAnnotation) {
                setIcon(classIcon);
            } else if (obj instanceof MethodAnnotation) {
                setIcon(methodIcon);
            } else if (obj instanceof FieldAnnotation) {
                setIcon(fieldIcon);
            } else if (obj instanceof SourceLineAnnotation) {
                setIcon(sourceFileIcon);
            } else if (obj instanceof BugInstanceGroup) {
                // This is a "group" node
                BugInstanceGroup groupNode = (BugInstanceGroup) obj;
                String groupType = groupNode.getGroupType();
                if (groupType == GROUP_BY_CLASS) {
                    setIcon(classIcon);
                } else if (groupType == GROUP_BY_PACKAGE) {
                    setIcon(packageIcon);
                } else if (groupType == GROUP_BY_BUG_TYPE) {
                    setIcon(bugGroupIcon);
                }
            } else {
                setIcon(null);
            }
            
            return this;
        }
    }
    
    /** The instance of BugCellRenderer. */
    private static final FindBugsFrame.BugCellRenderer bugCellRenderer = new FindBugsFrame.BugCellRenderer();
    
    /**
     * Tree node type for BugInstances.
     * We use this instead of plain DefaultMutableTreeNodes in order to
     * get more control over the exact text that is shown in the tree.
     */
    private class BugTreeNode extends DefaultMutableTreeNode {
        public BugTreeNode(BugInstance bugInstance) {
            super(bugInstance);
        }
        
        public String toString() {
            BugInstance bugInstance = (BugInstance) getUserObject();
            String result;
            if (fullDescriptionsItem.isSelected()) {
                result = bugInstance.getMessage();
            } else {
                result = bugInstance.toString();
            }
            return result;
        }
    }
    
    /**
     * Compare BugInstance class names.
     * This is useful for grouping bug instances by class.
     * Note that all instances with the same class name will compare
     * as equal.
     */
    private static class BugInstanceClassComparator implements Comparator<BugInstance> {
        public int compare(BugInstance lhs, BugInstance rhs) {
            return lhs.getPrimaryClass().compareTo(rhs.getPrimaryClass());
        }
    }
    
    /** The instance of BugInstanceClassComparator. */
    private static final Comparator<BugInstance> bugInstanceClassComparator = new BugInstanceClassComparator();
    
    /**
     * Compare BugInstance package names.
     * This is useful for grouping bug instances by package.
     * Note that all instances with the same package name will compare
     * as equal.
     */
    private static class BugInstancePackageComparator implements Comparator<BugInstance> {
        public int compare(BugInstance lhs, BugInstance rhs) {
            return lhs.getPrimaryClass().getPackageName().compareTo(
            rhs.getPrimaryClass().getPackageName());
        }
    }
    
    /** The instance of BugInstancePackageComparator. */
    private static final Comparator<BugInstance> bugInstancePackageComparator = new BugInstancePackageComparator();
    
    /**
     * Compare BugInstance bug types.
     * This is useful for grouping bug instances by bug type.
     * Note that all instances with the same bug type will compare
     * as equal.
     */
    private static class BugInstanceTypeComparator implements Comparator<BugInstance> {
        public int compare(BugInstance lhs, BugInstance rhs) {
            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            return lhsString.substring(0, lhsString.indexOf(':')).compareTo(
            rhsString.substring(0, rhsString.indexOf(':')));
        }
    }
    
    /** The instance of BugInstanceTypeComparator. */
    private static final Comparator<BugInstance> bugInstanceTypeComparator = new BugInstanceTypeComparator();
    
    /**
     * Two-level comparison of bug instances by class name and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByClassComparator implements Comparator<BugInstance> {
        public int compare(BugInstance a, BugInstance b) {
            int cmp = bugInstanceClassComparator.compare(a, b);
            if (cmp != 0)
                return cmp;
            return a.compareTo(b);
        }
    }
    
    /** The instance of BugInstanceByClassComparator. */
    private static final Comparator<BugInstance> bugInstanceByClassComparator = new FindBugsFrame.BugInstanceByClassComparator();
    
    /**
     * Two-level comparison of bug instances by package and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByPackageComparator implements Comparator<BugInstance> {
        public int compare(BugInstance a, BugInstance b) {
            int cmp = bugInstancePackageComparator.compare(a, b);
            if (cmp != 0)
                return cmp;
            return a.compareTo(b);
        }
    }
    
    /** The instance of BugInstanceByPackageComparator. */
    private static final Comparator<BugInstance> bugInstanceByPackageComparator = new FindBugsFrame.BugInstanceByPackageComparator();
    
    /**
     * Two-level comparison of bug instances by bug type and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByTypeComparator implements Comparator<BugInstance> {
        public int compare(BugInstance a, BugInstance b) {
            int cmp = bugInstanceTypeComparator.compare(a, b);
            if (cmp != 0)
                return cmp;
            return a.compareTo(b);
        }
    }
    
    /** The instance of BugTypeByTypeComparator. */
    private static final Comparator<BugInstance> bugInstanceByTypeComparator = new FindBugsFrame.BugInstanceByTypeComparator();
    
    /**
     * Swing FileFilter class for file selection dialogs for FindBugs project files.
     */
    private static class ProjectFileFilter extends FileFilter {
        public boolean accept(File file) { return file.isDirectory() || file.getName().endsWith(".fb"); }
        public String getDescription() { return "FindBugs projects (*.fb)"; }
    }
    
    /** The instance of ProjectFileFilter. */
    private static final FileFilter projectFileFilter = new ProjectFileFilter();
    
    /**
     * Swing FileFilter for choosing an auxiliary classpath entry.
     * Both Jar files and directories can be chosen.
     */
    private static class AuxClasspathEntryFileFilter extends FileFilter {
        public boolean accept(File file) { return file.isDirectory() || file.getName().endsWith(".jar"); }
        public String getDescription() { return "Jar files and directories"; }
    }
    
    /** The instance of AuxClasspathEntryFileFilter. */
    private static final FileFilter auxClasspathEntryFileFilter = new AuxClasspathEntryFileFilter();
    
    /* ----------------------------------------------------------------------
     * Constants
     * ---------------------------------------------------------------------- */
    
    private static final String GROUP_BY_CLASS = "By class";
    private static final String GROUP_BY_PACKAGE = "By package";
    private static final String GROUP_BY_BUG_TYPE = "By bug type";
    private static final String[] GROUP_BY_ORDER_LIST = {
        GROUP_BY_CLASS, GROUP_BY_PACKAGE, GROUP_BY_BUG_TYPE
    };
    
    /**
     * A fudge value required in our hack to get the REAL maximum
     * divider location for a JSplitPane.  Experience suggests that
     * the value "1" would work here, but setting it a little higher
     * makes the code a bit more robust.
     */
    private static final int DIVIDER_FUDGE = 3;
    
    /* ----------------------------------------------------------------------
     * Constructor
     * ---------------------------------------------------------------------- */
    
    /** Creates new form FindBugsFrame. */
    public FindBugsFrame() {
        initComponents();
        postInitComponents();
    }
    
    /* ----------------------------------------------------------------------
     * Component initialization and event handlers
     * ---------------------------------------------------------------------- */
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        consoleSplitter = new javax.swing.JSplitPane();
        viewPanel = new javax.swing.JPanel();
        emptyPanel = new javax.swing.JPanel();
        reportPanel = new javax.swing.JPanel();
        editProjectPanel = new javax.swing.JPanel();
        jarFileLabel = new javax.swing.JLabel();
        jarNameTextField = new javax.swing.JTextField();
        addJarButton = new javax.swing.JButton();
        jarFileListLabel = new javax.swing.JLabel();
        sourceDirLabel = new javax.swing.JLabel();
        srcDirTextField = new javax.swing.JTextField();
        addSourceDirButton = new javax.swing.JButton();
        sourceDirListLabel = new javax.swing.JLabel();
        removeJarButton = new javax.swing.JButton();
        removeSrcDirButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        browseJarButton = new javax.swing.JButton();
        browseSrcDirButton = new javax.swing.JButton();
        editProjectLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        findBugsButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jarFileListScrollPane = new javax.swing.JScrollPane();
        jarFileList = new javax.swing.JList();
        sourceDirListScrollPane = new javax.swing.JScrollPane();
        sourceDirList = new javax.swing.JList();
        classpathEntryLabel = new javax.swing.JLabel();
        classpathEntryListLabel = new javax.swing.JLabel();
        classpathEntryTextField = new javax.swing.JTextField();
        browseClasspathEntryButton = new javax.swing.JButton();
        addClasspathEntryButton = new javax.swing.JButton();
        removeClasspathEntryButton = new javax.swing.JButton();
        classpathEntryListScrollPane = new javax.swing.JScrollPane();
        classpathEntryList = new javax.swing.JList();
        jSeparator5 = new javax.swing.JSeparator();
        bugTreePanel = new javax.swing.JPanel();
        bugTreeBugDetailsSplitter = new javax.swing.JSplitPane();
        groupByTabbedPane = new javax.swing.JTabbedPane();
        byClassScrollPane = new javax.swing.JScrollPane();
        byClassBugTree = new javax.swing.JTree();
        byPackageScrollPane = new javax.swing.JScrollPane();
        byPackageBugTree = new javax.swing.JTree();
        byBugTypeScrollPane = new javax.swing.JScrollPane();
        byBugTypeBugTree = new javax.swing.JTree();
        bugDetailsTabbedPane = new javax.swing.JTabbedPane();
        bugDescriptionScrollPane = new javax.swing.JScrollPane();
        bugDescriptionEditorPane = new javax.swing.JEditorPane();
        sourceTextAreaScrollPane = new javax.swing.JScrollPane();
        sourceTextArea = new javax.swing.JTextArea();
        consoleScrollPane = new javax.swing.JScrollPane();
        consoleMessageArea = new javax.swing.JTextArea();
        theMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newProjectItem = new javax.swing.JMenuItem();
        openProjectItem = new javax.swing.JMenuItem();
        saveProjectItem = new javax.swing.JMenuItem();
        saveProjectAsItem = new javax.swing.JMenuItem();
        reloadProjectItem = new javax.swing.JMenuItem();
        closeProjectItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        exitItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        viewConsoleItem = new javax.swing.JCheckBoxMenuItem();
        viewBugDetailsItem = new javax.swing.JCheckBoxMenuItem();
        fullDescriptionsItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        configureDetectorsItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        consoleSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        consoleSplitter.setResizeWeight(1.0);
        consoleSplitter.setOneTouchExpandable(true);
        consoleSplitter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                consoleSplitterPropertyChange(evt);
            }
        });

        viewPanel.setLayout(new java.awt.CardLayout());

        viewPanel.add(emptyPanel, "EmptyPanel");

        viewPanel.add(reportPanel, "ReportPanel");

        editProjectPanel.setLayout(new java.awt.GridBagLayout());

        jarFileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileLabel.setText("Jar file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileLabel, gridBagConstraints);

        jarNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jarNameTextFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(jarNameTextField, gridBagConstraints);

        addJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addJarButton.setText("Add");
        addJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(addJarButton, gridBagConstraints);

        jarFileListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileListLabel.setText("Jar Files:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileListLabel, gridBagConstraints);

        sourceDirLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirLabel.setText("Source directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirLabel, gridBagConstraints);

        srcDirTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                srcDirTextFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(srcDirTextField, gridBagConstraints);

        addSourceDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addSourceDirButton.setText("Add");
        addSourceDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSourceDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(addSourceDirButton, gridBagConstraints);

        sourceDirListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirListLabel.setText("Source directories:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirListLabel, gridBagConstraints);

        removeJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeJarButton.setText("Remove");
        removeJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(removeJarButton, gridBagConstraints);

        removeSrcDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeSrcDirButton.setText("Remove");
        removeSrcDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSrcDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(removeSrcDirButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator1, gridBagConstraints);

        browseJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseJarButton.setText("Browse");
        browseJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(browseJarButton, gridBagConstraints);

        browseSrcDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseSrcDirButton.setText("Browse");
        browseSrcDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseSrcDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(browseSrcDirButton, gridBagConstraints);

        editProjectLabel.setBackground(new java.awt.Color(0, 0, 204));
        editProjectLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        editProjectLabel.setForeground(new java.awt.Color(255, 255, 255));
        editProjectLabel.setText("Project");
        editProjectLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        editProjectPanel.add(editProjectLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator2, gridBagConstraints);

        findBugsButton.setMnemonic('B');
        findBugsButton.setText("Find Bugs!");
        findBugsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findBugsButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(findBugsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator4, gridBagConstraints);

        jarFileListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        jarFileList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jarFileList.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jarFileListScrollPane.setViewportView(jarFileList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.6;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileListScrollPane, gridBagConstraints);

        sourceDirListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        sourceDirList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        sourceDirList.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sourceDirListScrollPane.setViewportView(sourceDirList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirListScrollPane, gridBagConstraints);

        classpathEntryLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathEntryLabel.setText("Classpath entry:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        editProjectPanel.add(classpathEntryLabel, gridBagConstraints);

        classpathEntryListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathEntryListLabel.setText("Classpath entries:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        editProjectPanel.add(classpathEntryListLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(classpathEntryTextField, gridBagConstraints);

        browseClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseClasspathEntryButton.setText("Browse");
        browseClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        editProjectPanel.add(browseClasspathEntryButton, gridBagConstraints);

        addClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addClasspathEntryButton.setText("Add");
        addClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(addClasspathEntryButton, gridBagConstraints);

        removeClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeClasspathEntryButton.setText("Remove");
        removeClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(removeClasspathEntryButton, gridBagConstraints);

        classpathEntryListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        classpathEntryList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        classpathEntryList.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathEntryListScrollPane.setViewportView(classpathEntryList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(classpathEntryListScrollPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        editProjectPanel.add(jSeparator5, gridBagConstraints);

        viewPanel.add(editProjectPanel, "EditProjectPanel");

        bugTreePanel.setLayout(new java.awt.GridBagLayout());

        bugTreeBugDetailsSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        bugTreeBugDetailsSplitter.setResizeWeight(1.0);
        bugTreeBugDetailsSplitter.setOneTouchExpandable(true);
        bugTreeBugDetailsSplitter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                bugTreeBugDetailsSplitterPropertyChange(evt);
            }
        });

        byClassScrollPane.setViewportView(byClassBugTree);

        groupByTabbedPane.addTab("By Class", byClassScrollPane);

        byPackageScrollPane.setViewportView(byPackageBugTree);

        groupByTabbedPane.addTab("By Package", byPackageScrollPane);

        byBugTypeScrollPane.setViewportView(byBugTypeBugTree);

        groupByTabbedPane.addTab("By Bug Type", byBugTypeScrollPane);

        bugTreeBugDetailsSplitter.setTopComponent(groupByTabbedPane);

        bugDescriptionEditorPane.setEditable(false);
        bugDescriptionScrollPane.setViewportView(bugDescriptionEditorPane);

        bugDetailsTabbedPane.addTab("Details", bugDescriptionScrollPane);

        sourceTextAreaScrollPane.setMinimumSize(new java.awt.Dimension(22, 180));
        sourceTextAreaScrollPane.setPreferredSize(new java.awt.Dimension(0, 100));
        sourceTextArea.setEditable(false);
        sourceTextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 12));
        sourceTextAreaScrollPane.setViewportView(sourceTextArea);

        bugDetailsTabbedPane.addTab("Source code", sourceTextAreaScrollPane);

        bugTreeBugDetailsSplitter.setBottomComponent(bugDetailsTabbedPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bugTreePanel.add(bugTreeBugDetailsSplitter, gridBagConstraints);

        viewPanel.add(bugTreePanel, "BugTree");

        consoleSplitter.setTopComponent(viewPanel);

        consoleScrollPane.setMinimumSize(new java.awt.Dimension(22, 100));
        consoleScrollPane.setPreferredSize(new java.awt.Dimension(0, 100));
        consoleMessageArea.setBackground(new java.awt.Color(204, 204, 204));
        consoleMessageArea.setEditable(false);
        consoleMessageArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 12));
        consoleMessageArea.setMinimumSize(new java.awt.Dimension(0, 0));
        consoleMessageArea.setAutoscrolls(false);
        consoleScrollPane.setViewportView(consoleMessageArea);

        consoleSplitter.setBottomComponent(consoleScrollPane);

        getContentPane().add(consoleSplitter, java.awt.BorderLayout.CENTER);

        theMenuBar.setFont(new java.awt.Font("Dialog", 0, 12));
        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        fileMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                fileMenuMenuSelected(evt);
            }
        });

        newProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        newProjectItem.setMnemonic('N');
        newProjectItem.setText("New Project");
        newProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(newProjectItem);

        openProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        openProjectItem.setMnemonic('O');
        openProjectItem.setText("Open Project");
        openProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(openProjectItem);

        saveProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        saveProjectItem.setMnemonic('S');
        saveProjectItem.setText("Save Project");
        saveProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveProjectItem);

        saveProjectAsItem.setFont(new java.awt.Font("Dialog", 0, 12));
        saveProjectAsItem.setMnemonic('A');
        saveProjectAsItem.setText("Save Project As");
        saveProjectAsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectAsItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveProjectAsItem);

        reloadProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        reloadProjectItem.setMnemonic('R');
        reloadProjectItem.setText("Reload Project");
        reloadProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(reloadProjectItem);

        closeProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        closeProjectItem.setMnemonic('C');
        closeProjectItem.setText("Close Project");
        closeProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(closeProjectItem);

        fileMenu.add(jSeparator3);

        exitItem.setFont(new java.awt.Font("Dialog", 0, 12));
        exitItem.setMnemonic('X');
        exitItem.setText("Exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });

        fileMenu.add(exitItem);

        theMenuBar.add(fileMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");
        viewMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        viewMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                viewMenuMenuSelected(evt);
            }
        });

        viewConsoleItem.setFont(new java.awt.Font("Dialog", 0, 12));
        viewConsoleItem.setMnemonic('C');
        viewConsoleItem.setText("Console");
        viewConsoleItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewConsoleItemActionPerformed(evt);
            }
        });

        viewMenu.add(viewConsoleItem);

        viewBugDetailsItem.setFont(new java.awt.Font("Dialog", 0, 12));
        viewBugDetailsItem.setMnemonic('D');
        viewBugDetailsItem.setSelected(true);
        viewBugDetailsItem.setText("Bug Details");
        viewBugDetailsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewBugDetailsItemActionPerformed(evt);
            }
        });

        viewMenu.add(viewBugDetailsItem);

        fullDescriptionsItem.setFont(new java.awt.Font("Dialog", 0, 12));
        fullDescriptionsItem.setMnemonic('F');
        fullDescriptionsItem.setSelected(true);
        fullDescriptionsItem.setText("Full Descriptions");
        fullDescriptionsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullDescriptionsItemActionPerformed(evt);
            }
        });

        viewMenu.add(fullDescriptionsItem);

        viewMenu.add(jSeparator6);

        configureDetectorsItem.setFont(new java.awt.Font("Dialog", 0, 12));
        configureDetectorsItem.setText("Configure Detectors...");
        configureDetectorsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureDetectorsItemActionPerformed(evt);
            }
        });

        viewMenu.add(configureDetectorsItem);

        theMenuBar.add(viewMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");
        helpMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        aboutItem.setFont(new java.awt.Font("Dialog", 0, 12));
        aboutItem.setMnemonic('A');
        aboutItem.setText("About");
        aboutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutItemActionPerformed(evt);
            }
        });

        helpMenu.add(aboutItem);

        theMenuBar.add(helpMenu);

        setJMenuBar(theMenuBar);

        pack();
    }//GEN-END:initComponents

    private void configureDetectorsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureDetectorsItemActionPerformed
        ConfigureDetectorsDialog dialog = new ConfigureDetectorsDialog(this, true);
        dialog.setSize(600, 400);
        dialog.show();
    }//GEN-LAST:event_configureDetectorsItemActionPerformed

    private void reloadProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadProjectItemActionPerformed
        Project current = getCurrentProject();

        if ( current == null )
            return;
        
        try {
            String filename = current.getFileName();
            File file = new File( filename );
            Project project = new Project(file.getPath());
            FileInputStream in = new FileInputStream(file);
            project.read(in);
            setProject( null );
            currentProject = project;
            findBugsButtonActionPerformed( evt );
        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.ERROR, "Could not reload project: " + e.getMessage());
        }

    }//GEN-LAST:event_reloadProjectItemActionPerformed

    private void saveProjectAsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectAsItemActionPerformed
        saveProject(getCurrentProject(), "Save project as", true);
    }//GEN-LAST:event_saveProjectAsItemActionPerformed
    
    private void viewMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_viewMenuMenuSelected
        // View bug details and full descriptions items
        // are only enabled if there is a project open.
        boolean hasProject = getCurrentProject() != null;
        viewBugDetailsItem.setEnabled(hasProject);
        fullDescriptionsItem.setEnabled(hasProject);
    }//GEN-LAST:event_viewMenuMenuSelected
    
    private void fileMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_fileMenuMenuSelected
        // Save and close project items are only enabled if there is a project open.
        boolean hasProject = getCurrentProject() != null;
        saveProjectItem.setEnabled(hasProject);
        saveProjectAsItem.setEnabled(hasProject);
	reloadProjectItem.setEnabled(hasProject);
        closeProjectItem.setEnabled(hasProject);
    }//GEN-LAST:event_fileMenuMenuSelected
    
    private void closeProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeProjectItemActionPerformed
        if (closeProjectHook(getCurrentProject(), "Close project")) {
            setProject(null);
        }
    }//GEN-LAST:event_closeProjectItemActionPerformed
    
    private void removeClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeClasspathEntryButtonActionPerformed
        int selIndex = classpathEntryList.getSelectedIndex();
        if (selIndex >= 0) {
            Project project = getCurrentProject();
            project.removeAuxClasspathEntry(selIndex);
            DefaultListModel listModel = (DefaultListModel) classpathEntryList.getModel();
            listModel.removeElementAt(selIndex);
        }
    }//GEN-LAST:event_removeClasspathEntryButtonActionPerformed
    
    private void addClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addClasspathEntryButtonActionPerformed
        addClasspathEntryToList();
    }//GEN-LAST:event_addClasspathEntryButtonActionPerformed
    
    private void browseClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseClasspathEntryButtonActionPerformed
        // Add your handling code here:
        JFileChooser chooser = new JFileChooser(currentDirectory);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(auxClasspathEntryFileFilter);
        chooser.setMultiSelectionEnabled(true);
        
        int result = chooser.showDialog(this, "Add entry");

        if (result != JFileChooser.CANCEL_OPTION) {
            File[] selectedFileList = chooser.getSelectedFiles();
            for (int i = 0; i < selectedFileList.length; ++i) {
                String entry = selectedFileList[i].getPath();
                addClasspathEntryToProject(entry);
            }
        }
    }//GEN-LAST:event_browseClasspathEntryButtonActionPerformed
    
    private void fullDescriptionsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullDescriptionsItemActionPerformed
        for (int j = 0; j < bugTreeList.length; ++j) {
            JTree bugTree = bugTreeList[j];
        
            // Redisplay the displayed bug instance nodes
            DefaultTreeModel bugTreeModel = (DefaultTreeModel) bugTree.getModel();
            int numRows = bugTree.getRowCount();
        
            for (int i = 0; i < numRows; ++i) {
                //System.out.println("Getting path for row " + i);
                TreePath path = bugTree.getPathForRow(i);
                if (path == null)
                    continue;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node instanceof BugTreeNode)
                    bugTreeModel.valueForPathChanged(path, node.getUserObject());
            }
        }
    }//GEN-LAST:event_fullDescriptionsItemActionPerformed
    
    private void viewBugDetailsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewBugDetailsItemActionPerformed
        String view = getView();
        if (view.equals("BugTree")) {
            checkBugDetailsVisibility();
        }
        
    }//GEN-LAST:event_viewBugDetailsItemActionPerformed
    
    private void bugTreeBugDetailsSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_bugTreeBugDetailsSplitterPropertyChange
        // Here we want to
        //   (1) Keep the View:Bug details checkbox item up to date, and
        //   (2) keep the details window synchronized with the current bug instance
        String propertyName = evt.getPropertyName();
        if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
            boolean isMaximized = isSplitterMaximized(bugTreeBugDetailsSplitter, evt);
            viewBugDetailsItem.setSelected(!isMaximized);
            if (!isMaximized) {
                // Details window is shown, so make sure it is populated
                // with bug detail information
                synchBugInstance();
            }
        }
    }//GEN-LAST:event_bugTreeBugDetailsSplitterPropertyChange
    
    private void openProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjectItemActionPerformed
        
        if (!closeProjectHook(getCurrentProject(), "Open Project"))
            return;
        
        JFileChooser chooser = new JFileChooser(currentDirectory);
        chooser.setFileFilter(projectFileFilter);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION)
            return;
        try {
            File file = chooser.getSelectedFile();
            currentDirectory = file.getParentFile();
            Project project = new Project(file.getPath());
            FileInputStream in = new FileInputStream(file);
            project.read(in);
            setProject(project);
        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.ERROR, "Could not open project: " + e.getMessage());
        }
    }//GEN-LAST:event_openProjectItemActionPerformed
    
    private void saveProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectItemActionPerformed
        saveProject(getCurrentProject(), "Save project");
    }//GEN-LAST:event_saveProjectItemActionPerformed
    
    private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
        AboutDialog dialog = new AboutDialog(this, true);
        dialog.setSize(500, 354);
        dialog.setLocationRelativeTo(null); // center the dialog
        dialog.show();
    }//GEN-LAST:event_aboutItemActionPerformed
    
    private void consoleSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_consoleSplitterPropertyChange
        // The idea here is to keep the View:Console checkbox up to date with
        // the real location of the divider of the consoleSplitter.
        // What we want is if any part of the console window is visible,
        // then the checkbox should be checked.
        String propertyName = evt.getPropertyName();
        if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
            boolean isMaximized = isSplitterMaximized(consoleSplitter, evt);
            viewConsoleItem.setSelected(!isMaximized);
        }
    }//GEN-LAST:event_consoleSplitterPropertyChange
    
    private void viewConsoleItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewConsoleItemActionPerformed
        if (viewConsoleItem.isSelected()) {
            consoleSplitter.resetToPreferredSizes();
        } else {
            consoleSplitter.setDividerLocation(1.0);
        }
    }//GEN-LAST:event_viewConsoleItemActionPerformed
    
    private void findBugsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findBugsButtonActionPerformed
        Project project = getCurrentProject();
        
        if (project.getNumJarFiles() == 0) {
            logger.logMessage(ConsoleLogger.ERROR, "Project " + project + " has no Jar files selected");
            return;
        }
        
        bugDescriptionEditorPane.setText("");
        sourceTextArea.setText("");
        AnalysisRun analysisRun = new AnalysisRun(project, logger);
        
        logger.logMessage(ConsoleLogger.INFO, "Beginning analysis of " + project);
        
        // Run the analysis!
        RunAnalysisDialog dialog = new RunAnalysisDialog(this, analysisRun);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null); // center the dialog
        dialog.show();
        
        if (dialog.isCompleted()) {
            logger.logMessage(ConsoleLogger.INFO, "Analysis " + project + " completed");
            
            // Now we have an analysis run to look at
            synchAnalysisRun(analysisRun);
            currentAnalysisRun = analysisRun;
            setView("BugTree");
        } else {
            logger.logMessage(ConsoleLogger.INFO, "Analysis of " + project + " cancelled by user");
        }
    }//GEN-LAST:event_findBugsButtonActionPerformed
    
    private void browseSrcDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSrcDirButtonActionPerformed
        JFileChooser chooser = new JFileChooser(currentDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rc = chooser.showDialog(this, "Add source directory");
        if (rc == JFileChooser.APPROVE_OPTION) {
            currentDirectory = chooser.getSelectedFile().getParentFile();
            srcDirTextField.setText(chooser.getSelectedFile().getPath());
            addSourceDirToList();
        }
    }//GEN-LAST:event_browseSrcDirButtonActionPerformed
    
    private void srcDirTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_srcDirTextFieldActionPerformed
        addSourceDirToList();
    }//GEN-LAST:event_srcDirTextFieldActionPerformed
    
    private void jarNameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jarNameTextFieldActionPerformed
        addJarToList();
    }//GEN-LAST:event_jarNameTextFieldActionPerformed
    
    private void browseJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseJarButtonActionPerformed
        JFileChooser chooser = new JFileChooser(currentDirectory);
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) { return file.isDirectory() || file.getName().endsWith(".jar"); }
            public String getDescription() { return "Jar files (*.jar)"; }
        };
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(true);
        
        int rc = chooser.showDialog(this, "Add Jar file");
        if (rc == JFileChooser.APPROVE_OPTION) {
            File[] selectedFileList = chooser.getSelectedFiles();
            for (int i = 0; i < selectedFileList.length; ++i) {
                String entry = selectedFileList[i].getPath();
                addJarToProject(entry);
            }
        }
    }//GEN-LAST:event_browseJarButtonActionPerformed
    
    private void newProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjectItemActionPerformed
        String projectName = "<<unnamed project>>";
        Project project = new Project(projectName);
        setProject(project);
    }//GEN-LAST:event_newProjectItemActionPerformed
    
    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
        exitFindBugs();
    }//GEN-LAST:event_exitItemActionPerformed
    
    private void removeSrcDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSrcDirButtonActionPerformed
        int selIndex = sourceDirList.getSelectedIndex();
        if (selIndex >= 0) {
            Project project = getCurrentProject();
            project.removeSourceDir(selIndex);
            DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();
            listModel.removeElementAt(selIndex);
        }
    }//GEN-LAST:event_removeSrcDirButtonActionPerformed
    
    private void removeJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJarButtonActionPerformed
        int selIndex = jarFileList.getSelectedIndex();
        if (selIndex >= 0) {
            Project project = getCurrentProject();
            project.removeJarFile(selIndex);
            DefaultListModel listModel = (DefaultListModel) jarFileList.getModel();
            listModel.removeElementAt(selIndex);
        }
    }//GEN-LAST:event_removeJarButtonActionPerformed
    
    private void addSourceDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSourceDirButtonActionPerformed
        addSourceDirToList();
    }//GEN-LAST:event_addSourceDirButtonActionPerformed
    
    private void addJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJarButtonActionPerformed
        addJarToList();
    }//GEN-LAST:event_addJarButtonActionPerformed
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        exitFindBugs();
    }//GEN-LAST:event_exitForm
    
    /**
     * This is called whenever the selection is changed in the bug tree.
     * @param e the TreeSelectionEvent
     */
    private void bugTreeSelectionChanged(TreeSelectionEvent e) {
        
        BugInstance selected = getCurrentBugInstance();
        if (selected != null) {
            synchBugInstance();
        }
    }
    
    /* ----------------------------------------------------------------------
     * Component initialization support
     * ---------------------------------------------------------------------- */
    
    /**
     * This is called from the constructor to perform post-initialization
     * of the components in the form.
     */
    private void postInitComponents() {
        logger = new ConsoleLogger(this);
        
        viewPanelLayout = (CardLayout) viewPanel.getLayout();
        
        // Console starts out disabled
        consoleSplitter.setDividerLocation(1.0);
        
        // List of bug group tabs.
        // This must be in the same order as GROUP_BY_ORDER_LIST!
        bugTreeList = new JTree[]{byClassBugTree, byPackageBugTree, byBugTypeBugTree};
        
        // Configure bug trees
        for (int i = 0; i < bugTreeList.length; ++i) {
            JTree bugTree = bugTreeList[i];
            bugTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            bugTree.setCellRenderer(bugCellRenderer);
            bugTree.setRootVisible(false);
            bugTree.setShowsRootHandles(true);
            bugTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    bugTreeSelectionChanged(e);
                }
            });
        }
        
        jarFileList.setModel(new DefaultListModel());
        sourceDirList.setModel(new DefaultListModel());
        classpathEntryList.setModel(new DefaultListModel());
        
        // We use a special highlight painter to ensure that the highlights cover
        // complete source lines, even though the source text doesn't
        // fill the lines completely.
        final Highlighter.HighlightPainter painter =
        new DefaultHighlighter.DefaultHighlightPainter(sourceTextArea.getSelectionColor()) {
            public Shape paintLayer(Graphics g, int offs0, int offs1,
            Shape bounds, JTextComponent c, View view) {
                try {
                    Shape extent = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                    Rectangle rect = extent.getBounds();
                    rect.x = 0;
                    rect.width = bounds.getBounds().width;
                    g.setColor(getColor());
                    g.fillRect(rect.x, rect.y, rect.width, rect.height);
                    return rect;
                } catch (BadLocationException e) {
                    return null;
                }
            }
        };
        Highlighter sourceHighlighter = new DefaultHighlighter() {
            public Object addHighlight(int p0, int p1, Highlighter.HighlightPainter p)
            throws BadLocationException {
                return super.addHighlight(p0, p1, painter);
            }
        };
        sourceTextArea.setHighlighter(sourceHighlighter);
        
        updateTitle(getCurrentProject());
    }
    
    /* ----------------------------------------------------------------------
     * Helpers for accessing and modifying UI components
     * ---------------------------------------------------------------------- */
    
    /**
     * Based on the current tree selection path, get a user object
     * whose class is the same as the given class.
     * @param tree the tree
     * @param c the class
     * @return an instance of the given kind of object which is in the
     *   current selection, or null if there is no matching object
     */
    private static Object getTreeSelectionOf(JTree tree, Class c) {
        TreePath selPath = tree.getSelectionPath();
        
        // There may not be anything selected at the moment
        if (selPath == null)
            return null;
        
        // Work backwards from end until we get to the kind of
        // object we're looking for.
        Object[] nodeList = selPath.getPath();
        for (int i = nodeList.length - 1; i >= 0; --i) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeList[i];
            Object nodeInfo = node.getUserObject();
            if (nodeInfo != null && nodeInfo.getClass() == c)
                return nodeInfo;
        }
        return null;
    }
    
    /**
     * Get the current project.
     */
    private Project getCurrentProject() {
        return currentProject;
    }
    
    /**
     * Get the current analysis run.
     */
    private AnalysisRun getCurrentAnalysisRun() {
        return currentAnalysisRun;
    }
    
    /**
     * Get the bug instance currently selected in the bug tree.
     */
    private BugInstance getCurrentBugInstance() {
        JTree bugTree = getCurrentBugTree();
        return (BugInstance) getTreeSelectionOf(bugTree, BugInstance.class);
    }
    
    /**
     * Return whether or not the given splitter is "maximized", meaning that
     * the top window of the split has been given all of the space.
     * Note that this window assumes that the split is vertical (meaning
     * that we have top and bottom components).
     * @param splitter the JSplitPane
     * @param evt the event that is changing the splitter value
     */
    private boolean isSplitterMaximized(JSplitPane splitter, java.beans.PropertyChangeEvent evt) {
        Integer location = (Integer) evt.getNewValue();
        
        java.awt.Container parent = splitter.getParent();
        int height = splitter.getHeight();
        int hopefullyMaxDivider = height - (splitter.getDividerSize() + DIVIDER_FUDGE);
        //System.out.println("Splitter: "+(splitter==consoleSplitter?"consoleSplitter":"bugTreeBugDetailsSplitter")+
        //    ": height="+height+",location="+location+
        //    ",hopefullyMax="+hopefullyMaxDivider);
        boolean isMaximized = location.intValue() >= hopefullyMaxDivider;
        return isMaximized;
    }
    
    private void checkBugDetailsVisibility() {
        if (viewBugDetailsItem.isSelected()) {
            bugTreeBugDetailsSplitter.resetToPreferredSizes();
        } else {
            bugTreeBugDetailsSplitter.setDividerLocation(1.0);
        }
        //System.out.("New bug detail splitter location " + bugTreeBugDetailsSplitter.getDividerLocation());
    }
    
    private JTree getCurrentBugTree() {
        JScrollPane selected = (JScrollPane) groupByTabbedPane.getSelectedComponent();
        JTree selectedTree = (JTree) selected.getViewport().getView();
        return selectedTree;
    }
    
    /* ----------------------------------------------------------------------
     * Synchronization of data model and UI
     * ---------------------------------------------------------------------- */
    
    private void setProject(Project project) {
        currentProject = project;
        if (project != null) {
            synchProject(project);
            setView("EditProjectPanel");
        } else {
            setView("EmptyPanel");
        }
        updateTitle(project);
    }
    
    private void updateTitle(Project project) {
        if (project == null)
            this.setTitle("FindBugs - no project");
        else
            this.setTitle("FindBugs - " + project.toString());
    }
    
    /**
     * Save given project.
     * If the project already has a valid filename, use that filename.
     * Otherwise, prompt for one.
     * @param project the Project to save
     * @param dialogTitle the title for the save dialog (if needed)
     */
    private boolean saveProject(Project project, String dialogTitle) {
        return saveProject(project, dialogTitle, false);
    }
    
    /**
     * Offer to save the current Project to a file.
     * @param project the Project to save
     * @param dialogTitle the title for the save dialog (if needed)
     * @param chooseFilename if true, force a dialog to prompt the user
     *   for a filename
     * @return true if the project is saved successfully, false if the user
     *   cancels or an error occurs
     */
    private boolean saveProject(Project project, String dialogTitle, boolean chooseFilename) {
        try {
            if (project == null)
                return true;
            
            File file;
            String fileName = project.getFileName();
            
            if (!fileName.startsWith("<") && !chooseFilename) {
                file = new File(fileName);
            } else {
                JFileChooser chooser = new JFileChooser(currentDirectory);
                chooser.setFileFilter(projectFileFilter);
                
                int result = chooser.showDialog(this, dialogTitle);
                if (result == JFileChooser.CANCEL_OPTION)
                    return false;
                file = chooser.getSelectedFile();
                fileName = Project.transformFilename(file.getPath());
                file = new File(fileName);
            }
            
            FileOutputStream out = new FileOutputStream(file);
            project.write(out);
            logger.logMessage(ConsoleLogger.INFO, "Project saved");
            project.setFileName(file.getPath());
            
            updateTitle(project);
            
            return true;
        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.ERROR, "Could not save project: " + e.toString());
            JOptionPane.showMessageDialog(this, "Error saving project: " + e.toString(),
            "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    /**
     * Hook to call before closing a project.
     * @param project the project being closed
     * @param savePromptTitle title to use for the "Save project?" dialog
     * @return true if user has confirmed that the project should be closed,
     *   false if the close is cancelled
     */
    private boolean closeProjectHook(Project project, String savePromptTitle) {
        if (project == null || !project.isModified())
            return true;
        
        // Confirm that the project should be closed.
        int option = JOptionPane.showConfirmDialog(this, "Save project?", savePromptTitle,
        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (option == JOptionPane.CANCEL_OPTION)
            return false;
        else if (option == JOptionPane.YES_OPTION) {
            boolean result = saveProject(project, "Save project");
            if (result)
                JOptionPane.showMessageDialog(this, "Project saved");
            return result;
        } else
            return true;
    }
    
    /**
     * Synchronize the edit project dialog with given project.
     * @param project the selected project
     */
    private void synchProject(Project project) {
        // Clear text fields
        jarNameTextField.setText("");
        srcDirTextField.setText("");
        classpathEntryTextField.setText("");
        
        // Populate jar file, source directory, and aux classpath entry lists
        
        DefaultListModel jarListModel = (DefaultListModel) jarFileList.getModel();
        jarListModel.clear();
        for (int i = 0; i < project.getNumJarFiles(); ++i) {
            jarListModel.addElement(project.getJarFile(i));
        }
        
        DefaultListModel srcDirListModel = (DefaultListModel) sourceDirList.getModel();
        srcDirListModel.clear();
        for (int i = 0; i < project.getNumSourceDirs(); ++i) {
            srcDirListModel.addElement(project.getSourceDir(i));
        }
        
        DefaultListModel classpathEntryListModel = (DefaultListModel) classpathEntryList.getModel();
        classpathEntryListModel.clear();
        for (int i = 0; i < project.getNumAuxClasspathEntries(); ++i) {
            classpathEntryListModel.addElement(project.getAuxClasspathEntry(i));
        }
    }
    
    /**
     * Synchronize the bug trees with the given analysisRun object.
     * @param analysisRun the selected analysis run
     */
    private void synchAnalysisRun(AnalysisRun analysisRun) {
        // Create and populate tree models
        for (int i = 0; i < GROUP_BY_ORDER_LIST.length; ++i) {
            DefaultMutableTreeNode bugRootNode = new DefaultMutableTreeNode();
            DefaultTreeModel bugTreeModel = new DefaultTreeModel(bugRootNode);
            
            String groupByOrder = GROUP_BY_ORDER_LIST[i];
            analysisRun.setTreeModel(groupByOrder, bugTreeModel);
            populateAnalysisRunTreeModel(analysisRun, groupByOrder);
            if (i < bugTreeList.length)
                bugTreeList[i].setModel(bugTreeModel);
        }
    }
    
    /**
     * Populate an analysis run's tree model for given sort order.
     */
    private void populateAnalysisRunTreeModel(AnalysisRun analysisRun, final String groupBy) {
        //System.out.println("Populating bug tree for order " + groupBy);
        
        // Set busy cursor - this is potentially a time-consuming operation
        Cursor orig = this.getCursor();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        final DefaultTreeModel bugTreeModel = analysisRun.getTreeModel(groupBy);
        final DefaultMutableTreeNode bugRootNode = (DefaultMutableTreeNode) bugTreeModel.getRoot();
        
        // Delete all children from root node
        bugRootNode.removeAllChildren();
        
        // Sort the instances
        TreeSet<BugInstance> sortedCollection = new TreeSet<BugInstance>(getBugInstanceComparator(groupBy));
        sortedCollection.addAll(analysisRun.getBugInstances());
        
        // The grouper callback is what actually adds the group and bug
        // nodes to the tree.
        Grouper.Callback<BugInstance> callback = new Grouper.Callback<BugInstance>() {
            private BugInstanceGroup currentGroup;
            private DefaultMutableTreeNode currentGroupNode;
            
            public void startGroup(BugInstance member) {
                String groupName;
                if (groupBy == GROUP_BY_CLASS)
                    groupName = member.getPrimaryClass().getClassName();
                else if (groupBy == GROUP_BY_PACKAGE) {
                    groupName = member.getPrimaryClass().getPackageName();
                    if (groupName.equals(""))
                        groupName = "Unnamed package";
                } else if (groupBy == GROUP_BY_BUG_TYPE) {
                    String desc = member.toString();
                    String shortBugType = desc.substring(0, desc.indexOf(':'));
                    String bugTypeDescription = I18N.instance().getBugTypeDescription(shortBugType);
                    groupName = shortBugType + ": " + bugTypeDescription;
                } else
                    throw new IllegalStateException("Unknown sort order: " + groupBy);
                currentGroup = new BugInstanceGroup(groupBy, groupName);
                currentGroupNode = new DefaultMutableTreeNode(currentGroup);
                bugTreeModel.insertNodeInto(currentGroupNode, bugRootNode, bugRootNode.getChildCount());
                
                insertIntoGroup(member);
            }
            
            public void addToGroup(BugInstance member) {
                insertIntoGroup(member);
            }
            
            private void insertIntoGroup(BugInstance member) {
                currentGroup.incrementMemberCount();
                DefaultMutableTreeNode bugNode = new BugTreeNode(member);
                bugTreeModel.insertNodeInto(bugNode, currentGroupNode, currentGroupNode.getChildCount());
                
                // Insert annotations
                Iterator j = member.annotationIterator();
                while (j.hasNext()) {
                    BugAnnotation annotation = (BugAnnotation) j.next();
                    DefaultMutableTreeNode annotationNode = new DefaultMutableTreeNode(annotation);
                    bugTreeModel.insertNodeInto(annotationNode, bugNode,  bugNode.getChildCount());
                }
                
            }
        };
        
        // Create the grouper, and execute it to populate the bug tree
        Grouper<BugInstance> grouper = new Grouper<BugInstance>(callback);
        Comparator<BugInstance> groupComparator = getGroupComparator(groupBy);
        grouper.group(sortedCollection, groupComparator);
        
        // Let the tree know it needs to update itself
        bugTreeModel.nodeStructureChanged(bugRootNode);
        
        // Now we're done
        this.setCursor(orig);
    }
    
    /**
     * Get a BugInstance Comparator for given sort order.
     */
    private Comparator<BugInstance> getBugInstanceComparator(String sortOrder) {
        if (sortOrder.equals(GROUP_BY_CLASS))
            return bugInstanceByClassComparator;
        else if (sortOrder.equals(GROUP_BY_PACKAGE))
            return bugInstanceByPackageComparator;
        else if (sortOrder.equals(GROUP_BY_BUG_TYPE))
            return bugInstanceByTypeComparator;
        else
            throw new IllegalArgumentException("Bad sort order: " + sortOrder);
    }
    
    /**
     * Get a Grouper for a given sort order.
     */
    private Comparator<BugInstance> getGroupComparator(String groupBy) {
        if (groupBy.equals(GROUP_BY_CLASS)) {
            return bugInstanceClassComparator;
        } else if (groupBy.equals(GROUP_BY_PACKAGE)) {
            return bugInstancePackageComparator;
        } else if (groupBy.equals(GROUP_BY_BUG_TYPE)) {
            return bugInstanceTypeComparator;
        } else
            throw new IllegalArgumentException("Bad sort order: " + groupBy);
    }
    
    /**
     * Set the view panel to display the named view.
     */
    private void setView(String viewName) {
        //System.out.println("Showing view " + viewName);
        viewPanelLayout.show(viewPanel, viewName);
        if (viewName.equals("BugTree"))
            checkBugDetailsVisibility();
        currentView = viewName;
    }
    
    /**
     * Get which view is displayed currently.
     */
    private String getView() {
        return currentView;
    }
    
    /**
     * Called to add the jar file in the jarNameTextField to the
     * Jar file list (and the project it represents).
     */
    private void addJarToList() {
        String jarFile = jarNameTextField.getText();
        if (!jarFile.equals("")) {
            addJarToProject(jarFile);
            jarNameTextField.setText("");
        }
    }
    
    /**
     * Add a Jar file to the current project.
     * @param jarFile the jar file to add to the project
     */
    private void addJarToProject(String jarFile) {
        Project project = getCurrentProject();
        if (project.addJar(jarFile)) {
            DefaultListModel listModel = (DefaultListModel)  jarFileList.getModel();
            listModel.addElement(jarFile);
        }
    }
    
    /**
     * Called to add the source directory in the sourceDirTextField
     * to the source directory list (and the project it represents).
     */
    private void addSourceDirToList() {
        String sourceDir = srcDirTextField.getText();
        if (!sourceDir.equals("")) {
            Project project = getCurrentProject();
            if (project.addSourceDir(sourceDir)) {
                DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();
                listModel.addElement(sourceDir);
            }
            srcDirTextField.setText("");
        }
    }
    
    /**
     * Called to add the classpath entry in the classpathEntryTextField
     * to the classpath entry list (and the project it represents).
     */
    private void addClasspathEntryToList() {
        String classpathEntry = classpathEntryTextField.getText();
        if (!classpathEntry.equals("")) {
            addClasspathEntryToProject(classpathEntry);
            classpathEntryTextField.setText("");
        }
    }
    
    /**
     * Add a classpath entry to the current project.
     * @param classpathEntry the classpath entry to add
     */
    private void addClasspathEntryToProject(String classpathEntry) {
        Project project = getCurrentProject();
        if (project.addAuxClasspathEntry(classpathEntry)) {
            DefaultListModel listModel = (DefaultListModel) classpathEntryList.getModel();
            listModel.addElement(classpathEntry);
        }
    }
    
    /**
     * Synchronize current bug instance with the bug detail
     * window (source view, details window, etc.)
     */
    private void synchBugInstance() {
        // Get current bug instance
        BugInstance selected = getCurrentBugInstance();
        if (selected == null)
            return;
        
        // If the details window is minimized, then the user can't see
        // it and there is no point in updating it.
        if (!viewBugDetailsItem.isSelected())
            return;
        
        // Get the current source line annotation.
        // If the current leaf selected is not a source line annotation,
        // use the default source line annotation from the current bug instance
        // (if any).
        JTree bugTree = getCurrentBugTree();
        SourceLineAnnotation srcLine = null;
        TreePath selPath = bugTree.getSelectionPath();
        if (selPath != null) {
            Object leaf = ((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
            if (leaf instanceof SourceLineAnnotation)
                srcLine = (SourceLineAnnotation) leaf;
            else
                srcLine = selected.getPrimarySourceLineAnnotation();
        }
        
        // Show source code.
        if (srcLine != currentSourceLineAnnotation) {
            Project project = getCurrentProject();
            AnalysisRun analysisRun = getCurrentAnalysisRun();
            if (project == null) throw new IllegalStateException("null project!");
            if (analysisRun == null) throw new IllegalStateException("null analysis run!");
            viewSource(project, analysisRun, srcLine);
            
            currentSourceLineAnnotation = srcLine;
        }
        
        // Show bug info.
        showBugInfo(selected);
        
        // Now the bug details are up to date.
        currentBugInstance = selected;
    }
    
    private static final int SELECTION_VOFFSET = 2;
    
    /**
     * Update the source view window.
     * @param project the project (containing the source directories to search)
     * @param analysisRun the analysis run (containing the mapping of classes to source files)
     * @param srcLine the source line annotation (specifying source file to load and
     *    which lines to highlight)
     */
    private void viewSource(Project project, AnalysisRun analysisRun, final SourceLineAnnotation srcLine) {
        // Get rid of old source code text
        sourceTextArea.setText("");
        
        // There is nothing to do without a source annotation
        // TODO: actually, might want to put a message in the source window
        // explaining that we don't have the source file, and that
        // they might want to recompile with debugging info turned on.
        if (srcLine == null)
            return;
        
        // Look up the source file for this class.
        sourceFinder.setSourceBaseList(project.getSourceDirList());
        String sourceFile = analysisRun.getSourceFile(srcLine.getClassName());
        if (sourceFile == null || sourceFile.equals("<Unknown>")) {
            logger.logMessage(ConsoleLogger.WARNING, "No source file for class " + srcLine.getClassName());
            return;
        }
        
        // Try to open the source file and display its contents
        // in the source text area.
        try {
            InputStream in = sourceFinder.openSource(srcLine.getPackageName(), sourceFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            String line;
            while ((line = reader.readLine()) != null) {
                sourceTextArea.append(line + "\n");
            }
            
            reader.close();
        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.WARNING, e.getMessage());
            return;
        }

        // Highlight the annotation.
        // There seems to be some bug in Swing that sometimes prevents this code
        // from working when executed immediately after populating the
        // text in the text area.  My guess is that when a large amount of text
        // is added, Swing defers some UI update work until "later" that is needed
        // to compute the visibility of text in the text area.
        // So, post some code to do the update to the Swing event queue.
        // Not really an ideal solution, but it seems to work.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Highlight the lines from the source annotation.
                // Note that the source lines start at 1, while the line numbers
                // in the text area start at 0.
                try {
                    int startLine = srcLine.getStartLine() - 1;
                    int endLine = srcLine.getEndLine();
                    
                    // Scroll the window so the annotation text will be SELECTION_VOFFSET
                    // lines from the top.
                    int viewLine = Math.max(startLine - SELECTION_VOFFSET, 0);
                    int viewBegin = sourceTextArea.getLineStartOffset(viewLine);
                    //sourceTextArea.scrollRectToVisible(sourceTextArea.modelToView(viewBegin));
                    Rectangle viewRect = sourceTextArea.modelToView(viewBegin);
                    sourceTextAreaScrollPane.getViewport().setViewPosition(new Point(viewRect.x, viewRect.y));
                    
                    // Select (and highlight) the annotation.
                    int selBegin = sourceTextArea.getLineStartOffset(startLine);
                    int selEnd = sourceTextArea.getLineStartOffset(endLine);
                    sourceTextArea.select(selBegin, selEnd);
                    sourceTextArea.getCaret().setSelectionVisible(true);
                } catch (javax.swing.text.BadLocationException e) {
                    logger.logMessage(ConsoleLogger.ERROR, e.toString());
                }
            }
        });
        
    }
    
    /**
     * Show descriptive text about the type of bug
     * @param bugInstance the bug instance
     */
    private void showBugInfo(BugInstance bugInstance) {
	// Are we already showing details for this kind of bug?
	String bugDetailsKey = bugInstance.getType();
	if (bugDetailsKey.equals(currentBugDetailsKey))
	    return;
        
	// Display the details
	String html = I18N.instance().getDetailHTML(bugDetailsKey);
	bugDescriptionEditorPane.setContentType("text/html");
	bugDescriptionEditorPane.setText(html);
	currentBugDetailsKey = bugDetailsKey;

	// FIXME: unfortunately, using setText() on the editor pane
	// results in the contents being scrolled to the bottom of the pane.
	// An immediate inline call to set the scroll position does nothing.
	// So, use invokeLater(), even though this results in flashing.
	// [What we really need is a way to set the text WITHOUT changing
	// the caret position.  Need to investigate.]
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		bugDescriptionScrollPane.getViewport().setViewPosition(new Point(0, 0));
	    }
	});
    }
    
    /* ----------------------------------------------------------------------
     * Misc. helpers
     * ---------------------------------------------------------------------- */
    
    /**
     * Exit the application.
     */
    private void exitFindBugs() {
        // TODO: offer to save work, etc.
        System.exit(0);
    }
    
    /**
     * Get the ConsoleLogger.
     */
    public ConsoleLogger getLogger() {
        return logger;
    }
    
    /**
     * Show an error dialog.
     */
    public void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Write a message to the console window.
     */
    public void writeToConsole(String message) {
        consoleMessageArea.append(message);
        consoleMessageArea.append("\n");
    }
    
    /* ----------------------------------------------------------------------
     * main() method
     * ---------------------------------------------------------------------- */
    
    /**
     * Invoke from the command line.
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            
            if (arg.equals("-betterCFG")) {
                // FIXME: this is for testing of BetterCFGBuilder
                System.out.println("Using BetterCFGBuilder");
                System.setProperty("cfg.better", "true");
            } else if (arg.equals("-debug")) {
                System.out.println("Setting findbugs.debug=true");
                System.setProperty("findbugs.debug", "true");
            } else if (arg.equals("-plastic")) {
		// You can get the Plastic look and feel from jgoodies.com:
		//    http://www.jgoodies.com/downloads/libraries.html
		// Just put "plastic.jar" in the lib directory, right next
		// to the other jar files.
		try {
		    UIManager.setLookAndFeel("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
		} catch (Exception e) {
		    System.err.println("Couldn't load plastic look and feel: " + e.toString());
		}
	    }
        
        }
        
        FindBugsFrame frame = new FindBugsFrame();
        frame.setSize(800, 600);
        frame.show();
    }
    
    /* ----------------------------------------------------------------------
     * Instance variables
     * ---------------------------------------------------------------------- */
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JButton addClasspathEntryButton;
    private javax.swing.JButton addJarButton;
    private javax.swing.JButton addSourceDirButton;
    private javax.swing.JButton browseClasspathEntryButton;
    private javax.swing.JButton browseJarButton;
    private javax.swing.JButton browseSrcDirButton;
    private javax.swing.JEditorPane bugDescriptionEditorPane;
    private javax.swing.JScrollPane bugDescriptionScrollPane;
    private javax.swing.JTabbedPane bugDetailsTabbedPane;
    private javax.swing.JSplitPane bugTreeBugDetailsSplitter;
    private javax.swing.JPanel bugTreePanel;
    private javax.swing.JTree byBugTypeBugTree;
    private javax.swing.JScrollPane byBugTypeScrollPane;
    private javax.swing.JTree byClassBugTree;
    private javax.swing.JScrollPane byClassScrollPane;
    private javax.swing.JTree byPackageBugTree;
    private javax.swing.JScrollPane byPackageScrollPane;
    private javax.swing.JLabel classpathEntryLabel;
    private javax.swing.JList classpathEntryList;
    private javax.swing.JLabel classpathEntryListLabel;
    private javax.swing.JScrollPane classpathEntryListScrollPane;
    private javax.swing.JTextField classpathEntryTextField;
    private javax.swing.JMenuItem closeProjectItem;
    private javax.swing.JMenuItem configureDetectorsItem;
    private javax.swing.JTextArea consoleMessageArea;
    private javax.swing.JScrollPane consoleScrollPane;
    private javax.swing.JSplitPane consoleSplitter;
    private javax.swing.JLabel editProjectLabel;
    private javax.swing.JPanel editProjectPanel;
    private javax.swing.JPanel emptyPanel;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JButton findBugsButton;
    private javax.swing.JCheckBoxMenuItem fullDescriptionsItem;
    private javax.swing.JTabbedPane groupByTabbedPane;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JLabel jarFileLabel;
    private javax.swing.JList jarFileList;
    private javax.swing.JLabel jarFileListLabel;
    private javax.swing.JScrollPane jarFileListScrollPane;
    private javax.swing.JTextField jarNameTextField;
    private javax.swing.JMenuItem newProjectItem;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JMenuItem reloadProjectItem;
    private javax.swing.JButton removeClasspathEntryButton;
    private javax.swing.JButton removeJarButton;
    private javax.swing.JButton removeSrcDirButton;
    private javax.swing.JPanel reportPanel;
    private javax.swing.JMenuItem saveProjectAsItem;
    private javax.swing.JMenuItem saveProjectItem;
    private javax.swing.JLabel sourceDirLabel;
    private javax.swing.JList sourceDirList;
    private javax.swing.JLabel sourceDirListLabel;
    private javax.swing.JScrollPane sourceDirListScrollPane;
    private javax.swing.JTextArea sourceTextArea;
    private javax.swing.JScrollPane sourceTextAreaScrollPane;
    private javax.swing.JTextField srcDirTextField;
    private javax.swing.JMenuBar theMenuBar;
    private javax.swing.JCheckBoxMenuItem viewBugDetailsItem;
    private javax.swing.JCheckBoxMenuItem viewConsoleItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JPanel viewPanel;
    // End of variables declaration//GEN-END:variables
    
    // My variable declarations
    private ConsoleLogger logger;
    private CardLayout viewPanelLayout;
    private String currentView;
    private File currentDirectory;
    private Project currentProject;
    private JTree[] bugTreeList;
    private AnalysisRun currentAnalysisRun;
    private SourceFinder sourceFinder = new SourceFinder();
    private BugInstance currentBugInstance; // be lazy in switching bug instance details
    private SourceLineAnnotation currentSourceLineAnnotation; // as above
    private String currentBugDetailsKey;
}
