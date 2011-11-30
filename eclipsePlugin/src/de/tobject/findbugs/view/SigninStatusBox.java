package de.tobject.findbugs.view;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.CloudStatusListener;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;

public class SigninStatusBox extends Composite {
    private final CloudStatusListener cloudStatusListener;

    private Cloud cloud;

    private Label statusLine;

    public SigninStatusBox(Composite parent, int style) {
        super(parent, style);

        cloudStatusListener = new CloudStatusListener() {
            public void handleStateChange(SigninState oldState, SigninState state) {
                updateLabel();
            }

            public void handleIssueDataDownloadedEvent() { // ok
            }
        };
        createControls();
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });
        addControlListener(new ControlListener() {

            public void controlResized(ControlEvent e) {
                statusLine.pack(true);
                SigninStatusBox.this.pack(true);
            }

            public void controlMoved(ControlEvent e) {
                // noop
            }
        });
    }

    @Override
    public void dispose() {
        if (statusLine.isDisposed()) {
            return;
        }
        statusLine.getCursor().dispose();
        super.dispose();
        if (this.cloud != null) {
            this.cloud.removeStatusListener(cloudStatusListener);
        }
    }

    public void setCloud(Cloud cloud) {
        if (this.cloud != null) {
            this.cloud.removeStatusListener(cloudStatusListener);
        }
        this.cloud = cloud;
        if (cloud != null) {
            cloud.addStatusListener(cloudStatusListener);
        }
        updateLabel();
    }

    private void createControls() {
        this.setLayout(new GridLayout(1, true));
        statusLine = new Label(this, SWT.NONE);
        statusLine.setLayoutData(new GridData(SWT.RIGHT, GridData.CENTER, true, false));
        statusLine.setAlignment(SWT.CENTER);
        statusLine.setCursor(new Cursor(null, SWT.CURSOR_HAND));
        statusLine.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                showSigninPopupMenu(statusLine.toDisplay(e.x, e.y));
            }
        });
        updateLabel();
    }

    private void showSigninPopupMenu(Point p) {
        Menu popupMenu = new Menu(this);
        MenuItem automaticCheckbox = new MenuItem(popupMenu, SWT.CHECK);
        automaticCheckbox.setText("Sign in automatically");
        SigninState state = cloud.getSigninState();
        automaticCheckbox.setEnabled(state != SigninState.NO_SIGNIN_REQUIRED);
        final boolean origSelection = WebCloudNameLookup.isSavingSessionInfoEnabled();
        automaticCheckbox.setSelection(origSelection);
        automaticCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                WebCloudNameLookup.setSaveSessionInformation(!origSelection);
            }
        });

        if (state.couldSignIn()) {
            MenuItem signInItem = new MenuItem(popupMenu, SWT.NONE);
            signInItem.setText("Sign in");
            signInItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        cloud.signIn();
                    } catch (IOException e1) {
                        MessageDialog.openError(FindbugsPlugin.getShell(), "Error", e1.toString());
                    }
                }
            });

        } else if (state == SigninState.SIGNING_IN || state == SigninState.SIGNED_IN) {
            MenuItem signOutItem = new MenuItem(popupMenu, SWT.NONE);
            signOutItem.setText("Sign out");
            signOutItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    cloud.signOut();
                }
            });
        }
        popupMenu.setLocation(p);
        popupMenu.setVisible(true);
    }

    private void updateLabel() {
        FindbugsPlugin.getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (statusLine != null && !statusLine.isDisposed()) {
                    if (cloud != null) {
                        String text = cloud.getCloudName() + "\n" + cloud.getSigninState();
                        if (cloud.getUser() != null) {
                            text += ": " + cloud.getUser();
                        }
                        statusLine.setText(text);
                        statusLine.pack(true);
                        SigninStatusBox.this.pack(true);
                    } else {
                        statusLine.setText("");
                    }
                }
            }
        });
    }
}
