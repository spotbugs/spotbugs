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
    
    /**
     * Compare BugInstance class names.
     * This is useful for grouping bug instances by class.
     * Note that all instances with the same class name will compare
     * as equal.
     */
    private static class BugInstanceClassComparator implements Comparator {
	public int compare(Object a, Object b) {
	    BugInstance lhs = (BugInstance) a;
	    BugInstance rhs = (BugInstance) b;
	    return lhs.getPrimaryClass().compareTo(rhs.getPrimaryClass());
	}
    }
    
    /** The instance of BugInstanceClassComparator. */
    private static final Comparator bugInstanceClassComparator = new BugInstanceClassComparator();
    
    /**
     * Compare BugInstance package names.
     * This is useful for grouping bug instances by package.
     * Note that all instances with the same package name will compare
     * as equal.
     */
    private static class BugInstancePackageComparator implements Comparator {
	public int compare(Object a, Object b) {
	    BugInstance lhs = (BugInstance) a;
	    BugInstance rhs = (BugInstance) b;
	    return lhs.getPrimaryClass().getPackageName().compareTo(
	    rhs.getPrimaryClass().getPackageName());
	}
    }
    
    /** The instance of BugInstancePackageComparator. */
    private static final Comparator bugInstancePackageComparator = new BugInstancePackageComparator();
    
    /**
     * Compare BugInstance bug types.
     * This is useful for grouping bug instances by bug type.
     * Note that all instances with the same bug type will compare
     * as equal.
     */
    private static class BugInstanceTypeComparator implements Comparator {
	public int compare(Object a, Object b) {
	    BugInstance lhs = (BugInstance) a;
	    BugInstance rhs = (BugInstance) b;
	    String lhsString = lhs.toString();
	    String rhsString = rhs.toString();
	    return lhsString.substring(0, lhsString.indexOf(':')).compareTo(
		rhsString.substring(0, rhsString.indexOf(':')));
	}
    }
    
    /** The instance of BugInstanceTypeComparator. */
    private static final Comparator bugInstanceTypeComparator = new BugInstanceTypeComparator();
    
    /**
     * Two-level comparison of bug instances by class name and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByClassComparator implements Comparator {
	public int compare(Object a, Object b) {
	    int cmp = bugInstanceClassComparator.compare(a, b);
	    if (cmp != 0)
		return cmp;
	    return ((Comparable)a).compareTo(b);
	}
    }
    
    /** The instance of BugInstanceByClassComparator. */
    private static final Comparator bugInstanceByClassComparator = new FindBugsFrame.BugInstanceByClassComparator();
    
    /**
     * Two-level comparison of bug instances by package and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByPackageComparator implements Comparator {
	public int compare(Object a, Object b) {
	    int cmp = bugInstancePackageComparator.compare(a, b);
	    if (cmp != 0)
		return cmp;
	    return ((Comparable)a).compareTo(b);
	}
    }
    
    /** The instance of BugInstanceByPackageComparator. */
    private static final Comparator bugInstanceByPackageComparator = new FindBugsFrame.BugInstanceByPackageComparator();

    /**
     * Two-level comparison of bug instances by bug type and
     * BugInstance natural ordering.
     */
    private static class BugInstanceByTypeComparator implements Comparator {
	public int compare(Object a, Object b) {
	    int cmp = bugInstanceTypeComparator.compare(a, b);
	    if (cmp != 0)
		return cmp;
	    return ((Comparable)a).compareTo(b);
	}
    }
    
    /** The instance of BugTypeByTypeComparator. */
    private static final Comparator bugInstanceByTypeComparator = new FindBugsFrame.BugInstanceByTypeComparator();

    /**
     * Swing FileFilter class for file selection dialogs for FindBugs project files.
     */
    private static class ProjectFileFilter extends FileFilter {
        public boolean accept(File file) { return file.isDirectory() || file.getName().endsWith(".fb"); }
        public String getDescription() { return "FindBugs projects (*.fb)"; }
    }
    
    /** The instance of ProjectFileFilter. */
    private static final FileFilter projectFileFilter = new ProjectFileFilter();

    /* ----------------------------------------------------------------------
     * Constants
     * ---------------------------------------------------------------------- */

    private static final String GROUP_BY_CLASS = "By class";
    private static final String GROUP_BY_PACKAGE = "By package";
    private static final String GROUP_BY_BUG_TYPE = "By bug type";
    
    /**
     * A fudge value required in our hack to get the REAL maximum
     * divider location for the consoleSplitter.  Experience suggests that
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
        navigatorViewSplitter = new javax.swing.JSplitPane();
        navigatorScrollPane = new javax.swing.JScrollPane();
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
        jarFileListScrollPane = new javax.swing.JScrollPane();
        jarFileList = new javax.swing.JList();
        sourceDirListScrollPane = new javax.swing.JScrollPane();
        sourceDirList = new javax.swing.JList();
        bugTreePanel = new javax.swing.JPanel();
        groupByChooser = new javax.swing.JComboBox();
        groupByLabel = new javax.swing.JLabel();
        bugTreeBugDetailsSplitter = new javax.swing.JSplitPane();
        bugTreeScrollPane = new javax.swing.JScrollPane();
        bugTree = new javax.swing.JTree();
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
        closeProjectItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        exitItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        viewConsoleItem = new javax.swing.JCheckBoxMenuItem();
        viewBugDetailsItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        setTitle("FindBugs");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
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

        navigatorScrollPane.setPreferredSize(new java.awt.Dimension(140, 0));
        navigatorTree.setModel(createNavigatorTreeModel());
        navigatorScrollPane.setViewportView(navigatorTree);

        navigatorViewSplitter.setLeftComponent(navigatorScrollPane);

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
        gridBagConstraints.weighty = 0.7;
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
        gridBagConstraints.weighty = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirListScrollPane, gridBagConstraints);

        viewPanel.add(editProjectPanel, "EditProjectPanel");

        bugTreePanel.setLayout(new java.awt.GridBagLayout());

        groupByChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupByChooserActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bugTreePanel.add(groupByChooser, gridBagConstraints);

        groupByLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        groupByLabel.setText("Group:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        bugTreePanel.add(groupByLabel, gridBagConstraints);

        bugTreeBugDetailsSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        bugTreeBugDetailsSplitter.setResizeWeight(1.0);
        bugTreeBugDetailsSplitter.setOneTouchExpandable(true);
        bugTreeScrollPane.setViewportView(bugTree);

        bugTreeBugDetailsSplitter.setLeftComponent(bugTreeScrollPane);

        bugDescriptionScrollPane.setViewportView(bugDescriptionEditorPane);

        bugDetailsTabbedPane.addTab("Details", bugDescriptionScrollPane);

        sourceTextAreaScrollPane.setMinimumSize(new java.awt.Dimension(22, 180));
        sourceTextAreaScrollPane.setPreferredSize(new java.awt.Dimension(0, 100));
        sourceTextArea.setEditable(false);
        sourceTextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 12));
        sourceTextAreaScrollPane.setViewportView(sourceTextArea);

        bugDetailsTabbedPane.addTab("Source code", sourceTextAreaScrollPane);

        bugTreeBugDetailsSplitter.setRightComponent(bugDetailsTabbedPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bugTreePanel.add(bugTreeBugDetailsSplitter, gridBagConstraints);

        viewPanel.add(bugTreePanel, "BugTree");

        navigatorViewSplitter.setRightComponent(viewPanel);

        consoleSplitter.setTopComponent(navigatorViewSplitter);

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
        saveProjectItem.setText("Save project");
        saveProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveProjectItem);

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

        theMenuBar.add(fileMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");
        viewMenu.setFont(new java.awt.Font("Dialog", 0, 12));
        viewConsoleItem.setFont(new java.awt.Font("Dialog", 0, 12));
        viewConsoleItem.setMnemonic('C');
        viewConsoleItem.setSelected(true);
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
        viewMenu.add(viewBugDetailsItem);

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

    private void openProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjectItemActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(projectFileFilter);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION)
            return;
        try {
            File file = chooser.getSelectedFile();
            Project project = new Project(file.getPath());
            FileInputStream in = new FileInputStream(file);
            project.read(in);
            addProject(project);
        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.ERROR, "Could not open project: " + e.getMessage());
        }
    }//GEN-LAST:event_openProjectItemActionPerformed
    
    private void saveProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectItemActionPerformed
        try {
            Project project = getCurrentProject();
            if (project == null)
                return;
            
            File file;
            String fileName = project.getFileName();

            if (!fileName.startsWith("<")) {
                file = new File(fileName);
            } else {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(projectFileFilter);

                int result = chooser.showSaveDialog(this);
                if (result == JFileChooser.CANCEL_OPTION)
                    return;
                file = chooser.getSelectedFile();
                project.setFileName(file.getPath());
            }
            
            FileOutputStream out = new FileOutputStream(file);
            project.write(out);
            logger.logMessage(ConsoleLogger.INFO, "Project saved");
            
            // Project filename may have changed, so update the node in
            // the navigator tree
            DefaultMutableTreeNode projectNode = (DefaultMutableTreeNode)
                navigatorTree.getSelectionPath().getPath()[1];
            navigatorTreeModel.nodeChanged(projectNode);

        } catch (IOException e) {
            logger.logMessage(ConsoleLogger.ERROR, "Could not save project: " + e.getMessage());
        }
    }//GEN-LAST:event_saveProjectItemActionPerformed

    private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
	AboutDialog dialog = new AboutDialog(this, true);
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
    
    private void groupByChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupByChooserActionPerformed
	String selection = groupByChooser.getSelectedItem().toString();
	if (selection != null && currentAnalysisRun != null)
	    populateAnalysisRunTreeModel(currentAnalysisRun, selection);
    }//GEN-LAST:event_groupByChooserActionPerformed
    
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
	Project project = new Project(projectName);
        addProject(project);
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
     * This is called whenever the selection is changed in the bug tree.
     * @param e the TreeSelectionEvent
     */
    private void bugTreeSelectionChanged(TreeSelectionEvent e) {
	BugInstance selected = getCurrentBugInstance();
	if (selected != null && selected != currentBugInstance) {
	    synchBugInstance(selected);
	}
    }
    
    /* ----------------------------------------------------------------------
     * Component initialization support
     * ---------------------------------------------------------------------- */
    
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
	
	bugTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	bugTree.setCellRenderer(new FindBugsFrame.BugCellRenderer());
	bugTree.setRootVisible(false);
	bugTree.setShowsRootHandles(true);
	bugTree.addTreeSelectionListener(new TreeSelectionListener() {
	    public void valueChanged(TreeSelectionEvent e) {
		bugTreeSelectionChanged(e);
	    }
	});
	
	jarFileList.setModel(new DefaultListModel());
	sourceDirList.setModel(new DefaultListModel());
	
	groupByChooser.addItem(GROUP_BY_CLASS);
	groupByChooser.addItem(GROUP_BY_PACKAGE);
	groupByChooser.addItem(GROUP_BY_BUG_TYPE);
	
	bugTreeBugDetailsSplitter.setDividerLocation(1.0);

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
     * Get the currently selected project.
     * @return the current project, or null if no project is selected
     *   (which should only be possible if the root node is selected)
     */
    private Project getCurrentProject() {
	return (Project) getTreeSelectionOf(navigatorTree, Project.class);
    }
    
    /**
     * Get the currently selected analysis run.
     * @return the current analysis run, or null if no analysis run
     *   is selected
     */
    private AnalysisRun getCurrentAnalysisRun() {
	return (AnalysisRun) getTreeSelectionOf(navigatorTree, AnalysisRun.class);
    }

    /**
     * Get the bug instances currently selected in the bug tree.
     */
    private BugInstance getCurrentBugInstance() {
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
        int parentHeight = parent.getHeight();
	int hopefullyMaxDivider = parentHeight - (splitter.getDividerSize() + DIVIDER_FUDGE);
/*
	    System.out.println("pane height = " + contentPaneHeight + ", dividerLoc=" + location.intValue() +
		", hopefullyMaxDivider=" + hopefullyMaxDivider);
 */
	boolean isMaximized = location.intValue() >= hopefullyMaxDivider;
        return isMaximized;
    }
    
    /* ----------------------------------------------------------------------
     * Synchronization of data model and UI
     * ---------------------------------------------------------------------- */

    /**
     * Add a new project to the UI.
     * @param project the new project
     */
    private void addProject(Project project) {
	projectCollection.addProject(project);
	DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
	DefaultTreeModel treeModel = (DefaultTreeModel) navigatorTree.getModel();
	treeModel.insertNodeInto(projectNode, rootNode, rootNode.getChildCount());
	TreePath projPath = new TreePath(new Object[]{rootNode, projectNode});
	navigatorTree.makeVisible(projPath);
	navigatorTree.setSelectionPath(projPath);
    }
    
    /**
     * Synchronize the edit project dialog with given project.
     * @param project the selected project
     */
    private void synchProject(Project project) {
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
	String currentSortOrder = groupByChooser.getSelectedItem().toString();
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
    private void populateAnalysisRunTreeModel(AnalysisRun analysisRun, final String groupBy) {
	// Set busy cursor - this is potentially a time-consuming operation
	Cursor orig = this.getCursor();
	this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	
	final DefaultTreeModel bugTreeModel = analysisRun.getTreeModel();
	final DefaultMutableTreeNode bugRootNode = (DefaultMutableTreeNode) bugTreeModel.getRoot();
	
	// Delete all children from root node
	bugRootNode.removeAllChildren();
	
	// Sort the instances
	TreeSet sortedCollection = new TreeSet(getBugInstanceComparator(groupBy));
	sortedCollection.addAll(analysisRun.getBugInstances());
	
	// The grouper callback is what actually adds the group and bug
	// nodes to the tree.
	Grouper.Callback callback = new Grouper.Callback() {
	    private BugInstanceGroup currentGroup;
	    private DefaultMutableTreeNode currentGroupNode;
	    
	    public void startGroup(Object member_) {
		BugInstance member = (BugInstance) member_;
		String groupName;
		if (groupBy == GROUP_BY_CLASS)
		    groupName = member.getPrimaryClass().getClassName();
		else if (groupBy == GROUP_BY_PACKAGE) {
		    groupName = member.getPrimaryClass().getPackageName();
                    if (groupName.equals(""))
                        groupName = "Unnamed package";
                } else if (groupBy == GROUP_BY_BUG_TYPE) {
		    String desc = member.toString();
		    groupName = desc.substring(0, desc.indexOf(':'));
		} else
		    throw new IllegalStateException("Unknown sort order: " + groupBy);
		currentGroup = new BugInstanceGroup(groupBy, groupName);
		currentGroupNode = new DefaultMutableTreeNode(currentGroup);
		bugTreeModel.insertNodeInto(currentGroupNode, bugRootNode, bugRootNode.getChildCount());
		
		insertIntoGroup(member);
	    }
	    
	    public void addToGroup(Object member_) {
		BugInstance member = (BugInstance) member_;
		insertIntoGroup(member);
	    }
	    
	    private void insertIntoGroup(BugInstance member) {
		currentGroup.incrementMemberCount();
		DefaultMutableTreeNode bugNode = new DefaultMutableTreeNode(member);
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
	Grouper grouper = new Grouper(callback);
	Comparator groupComparator = getGroupComparator(groupBy);
	grouper.group(sortedCollection, groupComparator);
	
	// Sort order is up to date now
	analysisRun.setSortOrder(groupBy);
	
	// Let the tree know it needs to update itself
	bugTreeModel.nodeStructureChanged(bugRootNode);
	
	// Now we're done
	this.setCursor(orig);
    }
    
    /**
     * Get a BugInstance Comparator for given sort order.
     */
    private Comparator getBugInstanceComparator(String sortOrder) {
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
    private Comparator getGroupComparator(String groupBy) {
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

    /**
     * Synchronize given bug instance with the bug detail
     * window (source view, details window, etc.)
     */
    private void synchBugInstance(BugInstance selected) {
	// Update source view
	// TODO: only do this when the detail window is displayed AND
	// the source code tab is chosen.
	SourceLineAnnotation primarySrcLine = selected.getPrimarySourceLineAnnotation();
	if (primarySrcLine != null) {
	    Project project = getCurrentProject();
	    AnalysisRun analysisRun = getCurrentAnalysisRun();
	    if (project == null) throw new IllegalStateException("null project!");
	    if (analysisRun == null) throw new IllegalStateException("null analysis run!");
	    viewSource(project, analysisRun, primarySrcLine);
	}
    }

    /**
     * Update the source view window.
     * @param project the project (containing the source directories to search)
     * @param analysisRun the analysis run (containing the mapping of classes to source files)
     * @param srcLine the source line annotation (specifying source file to load and
     *    which lines to highlight)
     */
    private void viewSource(Project project, AnalysisRun analysisRun, SourceLineAnnotation srcLine) {
	sourceFinder.setSourceBaseList(project.getSourceDirList());
	String sourceFile = analysisRun.getSourceFile(srcLine.getClassName());
	if (sourceFile == null) {
	    System.out.println("No source file for class " + srcLine.getClassName());
	    return;
	}
	
	sourceTextArea.setText("");
	bugTreeBugDetailsSplitter.resetToPreferredSizes();

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
	    logger.logMessage(ConsoleLogger.ERROR, e.getMessage());
	    return;
	}

        // Highlight the lines from the source annotation.
        // Note that the source lines start at 1, while the line numbers
        // in the text area start at 0.
        try {
            int selBegin = sourceTextArea.getLineStartOffset(srcLine.getStartLine() - 1);
            int selEnd = sourceTextArea.getLineStartOffset(srcLine.getEndLine());
            sourceTextArea.select(selBegin, selEnd);
            sourceTextArea.getCaret().setSelectionVisible(true);

        } catch (javax.swing.text.BadLocationException e) {
            logger.logMessage(ConsoleLogger.ERROR, e.getMessage());
        }
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
	FindBugsFrame frame = new FindBugsFrame();
	frame.setSize(800, 600);
	frame.show();
    }
    
    /* ----------------------------------------------------------------------
     * Instance variables
     * ---------------------------------------------------------------------- */
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel editProjectLabel;
    private javax.swing.JButton removeSrcDirButton;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem newProjectItem;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JSplitPane consoleSplitter;
    private javax.swing.JList jarFileList;
    private javax.swing.JLabel jarFileLabel;
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JButton addSourceDirButton;
    private javax.swing.JMenuBar theMenuBar;
    private javax.swing.JComboBox groupByChooser;
    private javax.swing.JScrollPane navigatorScrollPane;
    private javax.swing.JButton removeJarButton;
    private javax.swing.JButton addJarButton;
    private javax.swing.JScrollPane sourceTextAreaScrollPane;
    private javax.swing.JTree navigatorTree;
    private javax.swing.JList sourceDirList;
    private javax.swing.JMenuItem saveProjectItem;
    private javax.swing.JSplitPane bugTreeBugDetailsSplitter;
    private javax.swing.JEditorPane bugDescriptionEditorPane;
    private javax.swing.JTabbedPane bugDetailsTabbedPane;
    private javax.swing.JPanel emptyPanel;
    private javax.swing.JTextArea consoleMessageArea;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTree bugTree;
    private javax.swing.JScrollPane jarFileListScrollPane;
    private javax.swing.JSplitPane navigatorViewSplitter;
    private javax.swing.JLabel groupByLabel;
    private javax.swing.JCheckBoxMenuItem viewConsoleItem;
    private javax.swing.JMenuItem closeProjectItem;
    private javax.swing.JCheckBoxMenuItem viewBugDetailsItem;
    private javax.swing.JTextField jarNameTextField;
    private javax.swing.JScrollPane consoleScrollPane;
    private javax.swing.JButton browseJarButton;
    private javax.swing.JTextArea sourceTextArea;
    private javax.swing.JButton findBugsButton;
    private javax.swing.JPanel bugTreePanel;
    private javax.swing.JScrollPane bugDescriptionScrollPane;
    private javax.swing.JLabel sourceDirLabel;
    private javax.swing.JPanel viewPanel;
    private javax.swing.JLabel jarFileListLabel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel reportPanel;
    private javax.swing.JPanel editProjectPanel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextField srcDirTextField;
    private javax.swing.JButton browseSrcDirButton;
    private javax.swing.JLabel sourceDirListLabel;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JScrollPane bugTreeScrollPane;
    private javax.swing.JScrollPane sourceDirListScrollPane;
    // End of variables declaration//GEN-END:variables
    
    // My variable declarations
    private ConsoleLogger logger;
    private CardLayout viewPanelLayout;
    private ProjectCollection projectCollection;
    private DefaultTreeModel navigatorTreeModel;
    private DefaultMutableTreeNode rootNode;
    private int projectCount;
    private AnalysisRun currentAnalysisRun; // be lazy in switching tree models in BugTree
    private SourceFinder sourceFinder = new SourceFinder();
    private BugInstance currentBugInstance; // be lazy in switching bug instance details
}
