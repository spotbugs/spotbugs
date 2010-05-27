/**
 *
 */
package de.tobject.findbugs;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.CloudListener;

public class EclipseGuiCallback implements IGuiCallback {
	private final AbstractExecutorService guiExecutor = new AbstractExecutorService() {

		public boolean awaitTermination(long timeout, TimeUnit unit)
				throws InterruptedException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isShutdown() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isTerminated() {
			// TODO Auto-generated method stub
			return false;
		}

		public void shutdown() {
			// TODO Auto-generated method stub

		}

		public List<Runnable> shutdownNow() {
			// TODO Auto-generated method stub
			return null;
		}

		public void execute(Runnable command) {
			Display.getDefault().asyncExec(command);
		}

	};
	private CloudListener cloudListener;


	public void registerCloud(Project project, BugCollection collection, final Cloud cloud) {
		cloudListener = new CloudListener() {
			public void statusUpdated() {
				final String statusMsg = cloud.getStatusMsg();

				final IWorkbenchWindow win = FindbugsPlugin.getActiveWorkbenchWindow();
				win.getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						IWorkbenchPage page = win.getActivePage();

						IWorkbenchPart part = page.getActivePart();

						if (part instanceof IEditorPart) {
							IEditorPart epart = (IEditorPart) part;
							epart.getEditorSite().getActionBars().getStatusLineManager().setMessage(statusMsg);
						}
					}
				});
			}

			public void issueUpdated(BugInstance bug) {

			}
		};
		cloud.addListener(cloudListener);
	}

	public void unregisterCloud(Project project, BugCollection collection, Cloud cloud) {
		cloud.removeListener(cloudListener);
	}

	public String showQuestionDialog(String message, String title, String defaultValue) {
		throw new UnsupportedOperationException();
	}

	public void showMessageDialogAndWait(String message) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	public void showMessageDialog(String message) {
		throw new UnsupportedOperationException();
	}

	public List<String> showForm(String message, String title, List<FormItem> labels) {
		throw new UnsupportedOperationException();
	}

	public boolean showDocument(URL u) {
		return Program.launch(u.toExternalForm());
	}

	public int showConfirmDialog(String message, String title, String ok, String cancel) {
		MessageDialog dialog = new MessageDialog(FindbugsPlugin.getShell(), title, null, message,
				MessageDialog.NONE, new String[] { ok, cancel }, 0) {
			{
				setShellStyle(getShellStyle() | SWT.SHEET);
			}
		};
		return dialog.open();
	}

	public void setErrorMessage(String errorMsg) {
		throw new UnsupportedOperationException();
	}

	public boolean isHeadless() {
		return false;
	}

	public void invokeInGUIThread(Runnable r) {
		getBugUpdateExecutor().execute(r);
	}

	public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
		return in;
	}

	public ExecutorService getBugUpdateExecutor() {
		return guiExecutor;
	}

	public void displayNonmodelMessage(String title, String message) {
		throw new UnsupportedOperationException();
	}
}
