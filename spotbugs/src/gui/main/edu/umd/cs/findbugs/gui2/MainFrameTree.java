package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.gui2.FilterActivity.FilterActivityNotifier;

public class MainFrameTree {
    private final MainFrame mainFrame;

    JTree tree;

    SorterTableColumnModel sorter;

    JTableHeader tableheader;

    BugLeafNode currentSelectedBugLeaf;

    JPanel treePanel;

    JScrollPane treeScrollPane;

    JPopupMenu bugPopupMenu;

    JPopupMenu branchPopupMenu;

    JPanel cardPanel;

    private JTextField textFieldForPackagesToDisplay;

    private JLabel waitStatusLabel;

    public MainFrameTree(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void newTree(final JTree newTree, final BugTreeModel newModel) {
        SwingUtilities.invokeLater(() -> {
            tree = newTree;
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.setLargeModel(true);
            tree.setCellRenderer(new BugRenderer());
            treePanel.remove(treeScrollPane);
            treeScrollPane = new JScrollPane(newTree);
            treePanel.add(treeScrollPane);
            mainFrame.setFontSizeHelper(Driver.getFontSize(), treeScrollPane);
            tree.setRowHeight((int) (Driver.getFontSize() + 7));
            mainFrame.getContentPane().validate();
            mainFrame.getContentPane().repaint();

            setupTreeListeners();
            newModel.openPreviouslySelected(((BugTreeModel) (tree.getModel())).getOldSelectedBugs());
            expandTree(10);
            expandToFirstLeaf(14);
            mainFrame.getSorter().addColumnModelListener(newModel);
            FilterActivity.addFilterListener(newModel.bugTreeFilterListener);
            mainFrame.mainFrameTree.setSorting(true);

        });
    }

    public JTree getTree() {
        return tree;
    }

    public BugTreeModel getBugTreeModel() {
        return (BugTreeModel) getTree().getModel();
    }

    public Sortables[] getAvailableSortables() {
        Sortables[] sortables;
        ArrayList<Sortables> a = new ArrayList<>(Sortables.values().length);
        for (Sortables s : Sortables.values()) {
            if (s.isAvailable(mainFrame)) {
                a.add(s);
            }
        }
        sortables = new Sortables[a.size()];
        a.toArray(sortables);
        return sortables;
    }

    /**
     * Returns the SorterTableColumnModel of the MainFrame.
     */
    SorterTableColumnModel getSorter() {
        return sorter;
    }

    void rebuildBugTreeIfSortablesDependOnCloud() {
        BugTreeModel bt = (BugTreeModel) (mainFrame.getTree().getModel());
        List<Sortables> sortables = sorter.getOrderBeforeDivider();
        if (sortables.contains(Sortables.FIRSTVERSION) || sortables.contains(Sortables.LASTVERSION)) {

            bt.rebuild();
        }
    }

    public void updateBugTree() {
        mainFrame.acquireDisplayWait();
        try {
            BugTreeModel model = (BugTreeModel) getTree().getModel();
            BugSet bs;
            if (mainFrame.getBugCollection() != null) {
                bs = new BugSet(mainFrame.getBugCollection());
            } else {
                bs = new BugSet(Collections.<BugLeafNode>emptySet());
            }
            model.getOffListenerList();
            model.changeSet(bs);
            if (bs.size() == 0 && bs.sizeUnfiltered() > 0) {
                warnUserOfFilters();
            }

            mainFrame.updateStatusBar();
            mainFrame.updateTitle();
        } finally {
            mainFrame.releaseDisplayWait();
        }
    }

    private void warnUserOfFilters() {
        JOptionPane
                .showMessageDialog(
                        mainFrame,
                        edu.umd.cs.findbugs.L10N
                                .getLocalString("dlg.everything_is_filtered",
                                        "All bugs in this project appear to be filtered out.  \nYou may wish to check your filter settings in the preferences menu."),
                        "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Creates popup menu for bugs on tree.
     */
    JPopupMenu createBugPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem filterMenuItem = MainFrameHelper.newJMenuItem("menu.filterBugsLikeThis", "Filter bugs like this");

        filterMenuItem.addActionListener(evt -> {
            new NewFilterFromBug(new FilterFromBugPicker(currentSelectedBugLeaf.getBug(),
                    Arrays.asList(mainFrame.getAvailableSortables())),
                    new ApplyNewFilter(mainFrame.getProject().getSuppressionFilter(),
                            PreferencesFrame.getInstance(),
                            new FilterActivityNotifier()));

            mainFrame.setProjectChanged(true);
            mainFrame.getTree().setSelectionRow(0); // Selects the top of the Jtree so the CommentsArea syncs up.
        });

        popupMenu.add(filterMenuItem);

        return popupMenu;
    }

    /**
     * Creates the branch pop up menu that ask if the user wants to hide all the
     * bugs in that branch.
     */
    JPopupMenu createBranchPopUpMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem filterMenuItem = MainFrameHelper.newJMenuItem("menu.filterTheseBugs", "Filter these bugs");

        filterMenuItem.addActionListener(evt -> {
            // TODO This code does a smarter version of filtering that is
            // only possible for branches, and does so correctly
            // However, it is still somewhat of a hack, because if we ever
            // add more tree listeners than simply the bugtreemodel,
            // They will not be called by this code. Using FilterActivity to
            // notify all listeners will however destroy any
            // benefit of using the smarter deletion method.

            try {
                int startCount;
                TreePath path = MainFrame.getInstance().getTree().getSelectionPath();
                TreePath deletePath = path;
                startCount = ((BugAspects) (path.getLastPathComponent())).getCount();
                int count = ((BugAspects) (path.getParentPath().getLastPathComponent())).getCount();
                while (count == startCount) {
                    deletePath = deletePath.getParentPath();
                    if (deletePath.getParentPath() == null)// We are at the
                    // top of the
                    // tree, don't
                    // let this be
                    // removed,
                    // rebuild tree
                    // from root.
                    {
                        Matcher m1 = mainFrame.getCurrentSelectedBugAspects().getMatcher();
                        Filter suppressionFilter1 = MainFrame.getInstance().getProject().getSuppressionFilter();
                        suppressionFilter1.addChild(m1);
                        PreferencesFrame.getInstance().updateFilterPanel();
                        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
                        return;
                    }
                    count = ((BugAspects) (deletePath.getParentPath().getLastPathComponent())).getCount();
                }
                /*
                 * deletePath should now be a path to the highest ancestor
                 * branch with the same number of elements as the branch to
                 * be deleted in other words, the branch that we actually
                 * have to remove in order to correctly remove the selected
                 * branch.
                 */
                BugTreeModel model = MainFrame.getInstance().getBugTreeModel();
                TreeModelEvent event = new TreeModelEvent(mainFrame, deletePath.getParentPath(),
                        new int[] { model.getIndexOfChild(deletePath.getParentPath().getLastPathComponent(),
                                deletePath.getLastPathComponent()) }, new Object[] { deletePath.getLastPathComponent() });
                Matcher m2 = mainFrame.getCurrentSelectedBugAspects().getMatcher();
                Filter suppressionFilter2 = MainFrame.getInstance().getProject().getSuppressionFilter();
                suppressionFilter2.addChild(m2);
                PreferencesFrame.getInstance().updateFilterPanel();
                model.sendEvent(event, BugTreeModel.TreeModification.REMOVE);
                // FilterActivity.notifyListeners(FilterListener.Action.FILTERING,
                // null);

                mainFrame.setProjectChanged(true);

                MainFrame.getInstance().getTree().setSelectionRow(0);// Selects
                // the
                // top
                // of
                // the
                // Jtree
                // so
                // the
                // CommentsArea
                // syncs
                // up.
            } catch (RuntimeException e) {
                MainFrame.getInstance().showMessageDialog("Unable to create filter: " + e.getMessage());
            }
        });

        popupMenu.add(filterMenuItem);

        return popupMenu;
    }

    ActionListener treeActionAdapter(ActionMap map, String actionName) {
        final Action selectPrevious = map.get(actionName);
        return e -> {
            e.setSource(tree);
            selectPrevious.actionPerformed(e);
        };
    }

    @SwingThread
    void expandTree(int max) {
        Debug.printf("expandTree(%d)\n", max);
        JTree jTree = getTree();
        int i = 0;
        while (true) {
            int rows = jTree.getRowCount();
            if (i >= rows || rows >= max) {
                break;
            }
            jTree.expandRow(i++);
        }
    }

    @SwingThread
    boolean leavesShown() {
        JTree jTree = getTree();

        int rows = jTree.getRowCount();
        for (int i = 0; i < rows; i++) {
            TreePath treePath = jTree.getPathForRow(i);
            Object lastPathComponent = treePath.getLastPathComponent();
            if (lastPathComponent instanceof BugLeafNode) {
                return true;
            }
        }
        return false;
    }

    @SwingThread
    void expandToFirstLeaf(int max) {
        Debug.println("expand to first leaf");
        if (leavesShown()) {
            return;
        }
        JTree jTree = getTree();
        int i = 0;
        while (true) {
            int rows = jTree.getRowCount();
            if (i >= rows || rows >= max) {
                break;
            }
            TreePath treePath = jTree.getPathForRow(i);
            Object lastPathComponent = treePath.getLastPathComponent();
            if (lastPathComponent instanceof BugLeafNode) {
                return;
            }
            jTree.expandRow(i++);
        }
    }

    void setupTreeListeners() {
        tree.addTreeSelectionListener(new MyTreeSelectionListener());

        tree.addMouseListener(new TreeMouseListener());
    }

    void setSorting(boolean b) {
        tableheader.setReorderingAllowed(b);
    }

    Sortables[] sortables() {
        return Sortables.values();
    }

    public BugLeafNode getCurrentSelectedBugLeaf() {
        return currentSelectedBugLeaf;
    }

    public JPanel bugListPanel() {
        tableheader = new JTableHeader();
        getTableheader().setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        // Listener put here for when user double clicks on sorting
        // column header SorterDialog appears.
        getTableheader().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Debug.println("tableheader.getReorderingAllowed() = " + getTableheader().getReorderingAllowed());
                if (!getTableheader().getReorderingAllowed()) {
                    return;
                }
                if (e.getClickCount() == 2) {
                    SorterDialog.getInstance().setVisible(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
                if (!getTableheader().getReorderingAllowed()) {
                    return;
                }
                BugTreeModel bt = (BugTreeModel) (getTree().getModel());
                bt.checkSorter();
            }
        });
        sorter = GUISaveState.getInstance().getStarterTable();
        getTableheader().setColumnModel(getSorter());
        getTableheader().setToolTipText(
                edu.umd.cs.findbugs.L10N.getLocalString("tooltip.reorder_message", "Drag to reorder tree folder and sort order"));

        tree = new JTree();
        getTree().setLargeModel(true);
        getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        getTree().setCellRenderer(new BugRenderer());
        getTree().setRowHeight((int) (Driver.getFontSize() + 7));
        getTree().setModel(new BugTreeModel(mainFrame, getTree(), getSorter(), new BugSet(new ArrayList<BugLeafNode>())));
        setupTreeListeners();
        mainFrame.setProject(new Project());

        treeScrollPane = new JScrollPane(getTree());

        treePanel = new JPanel(new BorderLayout());
        treePanel.add(treeScrollPane, BorderLayout.CENTER);
        JTable t = new JTable(new DefaultTableModel(0, sortables().length));
        t.setTableHeader(getTableheader());

        textFieldForPackagesToDisplay = new JTextField();
        ActionListener filterAction = e -> {
            try {
                String text = textFieldForPackagesToDisplay.getText();
                if (text.indexOf('/') >= 0) {
                    text = text.replace('/', '.');
                    textFieldForPackagesToDisplay.setText(text);
                }
                mainFrame.getViewFilter().setPackagesToDisplay(text);
                mainFrame.resetViewCache();
            } catch (IllegalArgumentException err) {
                JOptionPane.showMessageDialog(mainFrame, err.getMessage(), "Bad class search string",
                        JOptionPane.ERROR_MESSAGE);
            }
        };
        textFieldForPackagesToDisplay.addActionListener(filterAction);
        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(filterAction);
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        filterPanel.add(textFieldForPackagesToDisplay, gbc);

        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        filterPanel.add(filterButton, gbc);

        filterPanel.setToolTipText("Only show classes containing the word(s) you specify");

        JPanel sortablePanel = new JPanel(new GridBagLayout());
        JLabel sortableLabel = new JLabel("Group bugs by:");
        sortableLabel.setLabelFor(getTableheader());
        gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.BOTH;
        sortablePanel.add(sortableLabel, gbc);
        gbc.weightx = 1;
        sortablePanel.add(getTableheader(), gbc);

        getTableheader().setBorder(new LineBorder(Color.BLACK));

        JPanel topPanel = makeNavigationPanel("Class name filter:", filterPanel, sortablePanel, treePanel);
        cardPanel = new JPanel(new CardLayout());
        JPanel waitPanel = new JPanel();
        waitPanel.setLayout(new BoxLayout(waitPanel, BoxLayout.Y_AXIS));
        waitPanel.add(new JLabel("Please wait..."));
        waitStatusLabel = new JLabel();
        waitPanel.add(waitStatusLabel);
        cardPanel.add(topPanel, MainFrame.BugCard.TREECARD.name());
        cardPanel.add(waitPanel, MainFrame.BugCard.WAITCARD.name());
        return cardPanel;
    }

    public JTableHeader getTableheader() {
        return tableheader;
    }

    public void setBugPopupMenu(JPopupMenu bugPopupMenu) {
        this.bugPopupMenu = bugPopupMenu;
    }

    public void setBranchPopupMenu(JPopupMenu branchPopupMenu) {
        this.branchPopupMenu = branchPopupMenu;
    }

    void updateFonts(float size) {
        bugPopupMenu.setFont(bugPopupMenu.getFont().deriveFont(size));
        mainFrame.setFontSizeHelper(size, bugPopupMenu.getComponents());

        branchPopupMenu.setFont(branchPopupMenu.getFont().deriveFont(size));
        mainFrame.setFontSizeHelper(size, branchPopupMenu.getComponents());
    }

    void showCard(final MainFrame.BugCard card, final Cursor cursor, final Window window) {
        Runnable doRun = () -> {
            mainFrame.enableRecentMenu(card == MainFrame.BugCard.TREECARD);
            getTableheader().setReorderingAllowed(card == MainFrame.BugCard.TREECARD);
            mainFrame.getMainFrameMenu().enablePreferencesMenuItem(card == MainFrame.BugCard.TREECARD);
            window.setCursor(cursor);
            CardLayout layout = (CardLayout) cardPanel.getLayout();
            layout.show(cardPanel, card.name());
            if (card == MainFrame.BugCard.TREECARD) {
                SorterDialog.getInstance().thaw();
            } else {
                SorterDialog.getInstance().freeze();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            doRun.run();
        } else {
            SwingUtilities.invokeLater(doRun);
        }
    }

    private JPanel makeNavigationPanel(String packageSelectorLabel, JComponent packageSelector, JComponent treeHeader,
            JComponent tree) {
        JPanel topPanel = new JPanel();
        topPanel.setMinimumSize(new Dimension(150, 150));

        topPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = c.ipady = 3;
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(packageSelectorLabel);
        topPanel.add(label, c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        topPanel.add(packageSelector, c);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        c.ipadx = c.ipady = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(treeHeader, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridy++;
        c.weighty = 1;
        c.ipadx = c.ipady = 0;
        c.insets = new Insets(0, 0, 0, 0);
        topPanel.add(tree, c);
        return topPanel;
    }

    public void setWaitStatusLabelText(String msg) {
        waitStatusLabel.setText(msg);
    }

    public void setFieldForPackagesToDisplayText(String filter) {
        textFieldForPackagesToDisplay.setText(filter);
    }

    private class TreeMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());

            if (path == null) {
                return;
            }

            if (currentSelectedBugLeaf == path.getLastPathComponent()) {
                // sync mainFrame if user just clicks on the same bug
                mainFrame.syncBugInformation();
            }

            if ((e.getButton() == MouseEvent.BUTTON3) || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {

                if (tree.getModel().isLeaf(path.getLastPathComponent())) {
                    tree.setSelectionPath(path);
                    bugPopupMenu.show(tree, e.getX(), e.getY());
                } else {
                    tree.setSelectionPath(path);
                    if (!(path.getParentPath() == null)) {
                        // path is null, the
                        // root was selected,
                        // don't allow them to
                        // filter out the root.
                        branchPopupMenu.show(tree, e.getX(), e.getY());
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent arg0) {
        }

        @Override
        public void mouseReleased(MouseEvent arg0) {
        }

        @Override
        public void mouseEntered(MouseEvent arg0) {
        }

        @Override
        public void mouseExited(MouseEvent arg0) {
        }
    }

    private class MyTreeSelectionListener implements TreeSelectionListener {
        private volatile boolean ignoreSelection = false;

        @Override
        public void valueChanged(TreeSelectionEvent selectionEvent) {
            if (ignoreSelection) {
                return;
            }

            TreePath path = selectionEvent.getNewLeadSelectionPath();
            if (path != null) {
                Object lastPathComponent = path.getLastPathComponent();
                if (lastPathComponent instanceof BugLeafNode) {
                    boolean beforeProjectChanged = mainFrame.isProjectChanged();
                    currentSelectedBugLeaf = (BugLeafNode) lastPathComponent;
                    mainFrame.setCurrentSelectedBugAspects(null);
                    mainFrame.syncBugInformation();
                    mainFrame.setProjectChanged(beforeProjectChanged);
                } else {
                    boolean beforeProjectChanged = mainFrame.isProjectChanged();
                    currentSelectedBugLeaf = null;
                    mainFrame.setCurrentSelectedBugAspects((BugAspects) lastPathComponent);
                    mainFrame.syncBugInformation();
                    mainFrame.setProjectChanged(beforeProjectChanged);
                }
            }
        }
    }
}
