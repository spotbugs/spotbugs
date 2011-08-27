/*
 * Contributions to FindBugs
 * Copyright (C) 2011, Andrei Loskutov
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
package de.tobject.findbugs.view;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Andrey Loskutov
 */
public class FindBugsPerspectiveFactory implements IPerspectiveFactory {

    /** perspective id, see plugin.xml */
    public static final String ID = "de.tobject.findbugs.FindBugsPerspective";

    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.25, editorArea);
        topLeft.addView(FindbugsPlugin.TREE_VIEW_ID);
        topLeft.addPlaceholder(JavaUI.ID_PACKAGES);

        // Bottom right.
        IFolderLayout bottomRightA = layout.createFolder("bottomRightA", IPageLayout.BOTTOM, (float) 0.55, editorArea);

        bottomRightA.addView(FindbugsPlugin.USER_ANNOTATIONS_VIEW_ID);


        IFolderLayout bottomRightB = layout.createFolder("bottomRightB", IPageLayout.RIGHT, (float) 0.45, "bottomRightA");

        bottomRightB.addView(FindbugsPlugin.DETAILS_VIEW_ID);
    }

}
