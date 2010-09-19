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

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabFolder;

import de.tobject.findbugs.properties.FindbugsPropertyPage;
import de.tobject.findbugs.properties.ReportConfigurationTab;

/**
 * Test subclass of ReportConfigurationTab that provides methods for testing
 * purposes.
 * 
 * @author Tomás Pollak
 */
public class ReportConfigurationTabTestSubclass extends ReportConfigurationTab {

    public ReportConfigurationTabTestSubclass(TabFolder tabFolder, FindbugsPropertyPage page, int style) {
        super(tabFolder, page, style);
    }

    public void deselectAllBugCategories() {
        for (Button button : getChkEnableBugCategoryList()) {
            button.setSelection(false);
        }
        syncSelectedCategories();
    }

    public void selectBugCategory(String category) {
        for (Button button : getChkEnableBugCategoryList()) {
            button.setSelection(button.getData().equals(category));
        }
        syncSelectedCategories();
    }

}
