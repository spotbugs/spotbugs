/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
package de.tobject.findbugs.properties;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * List box with two buttons on the left side: "add" and "remove". First the
 * viewer must be created, then the buttons.
 * 
 * @author andrei
 */
public class ManagePathsWidget extends Composite {

    private ListViewer viewer;

    public ManagePathsWidget(Composite parent) {
        super(parent, SWT.NONE);
    }

    public ListViewer createViewer(String title, String linkText) {
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        Label titleLabel = new Label(this, SWT.NULL);

        titleLabel.setText(title);
        titleLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false, 2, 1));

        if (linkText != null) {
            Link details = new Link(this, SWT.NULL);
            details.setText(linkText);
            details.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false, 2, 1));
            details.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    Program.launch(e.text);
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                    // noop
                }
            });
        }

        viewer = new ListViewer(this, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
        return viewer;
    }

    public void createButtonsArea(PathsProvider contentProvider) {
        final Button addButton = new Button(this, SWT.PUSH);
        String addButtonLabel = getMessage("property.addbutton");

        addButton.setText(addButtonLabel);
        addButton.setData("add");
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

        addButton.addSelectionListener(contentProvider);
        final Button removeButton = new Button(this, SWT.PUSH);
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, true));
        String removeButtonLabel = getMessage("property.removebutton");

        removeButton.setText(removeButtonLabel);
        removeButton.setData("remove");
        removeButton.setEnabled(false);
        removeButton.addSelectionListener(contentProvider);

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                removeButton.setEnabled(!event.getSelection().isEmpty());
            }
        });
    }

    protected static String getMessage(String key) {
        return FindbugsPlugin.getDefault().getMessage(key);
    }
}
