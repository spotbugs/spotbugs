/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.SelectionDialog;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Andrei
 * 
 */
public class GroupSelectionDialog extends SelectionDialog {

    private final List<GroupType> allowedGroups;

    private final List<GroupType> preSelectedGroups;

    private final Map<GroupType, Boolean> selectionMap;

    private CheckboxTableViewer checkList;

    private Button upButton;

    private Button downButton;

    public GroupSelectionDialog(Shell parentShell, List<GroupType> selectedGroups) {
        super(parentShell);
        this.preSelectedGroups = selectedGroups;
        this.allowedGroups = GroupType.getVisible();
        selectionMap = new HashMap<GroupType, Boolean>();

        initSelections();
    }

    private void initSelections() {
        Collections.reverse(preSelectedGroups);
        for (GroupType type : preSelectedGroups) {
            if (allowedGroups.remove(type)) {
                allowedGroups.add(0, type);
            }
        }
        Collections.reverse(preSelectedGroups);

        for (GroupType groupType : allowedGroups) {
            selectionMap.put(groupType, Boolean.valueOf(preSelectedGroups.contains(groupType)));
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        int columns = 2;
        composite.setLayout(new GridLayout(columns, false));
        GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        layoutData.minimumHeight = 200;
        layoutData.minimumWidth = 250;
        layoutData.heightHint = 200;
        layoutData.widthHint = 250;

        composite.setLayoutData(layoutData);

        checkList = CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER | SWT.RESIZE | SWT.V_SCROLL
                | SWT.H_SCROLL);

        Table table = checkList.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(false);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableColumn nameColumn = new TableColumn(table, SWT.NONE);
        nameColumn.setText("Group Visibility / Sort Order");
        nameColumn.setResizable(true);
        nameColumn.setWidth(170);
        checkList.setContentProvider(new ArrayContentProvider());
        // ITableLabelProvider labelProvider= new SeparateTableLabelProvider();
        // checkList.setLabelProvider(labelProvider);
        checkList.setInput(allowedGroups);
        checkList.setCheckedElements(preSelectedGroups.toArray());

        checkList.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                boolean checked = event.getChecked();
                GroupType element = (GroupType) event.getElement();
                selectionMap.put(element, Boolean.valueOf(checked));
            }
        });

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleTableSelection();
            }
        });

        createButtonList(composite);
        return composite;
    }

    private void createButtonList(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        upButton = new Button(composite, SWT.PUSH | SWT.CENTER);
        upButton.setText("Up");
        upButton.setEnabled(false);
        upButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = getSelectionIndex();
                if (index != -1) {
                    moveUp(allowedGroups.get(index));
                    checkList.refresh();
                    handleTableSelection();
                }
            }
        });
        GridData data = new GridData();
        data.widthHint = 50;
        data.horizontalAlignment = GridData.FILL;
        upButton.setLayoutData(data);

        downButton = new Button(composite, SWT.PUSH | SWT.CENTER);
        downButton.setText("Down");
        downButton.setEnabled(false);
        downButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = getSelectionIndex();
                if (index != -1) {
                    moveDown(allowedGroups.get(index));
                    checkList.refresh();
                    handleTableSelection();
                }
            }
        });
        data = new GridData();
        data.widthHint = 50;
        data.horizontalAlignment = GridData.FILL;
        downButton.setLayoutData(data);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
        IDialogSettings section = dialogSettings.getSection("GroupSelectionDialog");
        if (section == null) {
            dialogSettings.addNewSection("GroupSelectionDialog");
        }
        return section;
    }

    void moveUp(GroupType type) {
        int indexOf = allowedGroups.indexOf(type);
        allowedGroups.remove(indexOf);
        allowedGroups.add(indexOf - 1, type);
    }

    void moveDown(GroupType type) {
        int indexOf = allowedGroups.indexOf(type);
        allowedGroups.remove(indexOf);
        allowedGroups.add(indexOf + 1, type);
    }

    private void handleTableSelection() {
        GroupType item = getSelectedItem();
        if (item != null) {
            int index = getSelectionIndex();
            upButton.setEnabled(index > 0);
            downButton.setEnabled(index < allowedGroups.size() - 1);
        } else {
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        }
    }

    private GroupType getSelectedItem() {
        return (GroupType) ((IStructuredSelection) checkList.getSelection()).getFirstElement();
    }

    private int getSelectionIndex() {
        return checkList.getTable().getSelectionIndex();
    }

    public List<GroupType> getGroups() {
        List<GroupType> selected = new ArrayList<GroupType>();
        for (GroupType groupType : allowedGroups) {
            if (selectionMap.get(groupType).booleanValue()) {
                selected.add(groupType);
            }
        }
        return selected;
    }
}
