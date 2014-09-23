/**
 *
 */
package de.tobject.findbugs;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import edu.umd.cs.findbugs.cloud.Cloud.CloudTask;
import edu.umd.cs.findbugs.cloud.Cloud.CloudTaskListener;

public class EclipseGuiCallback implements IGuiCallback {
    private final AbstractExecutorService guiExecutor = new EclipseDisplayThreadExecutor();

    private CloudListener cloudListener;

    private final IProject iproject;

    public EclipseGuiCallback(IProject iproject) {
        super();
        this.iproject = iproject;
    }

    @Override
    public void registerCloud(Project project, BugCollection collection, final Cloud cloud) {
        cloudListener = new CloudListener() {
            @Override
            public void statusUpdated() {
                final String statusMsg = cloud.getStatusMsg();

                final IWorkbenchWindow win = FindbugsPlugin.getActiveWorkbenchWindow();
                win.getShell().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        IWorkbenchPage page = win.getActivePage();
                        IWorkbenchPart part = page.getActivePart();

                        if (part instanceof IEditorPart) {
                            IEditorPart epart = (IEditorPart) part;
                            Image image = FindbugsPlugin.getDefault().getImageRegistry().get("buggy-tiny-gray.png");
                            IStatusLineManager statusLineManager = epart.getEditorSite().getActionBars().getStatusLineManager();
                            if (statusMsg.isEmpty()) {
                                statusLineManager.setMessage("");
                            } else {
                                statusLineManager.setMessage(image, statusMsg);
                            }
                        }
                    }
                });
            }

            @Override
            public void issueUpdated(BugInstance bug) { // ok
            }

            @Override
            public void taskStarted(final CloudTask task) {
                task.setUseDefaultListener(false);
                Job job = new Job(task.getName()) {

                    @Override
                    public boolean belongsTo(Object family) {
                        return FindbugsPlugin.class == family;
                    }

                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {
                        monitor.beginTask(task.getName(), 1000);
                        monitor.subTask(task.getStatusLine());
                        monitor.worked((int) (task.getPercentCompleted() * 10));

                        final CountDownLatch latch = new CountDownLatch(1);
                        final AtomicBoolean success = new AtomicBoolean(false);
                        task.addListener(new CloudTaskListener() {

                            @Override
                            public void taskStatusUpdated(String statusLine, double percentCompleted) {
                                monitor.subTask(statusLine);
                                monitor.worked((int) (percentCompleted * 10)); // out
                                                                               // of
                                                                               // 1000
                                                                               // work
                                                                               // units
                            }

                            @Override
                            public void taskFinished() {
                                latch.countDown();
                                success.set(true);
                            }

                            @Override
                            public void taskFailed(String message) {
                                latch.countDown();
                                success.set(false);
                            }
                        });
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            // ok
                        }
                        monitor.subTask("");
                        monitor.done();
                        return Status.OK_STATUS;
                    }
                };
                job.setPriority(Job.SHORT);
                job.schedule();
            }
        };
        cloud.addListener(cloudListener);
    }

    @Override
    public void unregisterCloud(Project project, BugCollection collection, Cloud cloud) {
        cloud.removeListener(cloudListener);
    }

    public String getProjectName() {
        try {
            return iproject.getDescription().getName();
        } catch (CoreException e) {
            return iproject.getName();
        }
    }

    public String getDialogTitle() {
        return getProjectName() + ": FindBugs";
    }

    public String getDialogTitle(String title) {
        return getProjectName() + ": " + title;
    }

    @Override
    public String showQuestionDialog(String message, String title, final String defaultValue) {
        final AtomicReference<Text> textBoxRef = new AtomicReference<Text>();
        MessageDialog dlg = new MessageDialog(FindbugsPlugin.getShell(), getDialogTitle(title), null, message, MessageDialog.QUESTION,
                new String[] { "OK", "Cancel" }, 1) {
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

    @Override
    public void showMessageDialogAndWait(String message) throws InterruptedException {
        MessageDialog.openInformation(FindbugsPlugin.getShell(), getDialogTitle(), message);
    }

    @Override
    public void showMessageDialog(final String message) {
        FindbugsPlugin.getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(FindbugsPlugin.getShell(), getDialogTitle(), message);
            }
        });
    }

    @Override
    public List<String> showForm(String message, String title, List<FormItem> labels) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean showDocument(URL u) {
        return Program.launch(u.toExternalForm());
    }

    @Override
    public int showConfirmDialog(final String message, final String title, final String ok, final String cancel) {
        final AtomicInteger result = new AtomicInteger(-1);
        FindbugsPlugin.getShell().getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog dialog = new MessageDialog(FindbugsPlugin.getShell(), getDialogTitle(title), null, message, MessageDialog.NONE,
                        new String[] { ok, cancel }, 0) /*
                                                         * { { // the code below
                                                         * requires Eclipse 3.5
                                                         * setShellStyle
                                                         * (getShellStyle() |
                                                         * SWT.SHEET); } }
                                                         */;
                result.set(dialog.open());
            }
        });
        return result.get();
    }

    @Override
    public void setErrorMessage(String errorMsg) {
        showMessageDialog(errorMsg);
    }

    @Override
    public boolean isHeadless() {
        return false;
    }

    @Override
    public void invokeInGUIThread(Runnable r) {
        FindbugsPlugin.getShell().getDisplay().asyncExec(r);
    }

    @Override
    public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
        return in;
    }

    @Override
    public ExecutorService getBugUpdateExecutor() {
        return guiExecutor;
    }

    @Override
    public void displayNonmodelMessage(final String title, final String message) {
        invokeInGUIThread(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(FindbugsPlugin.getShell(), getDialogTitle(title), message);
            }
        });
    }

    private final static class EclipseDisplayThreadExecutor extends AbstractExecutorService {
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public void shutdown() {
            return;
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public void execute(Runnable command) {
            Display.getDefault().asyncExec(command);
        }
    }

}
