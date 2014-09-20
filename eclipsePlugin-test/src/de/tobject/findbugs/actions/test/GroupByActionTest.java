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
package de.tobject.findbugs.actions.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import de.tobject.findbugs.actions.GroupByAction;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;
import de.tobject.findbugs.view.explorer.GroupType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PartInitException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests the GroupByAction.
 *
 * @author Tom�s Pollak
 */
public class GroupByActionTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private static final String PRIORITY_CATEGORY_PROJECT_PACKAGE_CLASS_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Confidence.Category.Project.Package.Class.PatternType.Pattern.Marker";

    private static final String PRIORITY_CATEGORY_PROJECT_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Confidence.Category.Project.PatternType.Pattern.Marker";

    private static final String PRIORITY_PROJECT_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Confidence.Project.Pattern.Marker";

    private static final String PROJECT_PRIORITY_CATEGORY_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Confidence.Category.PatternType.Pattern.Marker";

    private static final String PROJECT_PRIORITY_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Confidence.Pattern.Marker";

    private static final String PROJECT_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Pattern.Marker";

    private GroupByAction action;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        action = new GroupByAction();
        action.init(showBugExplorerView());
    }

    @Override
    public void tearDown() throws CoreException {
        action = null;

        super.tearDown();
    }

    @Test
    public void testAction_Priority_Category_Project_Package_Class_PatternType_Pattern_Marker() throws PartInitException {
        runAction(PRIORITY_CATEGORY_PROJECT_PACKAGE_CLASS_PATTERN_TYPE_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Confidence, GroupType.Category, GroupType.Project, GroupType.Package, GroupType.Class,
                GroupType.PatternType, GroupType.Pattern, GroupType.Marker);
    }

    @Test
    public void testAction_Priority_Category_Project_PatternType_Pattern_Marker() throws PartInitException {
        runAction(PRIORITY_CATEGORY_PROJECT_PATTERN_TYPE_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Confidence, GroupType.Category, GroupType.Project, GroupType.PatternType,
                GroupType.Pattern, GroupType.Marker);
    }

    @Test
    public void testAction_Priority_Project_Pattern_Marker() throws PartInitException {
        runAction(PRIORITY_PROJECT_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Confidence, GroupType.Project, GroupType.Pattern, GroupType.Marker);
    }

    @Test
    public void testAction_Project_Pattern_Marker() throws PartInitException {
        runAction(PROJECT_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Project, GroupType.Pattern, GroupType.Marker);
    }

    @Test
    public void testAction_Project_Priority_Category_PatternType_Pattern_Marker() throws PartInitException {
        runAction(PROJECT_PRIORITY_CATEGORY_PATTERN_TYPE_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Project, GroupType.Confidence, GroupType.Category, GroupType.PatternType,
                GroupType.Pattern, GroupType.Marker);
    }

    @Test
    public void testAction_Project_Priority_Pattern_Marker() throws PartInitException {
        runAction(PROJECT_PRIORITY_PATTERN_MARKER_ID);

        assertExpectedGroupTypes(GroupType.Project, GroupType.Confidence, GroupType.Pattern, GroupType.Marker);
    }

    private void assertExpectedGroupTypes(GroupType... expectedTypes) throws PartInitException {
        List<GroupType> expectedGroupTypes = Arrays.asList(expectedTypes);
        List<GroupType> actualGroupTypes = getBugContentProvider().getGrouping().asList();
        assertEquals(expectedGroupTypes, actualGroupTypes);
    }

    private void runAction(String actionId) {
        IAction proxyAction = new ProxyAction(actionId);
        action.run(proxyAction);
    }

    private static class ProxyAction extends Action {
        public ProxyAction(String id) {
            setId(id);
        }
    }
}
