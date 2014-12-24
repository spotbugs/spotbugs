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

import java.awt.Component;
import java.awt.Font;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeModel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Handles the sorting order and informs the treeModel when changes are
 * necessary
 *
 * @author Dan
 *
 */
public class SorterTableColumnModel implements TableColumnModel {

    private ArrayList<Sortables> order = new ArrayList<Sortables>();

    private final Set<Sortables> shown = new HashSet<Sortables>();

    private final ArrayList<TableColumn> columnList = new ArrayList<TableColumn>();

    private DefaultListSelectionModel dlsm;

    private final ArrayList<TableColumnModelListener> watchers = new ArrayList<TableColumnModelListener>();

    private boolean frozen = false;

    public boolean isShown(Sortables s) {
        return shown.contains(s);
    }

    @Override
    public String toString() {
        return order.toString();
    }

    static boolean shownError;

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void check() {
        if (order.size() == shown.size() && order.containsAll(shown)) {
            return;
        }
        if (shownError) {
            return;
        }
        shownError = true;
        MainFrame.getInstance().error("Incompatible order and shown for SorterTable: " + order + " vs. " + shown);
        shown.clear();
        shown.addAll(order);
    }
    public SorterTableColumnModel(Sortables[] columnHeaders) {

        MainFrame mainFrame = MainFrame.getInstance();
        int x = 0;
        for (Sortables c : columnHeaders) {
            if (!c.isAvailable(mainFrame)) {
                continue;
            }
            shown.add(c);

            TableColumn tc = makeTableColumn(x, c);
            columnList.add(tc);
            x++;
        }
        dlsm = new DefaultListSelectionModel();
        dlsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderUpdate();
        check();
    }

    private TableColumn makeTableColumn(int x, Sortables c) {
        TableColumn tc = new TableColumn(x);
        FBTableCellRenderer temp = new FBTableCellRenderer();
        tc.setHeaderRenderer(temp);
        tc.setIdentifier(c);
        tc.setHeaderValue(c);
        tc.setResizable(false);
        tc.sizeWidthToFit();
        return tc;
    }

    public void createFrom(SorterTableColumnModel other) {
        if (this.getOrder().equals(other.getOrder())) {
            return;
        }
        columnList.clear();
        for (int x = 0; x < order.size(); x++) {
            for (TableColumnModelListener l : watchers) {
                l.columnRemoved(new TableColumnModelEvent(this, x, x));
            }
        }

        // First, empty showOrder
        shown.clear();
        MainFrame mainFrame = MainFrame.getInstance();
        int x = 0;
        for (Sortables c : other.order) {
            if (!c.isAvailable(mainFrame)) {
                continue;
            }

            shown.add(c);
            TableColumn tc = makeTableColumn(x, c);
            columnList.add(tc);
            for (TableColumnModelListener l : watchers) {
                l.columnAdded(new TableColumnModelEvent(this, x, x));
            }
            x++;
        }
        dlsm = new DefaultListSelectionModel();
        dlsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        orderUpdate();

    }

    public SorterTableColumnModel(Collection<Sortables> columnHeaders) {
        this(columnHeaders.toArray(new Sortables[columnHeaders.size()]));
    }

    static class FBTableCellRenderer implements TableCellRenderer {

        private final TableCellRenderer defaultRenderer = new JTableHeader().getDefaultRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            Component comp = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (comp instanceof JLabel) {
                JLabel cell = (JLabel) comp;
                cell.setFont(cell.getFont().deriveFont(Driver.getFontSize()));
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                cell.setBorder(BorderFactory.createCompoundBorder(cell.getBorder(), BorderFactory.createEmptyBorder(0, 6, 0, 6)));
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                if (value == Sortables.DIVIDER) {
                    URL arrows = MainFrame.class.getResource("arrows.png");

                    if (arrows != null) {
                        cell.setText("");
                        cell.setIcon(new ImageIcon(arrows));
                    } else {
                        cell.setText("<->");
                    }

                }
            }
            return comp;
        }
    }

    @Override
    public void addColumn(TableColumn arg0) {
        throw new UnsupportedOperationException("Can't change sorter table columns using addColumn");
    }

    @Override
    public void removeColumn(TableColumn arg0) {
        throw new UnsupportedOperationException("Can't change sorter table columns using removeColumn");
    }

    public void setVisible(Sortables s, boolean on) {

        if (shown.contains(s) == on) {
            // no op
            return;
        }
        if (on) {
            shown.add(s);
            TableColumn tc = makeTableColumn(columnList.size(), s);
            columnList.add(tc);
            for (int x = 0; x < columnList.size(); x++) {
                columnList.get(x).setModelIndex(x);
            }
            orderUpdate();
            for (TableColumnModelListener l : watchers) {
                l.columnAdded(new TableColumnModelEvent(this, columnList.size() - 1, columnList.size() - 1));
            }
        } else {
            shown.remove(s);
            for (int x = 0; x < columnList.size(); x++) {
                columnList.get(x).setModelIndex(x);
            }

            for (int counter = 0; counter < columnList.size(); counter++) {
                TableColumn tc = columnList.get(counter);
                if (tc.getIdentifier().equals(s)) {
                    columnList.remove(counter);
                    for (int x = counter; x < columnList.size(); x++) {
                        columnList.get(x).setModelIndex(x);
                    }

                    orderUpdate();
                    for (TableColumnModelListener l : watchers) {
                        l.columnRemoved(new TableColumnModelEvent(this, counter, counter));
                    }
                }
            }
        }

    }

    @Override
    public void moveColumn(int fromIndex, int toIndex) {

        if (!MainFrame.getInstance().canNavigateAway()) {
            return;
        }
        MainFrame.getInstance().updateDesignationDisplay();
        TableColumn from = columnList.get(fromIndex);
        TableColumn to = columnList.get(toIndex);

        columnList.set(fromIndex, to);
        to.setModelIndex(fromIndex);

        columnList.set(toIndex, from);
        from.setModelIndex(toIndex);

        orderUpdate();

        for (TableColumnModelListener w : new ArrayList<TableColumnModelListener>(watchers)) {
            w.columnMoved(new TableColumnModelEvent(this, fromIndex, toIndex));
        }
    }

    @Override
    public void setColumnMargin(int arg0) {
        throw new UnsupportedOperationException("NoBah");
    }

    @Override
    public int getColumnCount() {
        return columnList.size();
    }

    @Override
    public Enumeration<TableColumn> getColumns() {
        return Collections.<TableColumn> enumeration(columnList);
    }

    @Override
    public int getColumnIndex(Object columnIdentifier) {

        if (columnIdentifier == null) {
            throw new IllegalArgumentException("Dont send null to getColumnIndex, null shouldn't be in the sorting table.");
        }

        for (int x = 0; x < columnList.size(); x++) {
            if (columnList.get(x).getIdentifier().equals(columnIdentifier)) {
                return x;
            }
        }

        throw new IllegalArgumentException();
    }

    @Override
    public TableColumn getColumn(int x) {
        return columnList.get(x);
    }

    @Override
    public int getColumnMargin() {
        return 0;
    }

    @Override
    public int getColumnIndexAtX(int XPosition) {

        for (TableColumn tc : columnList) {
            XPosition -= tc.getWidth();
            if (XPosition < 0) {
                return tc.getModelIndex();
            }
        }
        return -1;
    }

    @Override
    public int getTotalColumnWidth() {
        int total = 0;
        for (TableColumn tc : columnList) {
            total += tc.getWidth();
        }
        return total;
    }

    @Override
    public void setColumnSelectionAllowed(boolean arg0) {
        throw new UnsupportedOperationException("BAH");// BAH
    }

    @Override
    public boolean getColumnSelectionAllowed() {
        return true;
    }

    @Override
    public int[] getSelectedColumns() {
        int index = dlsm.getMinSelectionIndex();
        if (index == -1) {
            return new int[] {};
        }
        return new int[] { index };
    }

    @Override
    public int getSelectedColumnCount() {

        if (dlsm.getMinSelectionIndex() == -1) {
            return 0;
        }
        return 1;
    }

    @Override
    public void setSelectionModel(ListSelectionModel arg0) {
        throw new UnsupportedOperationException("No... NO NO NO NO");
    }

    @Override
    public ListSelectionModel getSelectionModel() {
        return dlsm;
    }

    @Override
    public void addColumnModelListener(TableColumnModelListener listener) {
        watchers.add(listener);
    }

    @Override
    public void removeColumnModelListener(TableColumnModelListener listener) {
        watchers.remove(listener);
    }

    public void columnSelectionChanged(ListSelectionEvent arg0) {
        throw new UnsupportedOperationException("columnSelectionChangedBAH");
    }

    ArrayList<Sortables> getOrder() {
        return order;
    }

    List<Sortables> getOrderBeforeDivider() {
        if (!order.contains(Sortables.DIVIDER)) {
            return order;
        }

        return order.subList(0, order.indexOf(Sortables.DIVIDER));
    }

    List<Sortables> getOrderAfterDivider() {
        if (!order.contains(Sortables.DIVIDER) || order.indexOf(Sortables.DIVIDER) == order.size() - 1) {
            return new ArrayList<Sortables>();
        }

        return order.subList(order.indexOf(Sortables.DIVIDER) + 1, order.size());
    }

    private void orderUpdate() {
        // order.clear();
        if (!frozen) {
            order = new ArrayList<Sortables>();
            for (TableColumn c : columnList) {
                order.add((Sortables) c.getIdentifier());
            }
        }
        check();
    }

    public void freezeOrder() {
        frozen = true;
    }

    @SwingThread
    public void thawOrder() {
        frozen = false;
        orderUpdate();
        TreeModel model = MainFrame.getInstance().getTree().getModel();
        if (model instanceof BugTreeModel) {
            ((BugTreeModel) model).checkSorter();
        }
    }
}
