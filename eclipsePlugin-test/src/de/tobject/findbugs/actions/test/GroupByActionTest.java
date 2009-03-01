package de.tobject.findbugs.actions.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PartInitException;
import org.junit.Test;

import de.tobject.findbugs.actions.GroupByAction;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.view.explorer.GroupType;

public class GroupByActionTest extends AbstractFindBugsTest {

	private static final String PRIORITY_CATEGORY_PROJECT_PACKAGE_CLASS_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Priority.Category.Project.Package.Class.PatternType.Pattern.Marker";
	private static final String PRIORITY_CATEGORY_PROJECT_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Priority.Category.Project.PatternType.Pattern.Marker";
	private static final String PRIORITY_PROJECT_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Priority.Project.Pattern.Marker";
	private static final String PROJECT_PRIORITY_CATEGORY_PATTERN_TYPE_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Priority.Category.PatternType.Pattern.Marker";
	private static final String PROJECT_PRIORITY_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Priority.Pattern.Marker";
	private static final String PROJECT_PATTERN_MARKER_ID = "findBugsEclipsePlugin.toggleGrouping.Project.Pattern.Marker";

	private GroupByAction action;

	@Override
	public void setUp() throws CoreException, IOException {
		super.setUp();

		action = new GroupByAction();
		action.init(showBugExplorerView());
	}

	@Override
	public void tearDown() throws CoreException {
		super.tearDown();
	}

	@Test
	public void testAction_Priority_Category_Project_Package_Class_PatternType_Pattern_Marker()
			throws PartInitException {
		runAction(PRIORITY_CATEGORY_PROJECT_PACKAGE_CLASS_PATTERN_TYPE_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Priority, GroupType.Category,
				GroupType.Project, GroupType.Package, GroupType.Class,
				GroupType.PatternType, GroupType.Pattern, GroupType.Marker);
	}

	@Test
	public void testAction_Priority_Category_Project_PatternType_Pattern_Marker()
			throws PartInitException {
		runAction(PRIORITY_CATEGORY_PROJECT_PATTERN_TYPE_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Priority, GroupType.Category,
				GroupType.Project, GroupType.PatternType, GroupType.Pattern,
				GroupType.Marker);
	}

	@Test
	public void testAction_Priority_Project_Pattern_Marker() throws PartInitException {
		runAction(PRIORITY_PROJECT_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Priority, GroupType.Project,
				GroupType.Pattern, GroupType.Marker);
	}

	@Test
	public void testAction_Project_Pattern_Marker() throws PartInitException {
		runAction(PROJECT_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Project, GroupType.Pattern, GroupType.Marker);
	}

	@Test
	public void testAction_Project_Priority_Category_PatternType_Pattern_Marker()
			throws PartInitException {
		runAction(PROJECT_PRIORITY_CATEGORY_PATTERN_TYPE_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Project, GroupType.Priority,
				GroupType.Category, GroupType.PatternType, GroupType.Pattern,
				GroupType.Marker);
	}

	@Test
	public void testAction_Project_Priority_Pattern_Marker() throws PartInitException {
		runAction(PROJECT_PRIORITY_PATTERN_MARKER_ID);

		assertExpectedGroupTypes(GroupType.Project, GroupType.Priority,
				GroupType.Pattern, GroupType.Marker);
	}

	private void assertExpectedGroupTypes(GroupType... expectedTypes)
			throws PartInitException {
		List<GroupType> expectedGroupTypes = Arrays.asList(expectedTypes);
		List<GroupType> actualGroupTypes = getBugContentProvider().getGrouping().asList();
		assertEquals(expectedGroupTypes, actualGroupTypes);
	}

	private void runAction(String actionId) {
		IAction proxyAction = new ProxyAction(actionId);
		action.run(proxyAction);
	}

	private static class ProxyAction extends Action implements IAction {
		public ProxyAction(String id) {
			setId(id);
		}
	}
}
