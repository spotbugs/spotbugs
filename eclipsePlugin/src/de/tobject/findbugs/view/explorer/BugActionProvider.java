package de.tobject.findbugs.view.explorer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import de.tobject.findbugs.actions.MarkerShowDetailsAction;

public class BugActionProvider extends CommonActionProvider {

	/**
	 * @author Andrei
	 */
	public class MyAction extends Action implements ISelectionChangedListener {
		private MarkerShowDetailsAction action;

		public MyAction() {
			super();
			action = new MarkerShowDetailsAction();
		}

		public void selectionChanged(SelectionChangedEvent event) {
			action.selectionChanged(this, event.getSelection());
		}

		@Override
		public void run() {
			action.run(this);
		}

		void setSelection(ISelection sel) {
			action.selectionChanged(this, sel);
		}
	}

	private MyAction doubleClickAction;

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);

		doubleClickAction = new MyAction();

		// only if doubleClickAction must know tree selection:
//		aSite.getStructuredViewer().addSelectionChangedListener(doubleClickAction);
//		aSite.getStructuredViewer().addDoubleClickListener(doubleClickAction);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		// super.fillActionBars(actionBars);

		IStructuredSelection selection = (IStructuredSelection) getContext()
				.getSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof IMarker) {
			super.fillActionBars(actionBars);
			// forward doubleClick to doubleClickAction
			doubleClickAction.setSelection(selection);
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN,
					doubleClickAction);
		}
	}
}
