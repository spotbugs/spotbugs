/*
 * FindBugsFrame.java
 *
 * Created on March 30, 2003, 12:05 PM
 */

package edu.umd.cs.findbugs.gui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import edu.umd.cs.findbugs.*;

/**
 * The main GUI frame for FindBugs.
 *
 * @author David Hovemeyer
 */
public class FindBugsFrame extends javax.swing.JFrame {
    
    /**
     * Custom cell renderer for the navigator tree.
     */
    private static class NavigatorCellRenderer extends DefaultTreeCellRenderer {
        private ImageIcon projectIcon;
        private ImageIcon analysisRunIcon;

        public NavigatorCellRenderer() {
            ClassLoader classLoader = this.getClass().getClassLoader();
            projectIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/project.png"));
            analysisRunIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/execute.png"));
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
             boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // Set the icon, depending on what kind of node it is
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object obj = node.getUserObject();
            if (obj instanceof Project) {
                setIcon(projectIcon);
            } else if (obj instanceof AnalysisRun) {
                setIcon(analysisRunIcon);
            }

            return this;
        }
    }

    /**
     * Custom cell renderer for the bug tree.
     */
    private static class BugCellRenderer extends DefaultTreeCellRenderer {
        private ImageIcon bugIcon;
        private ImageIcon classIcon;
        private ImageIcon methodIcon;
        private ImageIcon fieldIcon;
        
        public BugCellRenderer() {
            ClassLoader classLoader = this.getClass().getClassLoader();
            bugIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug2.png"));
            classIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/class.png"));
            methodIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/method.png"));
            fieldIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/field.png"));
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
            } else {
                setIcon(null);
            }
            
            return this;
        }
    }
    
    /** Compare BugInstances by class name. */
    private static class BugInstanceByClassComparator implements Comparator {
        public int compare(Object a, Object b) {
            BugInstance lhs = (BugInstance) a;
            BugInstance rhs = (BugInstance) b;
            int cmp = lhs.getPrimaryClass().getClassName().compareTo(rhs.getPrimaryClass().getClassName());
            if (cmp != 0)
                return cmp;
            return lhs.compareTo(rhs);
        }
    }
    private static Comparator bugInstanceByClassComparator = new FindBugsFrame.BugInstanceByClassComparator();
    
    /** Compare BugInstances by package name. */
    private static class BugInstanceByPackageComparator implements Comparator {
        public int compare(Object a, Object b) {
            BugInstance lhs = (BugInstance) a;
            BugInstance rhs = (BugInstance) b;
            int cmp = lhs.getPrimaryClass().getPackageName().compareTo(rhs.getPrimaryClass().getPackageName());
            if (cmp != 0)
                return cmp;
            return lhs.compareTo(rhs);
        }
    }
    private static Comparator bugInstanceByPackageComparator = new FindBugsFrame.BugInstanceByPackageComparator();
    
    private static class BugInstanceByCategoryComparator implements Comparator {
        public int compare(Object a, Object b) {
            BugInstance lhs = (BugInstance) a;
            BugInstance rhs = (BugInstance) b;
            // FIXME: we're just sorting them by type, sort of.
            // Need to do something more intelligent here.
            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            int cmp = lhsString.substring(0, lhsString.indexOf(':')).compareTo(rhsString.substring(0, rhsString.indexOf(':')));
            if (cmp != 0)
                return cmp;
            return lhs.compareTo(rhs);
        }
    }
    private static Comparator bugInstanceByCategoryComparator = new FindBugsFrame.BugInstanceByCategoryComparator();

    private static final String BY_CLASS = "By class";
    private static final String BY_PACKAGE = "By package";
    private static final String BY_CATEGORY = "By category";
    
    /** Creates new form FindBugsFrame */
    public FindBugsFrame() {
	initComponents();
        postInitComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        consoleSplitter = new javax.swing.JSplitPane();
        navigatorViewSplitter = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        navigatorTree = new javax.swing.JTree();
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
        jScrollPane2 = new javax.swing.JScrollPane();
        jarFileList = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        sourceDirList = new javax.swing.JList();
        bugTreePanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        bugTree = new javax.swing.JTree();
        sortOrderChooser = new javax.swing.JComboBox();
        sortOrderLabel = new javax.swing.JLabel();
        leftFiller = new javax.swing.JLabel();
        rightFiller = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        consoleMessageArea = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newProjectItem = new javax.swing.JMenuItem();
        openProjectItem = new javax.swing.JMenuItem();
        closeProjectItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        exitItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        viewConsoleItem = new javax.swing.JCheckBoxMenuItem();

        setTitle("FindBugs");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
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

        navigatorViewSplitter.setEnabled(false);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(200, 0));
        navigatorTree.setModel(createNavigatorTreeModel());
        jScrollPane1.setViewportView(navigatorTree);

        navigatorViewSplitter.setLeftComponent(jScrollPane1);

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
        sourceDirLabel.setText("Source Dir:");
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
        sourceDirListLabel.setText("Source Dirs:");
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

        browseJarButton.setText("...");
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

        browseSrcDirButton.setText("...");
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

        findBugsButton.setText("Find Bugs!");
        findBugsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findBugsButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
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

        jScrollPane2.setPreferredSize(new java.awt.Dimension(259, 1));
        jarFileList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jarFileList.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jarFileList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.7;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jScrollPane2, gridBagConstraints);

        jScrollPane3.setPreferredSize(new java.awt.Dimension(259, 1));
        sourceDirList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        sourceDirList.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(sourceDirList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jScrollPane3, gridBagConstraints);

        viewPanel.add(editProjectPanel, "EditProjectPanel");

        bugTreePanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane4.setViewportView(bugTree);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bugTreePanel.add(jScrollPane4, gridBagConstraints);

        sortOrderChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortOrderChooserActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bugTreePanel.add(sortOrderChooser, gridBagConstraints);

        sortOrderLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        sortOrderLabel.setText("Sort order:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        bugTreePanel.add(sortOrderLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        bugTreePanel.add(leftFiller, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        bugTreePanel.add(rightFiller, gridBagConstraints);

        viewPanel.add(bugTreePanel, "BugTree");

        navigatorViewSplitter.setRightComponent(viewPanel);

        consoleSplitter.setTopComponent(navigatorViewSplitter);

        jScrollPane5.setMinimumSize(new java.awt.Dimension(22, 100));
        jScrollPane5.setPreferredSize(new java.awt.Dimension(0, 100));
        consoleMessageArea.setBackground(new java.awt.Color(204, 204, 204));
        consoleMessageArea.setEditable(false);
        consoleMessageArea.setFont(new java.awt.Font("Courier", 0, 12));
        consoleMessageArea.setMinimumSize(new java.awt.Dimension(0, 0));
        consoleMessageArea.setPreferredSize(new java.awt.Dimension(0, 5));
        jScrollPane5.setViewportView(consoleMessageArea);

        consoleSplitter.setBottomComponent(jScrollPane5);

        getContentPane().add(consoleSplitter, java.awt.BorderLayout.CENTER);

        jMenuBar1.setFont(new java.awt.Font("Dialog", 0, 12));
        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileMenu.setFont(new java.awt.Font("Dialog", 0, 12));
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
        fileMenu.add(openProjectItem);

        closeProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
        closeProjectItem.setMnemonic('C');
        closeProjectItem.setText("Close Project");
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

        jMenuBar1.add(fileMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");
        viewMenu.setToolTipText("null");
        viewMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        viewConsoleItem.setMnemonic('C');
        viewConsoleItem.setSelected(true);
        viewConsoleItem.setText("Console");
        viewConsoleItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewConsoleItemActionPerformed(evt);
            }
        });

        viewMenu.add(viewConsoleItem);

        jMenuBar1.add(viewMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }//GEN-END:initComponents
    
    /**
     * A fudge value required in our hack to get the REAL maximum
     * divider location for the consoleSplitter.  Experience suggests that
     * the value "1" would work here, but setting it a little higher
     * makes the code a bit more robust.
     */
    private static final int DIVIDER_FUDGE = 3;

    private void consoleSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_consoleSplitterPropertyChange
        // The idea here is to keep the View:Console checkbox up to date with
        // the real location of the divider of the consoleSplitter.
        // What we want is if any part of the console window is visible,
        // then the checkbox should be checked.
        String propertyName = evt.getPropertyName();
        if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
            Integer location = (Integer) evt.getNewValue();
            /*
            if (location.intValue() > consoleSplitter.getMaximumDividerLocation())
                throw new IllegalStateException("JSplitPane is stupid");
            viewConsoleItem.setSelected(location.intValue() != consoleSplitter.getMaximumDividerLocation());
             */
            // FIXME - I need to find out the REAL maximum divider value.
            // getMaximumDividerLocation() is based on minimum component sizes,
            // but it may be violated if the user clicks the little "contracter"
            // button put in place when the "one touch expandable" property was set.
            // Here is a nasty hack which makes a guess based on the current size
            // of the frame's content pane.
            int contentPaneHeight = this.getContentPane().getHeight();
            int hopefullyMaxDivider = contentPaneHeight - (consoleSplitter.getDividerSize() + DIVIDER_FUDGE);
/*
            System.out.println("pane height = " + contentPaneHeight + ", dividerLoc=" + location.intValue() +
                ", hopefullyMaxDivider=" + hopefullyMaxDivider);
 */
            
            viewConsoleItem.setSelected(location.intValue() < hopefullyMaxDivider);
        }
    }//GEN-LAST:event_consoleSplitterPropertyChange

    private void viewConsoleItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewConsoleItemActionPerformed
        if (viewConsoleItem.isSelected()) {
            consoleSplitter.resetToPreferredSizes();
        } else {
            consoleSplitter.setDividerLocation(1.0);
        }
    }//GEN-LAST:event_viewConsoleItemActionPerformed

    private void sortOrderChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortOrderChooserActionPerformed
        String selection = sortOrderChooser.getSelectedItem().toString();
        if (selection != null && currentAnalysisRun != null)
            populateAnalysisRunTreeModel(currentAnalysisRun, selection);
    }//GEN-LAST:event_sortOrderChooserActionPerformed

    private void findBugsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findBugsButtonActionPerformed
        Project project = getCurrentProject();
        AnalysisRun analysisRun = new AnalysisRun(project, logger);
        
        logger.logMessage(ConsoleLogger.INFO, "Beginning analysis of " + project);
        
        // Run the analysis!
        RunAnalysisDialog dialog = new RunAnalysisDialog(this, analysisRun);
        dialog.setSize(400, 300);
	dialog.setLocationRelativeTo(null); // center the dialog
        dialog.show();

        if (dialog.isCompleted()) {
            logger.logMessage(ConsoleLogger.INFO, "Analysis " + project + " completed");
            
            // Create a navigator tree node for the analysis run
            DefaultTreeModel treeModel = (DefaultTreeModel) navigatorTree.getModel();
            TreePath treePath = navigatorTree.getSelectionPath();
            DefaultMutableTreeNode projectNode = (DefaultMutableTreeNode) treePath.getPath()[1];
            DefaultMutableTreeNode analysisRunNode = new DefaultMutableTreeNode(analysisRun);
            treeModel.insertNodeInto(analysisRunNode, projectNode, projectNode.getChildCount());
            
            // Make the new node the currently selected node
            TreePath path = new TreePath(new Object[]{rootNode, projectNode, analysisRunNode});
            navigatorTree.makeVisible(path);
            navigatorTree.setSelectionPath(path);
        } else {
            logger.logMessage(ConsoleLogger.INFO, "Analysis of " + project + " cancelled by user");
        }
    }//GEN-LAST:event_findBugsButtonActionPerformed

    private void browseSrcDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSrcDirButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int rc = chooser.showDialog(this, "Add source directory");
        if (rc == JFileChooser.APPROVE_OPTION) {
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
        JFileChooser chooser = new JFileChooser();
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) { return file.isDirectory() || file.getName().endsWith(".jar"); }
            public String getDescription() { return "Jar files (*.jar)"; }
        };
        chooser.setFileFilter(filter);
        int rc = chooser.showDialog(this, "Add Jar file");
        if (rc == JFileChooser.APPROVE_OPTION) {
            jarNameTextField.setText(chooser.getSelectedFile().getPath());
	    addJarToList();
	}
    }//GEN-LAST:event_browseJarButtonActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
	navigatorTree.setSelectionPath(new TreePath(rootNode));
    }//GEN-LAST:event_formWindowOpened

    private void newProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjectItemActionPerformed
        String projectName = "<<project " + (++projectCount) + ">>";
        System.out.println("Adding " + projectName);
        Project project = new Project(projectName);
        projectCollection.addProject(project);
        DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
        DefaultTreeModel treeModel = (DefaultTreeModel) navigatorTree.getModel();
        treeModel.insertNodeInto(projectNode, rootNode, rootNode.getChildCount());
        TreePath projPath = new TreePath(new Object[]{rootNode, projectNode});
        navigatorTree.makeVisible(projPath);
        navigatorTree.setSelectionPath(projPath);
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
     * Create the tree model that will be used by the navigator tree.
     */
    private TreeModel createNavigatorTreeModel() {
        projectCollection = new ProjectCollection();
        rootNode = new DefaultMutableTreeNode(projectCollection);
        navigatorTreeModel = new DefaultTreeModel(rootNode);
        return navigatorTreeModel;
    }

    /**
     * This is called from the constructor to perform post-initialization
     * of the components in the form.
     */
    private void postInitComponents() {
        logger = new ConsoleLogger(this);
        
        viewPanelLayout = (CardLayout) viewPanel.getLayout();
        navigatorTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Add a tree selection listener to the navigator tree, so we can
        // ensure that the view is always consistent with the current selection.
        navigatorTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                navigatorTreeSelectionChanged(e);
            }
        });

        navigatorTree.setCellRenderer(new FindBugsFrame.NavigatorCellRenderer());
        navigatorTree.setRootVisible(false);
        navigatorTree.setShowsRootHandles(false);

        bugTree.setCellRenderer(new FindBugsFrame.BugCellRenderer());
        bugTree.setRootVisible(false);
        bugTree.setShowsRootHandles(true);
        
	jarFileList.setModel(new DefaultListModel());
	sourceDirList.setModel(new DefaultListModel());
        
        sortOrderChooser.addItem(BY_CLASS);
        sortOrderChooser.addItem(BY_PACKAGE);
        sortOrderChooser.addItem(BY_CATEGORY);
    }
    
    /**
     * This handler is called whenever the selection in the navigator
     * tree changes.
     * @param e the TreeSelectionEvent
     */
    private void navigatorTreeSelectionChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) navigatorTree.getLastSelectedPathComponent();

        if (node == null)
            return;

        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof ProjectCollection) {
            // Project collection node - there is no view associated with this node
            setView("EmptyPanel");
        } else if (nodeInfo instanceof Project) {
            synchProject((Project) nodeInfo);
            setView("EditProjectPanel");
        } else if (nodeInfo instanceof AnalysisRun) {
            synchAnalysisRun((AnalysisRun) nodeInfo);
            setView("BugTree");
        }
    }
  
    /**
     * Get the currently selected project.
     * @return the current project, or null if no project is selected
     *   (which should only be possible if the root node is selected)
     */
    private Project getCurrentProject() {
	TreePath selPath = navigatorTree.getSelectionPath();
	// Work backwards from end until we get to a project.
	Object[] nodeList = selPath.getPath();
	for (int i = nodeList.length - 1; i >= 0; --i) {
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeList[i];
	    Object nodeInfo = node.getUserObject();
	    if (nodeInfo instanceof Project)
		return (Project) nodeInfo;
	}
	return null;
    }
    
    /**
     * Synchronize the edit project dialog with given project.
     * @param project the selected project
     */
    private void synchProject(Project project) {
        System.out.println("Synch with project " + project.toString());
        
	// Clear text fields
	jarNameTextField.setText("");
	srcDirTextField.setText("");

	// Populate jar and source dir lists
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
    }
    
    /**
     * Synchronize the bug tree with the given analysisRun object.
     * @param analysisRun the selected analysis run
     */
    private void synchAnalysisRun(AnalysisRun analysisRun) {
        boolean modelChanged = false;
        
        if (analysisRun != currentAnalysisRun) {
            modelChanged = true;
            // If this is the first time the analysis run is being shown in
            // the bug tree, it won't have a tree model yet.
            if (analysisRun.getTreeModel() == null) {
                DefaultMutableTreeNode bugRootNode = new DefaultMutableTreeNode();
                DefaultTreeModel bugTreeModel = new DefaultTreeModel(bugRootNode);
                analysisRun.setTreeModel(bugTreeModel);
            }
            
        }
        
        // Make sure that the sort order is correct.
        String currentSortOrder = sortOrderChooser.getSelectedItem().toString();
        if (!analysisRun.getSortOrder().equals(currentSortOrder)) {
            populateAnalysisRunTreeModel(analysisRun, currentSortOrder);
        }

        if (modelChanged) {
            bugTree.setModel(analysisRun.getTreeModel());
            currentAnalysisRun = analysisRun;
        }
        
        // TODO: restore state of tree! I.e., which nodes expanded, and selection
    }
    
    /**
     * Populate an analysis run's tree model for given sort order.
     */
    private void populateAnalysisRunTreeModel(AnalysisRun analysisRun, String sortOrder) {
        // Set busy cursor - this is potentially a time-consuming operation
        Cursor orig = this.getCursor();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        DefaultTreeModel bugTreeModel = analysisRun.getTreeModel();
        DefaultMutableTreeNode bugRootNode = (DefaultMutableTreeNode) bugTreeModel.getRoot();
        
        // Delete all children from root node
        bugRootNode.removeAllChildren();
        
        // Sort the instances
        TreeSet sortedCollection = new TreeSet(getBugInstanceComparator(sortOrder));
        sortedCollection.addAll(analysisRun.getBugInstances());
        
        // Add all instances as children of the root, in sorted order
        Iterator i = sortedCollection.iterator();
        while (i.hasNext()) {
            BugInstance bugInstance = (BugInstance) i.next();
            DefaultMutableTreeNode bugNode = new DefaultMutableTreeNode(bugInstance);
            bugTreeModel.insertNodeInto(bugNode, bugRootNode, bugRootNode.getChildCount());
            
            // Insert annotations
            Iterator j = bugInstance.annotationIterator();
            while (j.hasNext()) {
                BugAnnotation annotation = (BugAnnotation) j.next();
                DefaultMutableTreeNode annotationNode = new DefaultMutableTreeNode(annotation);
                bugTreeModel.insertNodeInto(annotationNode, bugNode,  bugNode.getChildCount());
            }
        }

        // Sort order is up to date now
        analysisRun.setSortOrder(sortOrder);

        // Let the tree know it needs to update itself
        bugTreeModel.nodeStructureChanged(bugRootNode);
        
        // Now we're done
        this.setCursor(orig);
    }
    
    /**
     * Get a BugInstance Comparator for given sort order.
     */
    private Comparator getBugInstanceComparator(String sortOrder) {
        if (sortOrder.equals(BY_CLASS))
            return bugInstanceByClassComparator;
        else if (sortOrder.equals(BY_PACKAGE))
            return bugInstanceByPackageComparator;
        else if (sortOrder.equals(BY_CATEGORY))
            return bugInstanceByCategoryComparator;
        else
            throw new IllegalArgumentException("Bad sort order: " + sortOrder);
    }
    
    private void exitFindBugs() {
        // TODO: offer to save work, etc.
        System.exit(0);
    }
    
    /**
     * Set the view panel to display the named view.
     */
    private void setView(String viewName) {
        viewPanelLayout.show(viewPanel, viewName);
    }
    
    /**
     * Called to add the jar file in the jarNameTextField to the
     * Jar file list (and the project it represents).
     */
    private void addJarToList() {
        String jarFile = jarNameTextField.getText();
        if (!jarFile.equals("")) {
            Project project = getCurrentProject();
            project.addJar(jarFile);
            DefaultListModel listModel = (DefaultListModel)  jarFileList.getModel();
            listModel.addElement(jarFile);
            jarNameTextField.setText("");
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
            project.addSourceDir(sourceDir);
            DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();
            listModel.addElement(sourceDir);
            srcDirTextField.setText("");
        }
    }
    
    public void writeToConsole(String message) {
        consoleMessageArea.append(message);
        consoleMessageArea.append("\n");
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FindBugsFrame frame = new FindBugsFrame();
        frame.setSize(750, 550);
        frame.show();
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel editProjectLabel;
    private javax.swing.JSplitPane navigatorViewSplitter;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JButton removeSrcDirButton;
    private javax.swing.JCheckBoxMenuItem viewConsoleItem;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem closeProjectItem;
    private javax.swing.JMenuItem newProjectItem;
    private javax.swing.JTextField jarNameTextField;
    private javax.swing.JLabel leftFiller;
    private javax.swing.JButton browseJarButton;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane consoleSplitter;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JLabel sortOrderLabel;
    private javax.swing.JList jarFileList;
    private javax.swing.JLabel jarFileLabel;
    private javax.swing.JButton addSourceDirButton;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton findBugsButton;
    private javax.swing.JPanel bugTreePanel;
    private javax.swing.JLabel sourceDirLabel;
    private javax.swing.JPanel viewPanel;
    private javax.swing.JButton removeJarButton;
    private javax.swing.JLabel jarFileListLabel;
    private javax.swing.JButton addJarButton;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JList sourceDirList;
    private javax.swing.JTree navigatorTree;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox sortOrderChooser;
    private javax.swing.JPanel reportPanel;
    private javax.swing.JPanel editProjectPanel;
    private javax.swing.JLabel rightFiller;
    private javax.swing.JButton browseSrcDirButton;
    private javax.swing.JTextField srcDirTextField;
    private javax.swing.JLabel sourceDirListLabel;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JPanel emptyPanel;
    private javax.swing.JTextArea consoleMessageArea;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTree bugTree;
    private javax.swing.JMenuBar jMenuBar1;
    // End of variables declaration//GEN-END:variables
    
    // My variable declarations
    private ConsoleLogger logger;
    private CardLayout viewPanelLayout;
    private ProjectCollection projectCollection;
    private DefaultTreeModel navigatorTreeModel;
    private DefaultMutableTreeNode rootNode;
    private int projectCount;
    private AnalysisRun currentAnalysisRun; // be lazy in switching tree models in BugTree
}
