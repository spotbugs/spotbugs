/**
 *
 */
package de.tobject.findbugs.view;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Andrei Loskutov
 */
public class FindBugsPerspectiveFactory implements IPerspectiveFactory {

	/** perspective id, see plugin.xml */
	static final String ID = "de.tobject.findbugs.FindBugsPerspective";

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.33, editorArea);
		topLeft.addView(FindbugsPlugin.TREE_VIEW_ID);
		topLeft.addView(IPageLayout.ID_PROBLEM_VIEW);
		topLeft.addPlaceholder(JavaUI.ID_PACKAGES);

		// Bottom left.
		IFolderLayout bottomLeft = layout.createFolder(
				"bottomLeft", IPageLayout.BOTTOM, (float) 0.85,
				"topLeft");
		bottomLeft.addView(FindbugsPlugin.USER_ANNOTATIONS_VIEW_ID);

		// Bottom right.
		IFolderLayout bottomRight = layout.createFolder(
				"bottomRight", IPageLayout.BOTTOM, (float) 0.55,
				editorArea);

		bottomRight.addView(FindbugsPlugin.DETAILS_VIEW_ID);
		bottomRight.addView(IPageLayout.ID_BOOKMARKS);
		bottomRight.addView(IPageLayout.ID_TASK_LIST);
		bottomRight.addView(IPageLayout.ID_PROGRESS_VIEW);
	}

}
