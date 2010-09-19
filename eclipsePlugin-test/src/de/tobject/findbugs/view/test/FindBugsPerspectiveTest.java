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
package de.tobject.findbugs.view.test;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.AbstractPluginTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the FindBugsPerspectiveFactory.
 * 
 * @author Tomás Pollak
 */
public class FindBugsPerspectiveTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private static final String FINDBUGS_PERSPECTIVE_ID = "de.tobject.findbugs.FindBugsPerspective";

    @Test
    public void testShowPerspective() throws WorkbenchException {
        // Show the perspective
        IWorkbenchPage page = showFindBugsPerspective();

        // Reset the perspective to its default state
        page.resetPerspective();

        // Assert the FindBugs explorer view is visible
        IViewPart bugExplorerView = page.findView(AbstractPluginTest.BUG_EXPLORER_VIEW_ID);
        assertNotNull(bugExplorerView);
        assertTrue(page.isPartVisible(bugExplorerView));
    }

    private IWorkbenchPage showFindBugsPerspective() throws WorkbenchException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = PlatformUI.getWorkbench().showPerspective(FINDBUGS_PERSPECTIVE_ID, window);
        return page;
    }
}
