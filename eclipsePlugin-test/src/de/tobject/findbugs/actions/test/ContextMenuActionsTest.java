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
package de.tobject.findbugs.actions.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.actions.ClearMarkersAction;
import de.tobject.findbugs.actions.FindBugsAction;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;

/**
 * This class tests the FindBugsAction, SaveXMLAction and LoadXMLAction.
 *
 * @author Tomás Pollak
 */
class ContextMenuActionsTest extends AbstractFindBugsTest {
    @BeforeAll
    static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterAll
    static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private File tempFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tempFile = File.createTempFile("bugs", ".xml");
        assertTrue(tempFile.delete());
    }

    @Override
    public void tearDown() throws CoreException {
        tempFile.delete();
        super.tearDown();
    }

    @Test
    void testClearFindBugs() throws CoreException {
        assertNoBugs();

        StructuredSelection selection = new StructuredSelection(getProject());
        FindBugsAction action = new FindBugsAction();
        action.selectionChanged(null, selection);
        action.run(null);

        joinJobFamily(FindbugsPlugin.class);

        assertExpectedBugs();

        ClearMarkersAction clearAction = new ClearMarkersAction();
        clearAction.selectionChanged(null, selection);
        clearAction.run(null);

        joinJobFamily(FindbugsPlugin.class);

        assertBugsCount(0, getProject());
    }

    @Test
    void testLoadXML() throws CoreException {
        assertNoBugs();

        StructuredSelection selection = new StructuredSelection(getProject());
        IActionDelegate action = new LoadXMLActionTestSubclass(getBugsFileLocation());
        action.selectionChanged(null, selection);
        action.run(null);

        joinJobFamily(FindbugsPlugin.class);

        assertExpectedBugs();
    }

    @Test
    void testRunFindBugs() throws CoreException {
        assertNoBugs();

        StructuredSelection selection = new StructuredSelection(getProject());
        FindBugsAction action = new FindBugsAction();
        action.selectionChanged(null, selection);
        action.run(null);

        joinJobFamily(FindbugsPlugin.class);

        assertExpectedBugs();
    }

    @Test
    void testSaveXML() throws CoreException {
        assertNoBugs();
        work(createFindBugsWorker());
        assertExpectedBugs();

        StructuredSelection selection = new StructuredSelection(getProject());
        IActionDelegate action = new SaveXMLActionTestSubclass(getTempFilePath());
        action.selectionChanged(null, selection);
        action.run(null);

        joinJobFamily(FindbugsPlugin.class);

        clearBugsState();
        assertNoBugs();

        loadXml(createFindBugsWorker(), getTempFilePath());
        assertExpectedBugs();
    }

    private String getTempFilePath() {
        return tempFile.getAbsolutePath();
    }
}
