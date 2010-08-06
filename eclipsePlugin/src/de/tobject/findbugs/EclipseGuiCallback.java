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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
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
	private final AbstractExecutorService guiExecutor = new EclipseDisplayThreadExecutor();
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
							Image image = FindbugsPlugin.getDefault().getImageRegistry().get("buggy-tiny-gray.png");
							IStatusLineManager statusLineManager = epart.getEditorSite().getActionBars().getStatusLineManager();
							if (statusMsg.equals("")) {
								statusLineManager.setMessage("");
							} else {
								statusLineManager.setMessage(image, statusMsg);
							}
						}
					}
				});
			}

			public void issueUpdated(BugInstance bug) { // ok
			}
		};
		cloud.addListener(cloudListener);
	}

	public void unregisterCloud(Project project, BugCollection collection, Cloud cloud) {
		cloud.removeListener(cloudListener);
	}

	public String showQuestionDialog(String message, String title, final String defaultValue) {
		final AtomicReference<Text> textBoxRef = new AtomicReference<Text>();
		MessageDialog dlg = new MessageDialog(FindbugsPlugin.getShell(), title, null, message,
				MessageDialog.QUESTION, new String[] {"OK", "Cancel"}, 1) {
					@Override
					protected Control createCustomArea(Composite parent) {
						Text text = new Text(parent, SWT.SINGLE);
						text.setText(defaultValue);
						textBoxRef.set(text);
						return text;
					}

		};

		dlg.open();
		return textBoxRef.get().getText();
	}

	public void showMessageDialogAndWait(String message) throws InterruptedException {
		MessageDialog.openInformation(FindbugsPlugin.getShell(), null, message);
	}

	public void showMessageDialog(final String message) {
		FindbugsPlugin.getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(FindbugsPlugin.getShell(), null, message);
			}
		});
	}

	public List<String> showForm(String message, String title, List<FormItem> labels) {
		throw new UnsupportedOperationException();
	}

	public boolean showDocument(URL u) {
		return Program.launch(u.toExternalForm());
	}

	public int showConfirmDialog(final String message, final String title, final String ok, final String cancel) {
		final AtomicInteger result = new AtomicInteger(-1);
		FindbugsPlugin.getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog dialog = new MessageDialog(FindbugsPlugin.getShell(), title, null, message,
						MessageDialog.NONE, new String[] { ok, cancel }, 0) /*{
					{	// the code below requires Eclipse 3.5
						setShellStyle(getShellStyle() | SWT.SHEET);
					}
				}*/;
				result.set(dialog.open());
			}
		});
		return result.get();
	}

	public void setErrorMessage(String errorMsg) {
		showMessageDialog(errorMsg);
	}

	public boolean isHeadless() {
		return false;
	}

	public void invokeInGUIThread(Runnable r) {
		FindbugsPlugin.getShell().getDisplay().asyncExec(r);
	}

	public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
		return in;
	}

	public ExecutorService getBugUpdateExecutor() {
		return guiExecutor;
	}

	public void displayNonmodelMessage(final String title, final String message) {
		invokeInGUIThread(new Runnable() {
			public void run() {
				MessageDialog.openInformation(FindbugsPlugin.getShell(), title, message);
			}
		});
	}

	private final static class EclipseDisplayThreadExecutor extends AbstractExecutorService {
		public boolean awaitTermination(long timeout, TimeUnit unit)
				throws InterruptedException {
			return false;
		}

		public boolean isShutdown() {
			return false;
		}

		public boolean isTerminated() {
			return false;
		}

		public void shutdown() {
			return;
		}

		public List<Runnable> shutdownNow() {
			return null;
		}

		public void execute(Runnable command) {
			Display.getDefault().asyncExec(command);
		}
	}

}
