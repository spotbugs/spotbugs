/**
 *
 */
package de.tobject.findbugs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import edu.umd.cs.findbugs.cloud.Cloud.CloudStatusListener;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

public class EclipseGuiCallback implements IGuiCallback {
		private final AbstractExecutorService guiExecutor = new EclipseDisplayThreadExecutor();
	private CloudListener cloudListener;
	private CloudStatusListener cloudStatusListener;
	private Label loginStatusBarItem;
	private Cloud cloud;


	public void registerCloud(Project project, BugCollection collection, final Cloud cloud) {
		this.cloud = cloud;

		// IWorkbenchWindow[] windows = FindbugsPlugin.getDefault().getWorkbench().getWorkbenchWindows();
		IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage();
		IWorkbenchPart part = page.getActivePart();
		// IViewPart btv = page.findView("de.tobject.findbugs.view.bugtreeview");
		// IViewSite btvsite = btv.getViewSite();

		if (part instanceof IEditorPart) {
			final IEditorPart epart = (IEditorPart) part;
			FindbugsPlugin.getShell().getDisplay().asyncExec(new Runnable() {

				public void run() {
					IStatusLineManager slm = epart.getEditorSite().getActionBars().getStatusLineManager();
					slm.add(new ContributionItem() {

						@Override
						public void fill(Composite parent) {
							createLoginStatusBarItem(parent);
							updateLoginStatusBarItem();
						}

					});
					slm.update(true);
				}
			});

		}
		cloudListener = new CloudListener() {
			public void statusUpdated() {
				final String statusMsg = cloud.getStatusMsg();

				final IWorkbenchWindow win = FindbugsPlugin.getActiveWorkbenchWindow();
				win.getShell().getDisplay().asyncExec(new Runnable() {
					@SuppressWarnings("hiding")
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

			public void issueUpdated(BugInstance bug) {

			}
		};
		cloud.addListener(cloudListener);
		cloudStatusListener = new CloudStatusListener() {

			public void handleStateChange(SigninState oldState, SigninState state) {
				updateLoginStatusBarItem();
			}

			public void handleIssueDataDownloadedEvent() {
				// TODO Auto-generated method stub

			}
		};
		cloud.addStatusListener(cloudStatusListener);
	}

	private void showSigninPopupMenu(Point p) {
		Menu popupMenu = new Menu(loginStatusBarItem);
	    MenuItem automaticCheckbox = new MenuItem(popupMenu, SWT.CHECK);
	    automaticCheckbox.setText("Sign in automatically");
	    SigninState state = cloud.getSigninState();
		automaticCheckbox.setEnabled(state != SigninState.NO_SIGNIN_REQUIRED);
	    final boolean origSelection = AppEngineNameLookup.isSavingSessionInfoEnabled();
		automaticCheckbox.setSelection(origSelection);
	    automaticCheckbox.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				AppEngineNameLookup.setSaveSessionInformation(!origSelection);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

	    if (state == SigninState.NOT_SIGNED_IN_YET || state == SigninState.SIGNED_OUT
	    		|| state == SigninState.SIGNIN_FAILED) {
		    MenuItem signInItem = new MenuItem(popupMenu, SWT.NONE);
		    signInItem.setText("Sign in");
		    signInItem.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					try {
						cloud.signIn();
					} catch (IOException e1) {
						MessageDialog.openError(FindbugsPlugin.getShell(), "Error", e1.toString());
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});

	    } else if (state == SigninState.SIGNING_IN || state == SigninState.SIGNED_IN) {
	    	MenuItem signOutItem = new MenuItem(popupMenu, SWT.NONE);
		    signOutItem.setText("Sign out");
		    signOutItem.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					cloud.signOut();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});
	    }
	    popupMenu.setLocation(p);
	    popupMenu.setVisible(true);
	}

	private void updateLoginStatusBarItem() {
		if (loginStatusBarItem != null && cloud != null) {
			loginStatusBarItem.setVisible(cloud.getSigninState() != SigninState.NO_SIGNIN_REQUIRED);
			loginStatusBarItem.setText(cloud.getSigninState().name());
		}
	}

	public void unregisterCloud(Project project, BugCollection collection, Cloud cloud) {
		cloud.removeListener(cloudListener);
		cloud.removeStatusListener(cloudStatusListener);
		this.cloud = null;
	}

	public String showQuestionDialog(String message, String title, String defaultValue) {
		System.out.println(message);
		return defaultValue;
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

	private void createLoginStatusBarItem(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		loginStatusBarItem = new Label(comp, SWT.NONE);
		loginStatusBarItem.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER,
				false, false));
		loginStatusBarItem.setAlignment(SWT.CENTER);
		loginStatusBarItem.setCursor(new Cursor(null, SWT.CURSOR_HAND));
		loginStatusBarItem.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseDown(MouseEvent e) {

			    showSigninPopupMenu(loginStatusBarItem.toDisplay(e.x, e.y));
			}

			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});
	}

	private final class EclipseDisplayThreadExecutor extends AbstractExecutorService {
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
	}

}
