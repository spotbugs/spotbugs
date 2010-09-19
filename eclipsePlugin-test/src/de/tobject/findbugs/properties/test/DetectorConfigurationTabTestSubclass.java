/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom�s Pollak
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

import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TableItem;

import de.tobject.findbugs.properties.DetectorConfigurationTab;
import de.tobject.findbugs.properties.FindbugsPropertyPage;
import edu.umd.cs.findbugs.DetectorFactory;

/**
 * Test subclass of DetectorConfigurationTab that provides methods for testing
 * purposes.
 * 
 * @author Tom�s Pollak
 */
public class DetectorConfigurationTabTestSubclass extends DetectorConfigurationTab {

    public DetectorConfigurationTabTestSubclass(TabFolder tabFolder, FindbugsPropertyPage page, int style) {
        super(tabFolder, page, style);
    }

    public void disableAllDetectors() {
        TableItem[] items = availableFactoriesTableViewer.getTable().getItems();
        for (int i = 0; i < items.length; i++) {
            items[i].setChecked(false);
        }
        syncUserPreferencesWithTable();
    }

    public void enableDetector(String detectorShortName) {
        TableItem[] items = availableFactoriesTableViewer.getTable().getItems();
        for (int i = 0; i < items.length; i++) {
            DetectorFactory detectorFactory = (DetectorFactory) items[i].getData();
            boolean enable = detectorFactory.getShortName().equals(detectorShortName);
            items[i].setChecked(enable);
        }
        syncUserPreferencesWithTable();
    }
}
