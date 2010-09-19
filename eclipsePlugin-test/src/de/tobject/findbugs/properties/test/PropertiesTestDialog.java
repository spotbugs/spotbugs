/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.properties.test;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPropertyPage;

/**
 * Dialog class to open the properties page.
 * 
 * @author Tomás Pollak
 */
public class PropertiesTestDialog extends Dialog {
    private final IWorkbenchPropertyPage page;

    protected PropertiesTestDialog(Shell parentShell, IWorkbenchPropertyPage page) {
        super(parentShell);
        this.page = page;
    }

    @Override
    public void cancelPressed() {
        page.performCancel();
        super.cancelPressed();
    }

    @Override
    public boolean close() {
        page.dispose();
        return super.close();
    }

    @Override
    public void okPressed() {
        page.performOk();
        super.okPressed();
    }

    @Override
    public int open() {
        setBlockOnOpen(false);
        return super.open();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        page.createControl(composite);
        return composite;
    }

}
