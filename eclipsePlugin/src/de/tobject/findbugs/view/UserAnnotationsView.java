/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.view;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.reporter.MarkerUtil.BugCollectionAndInstance;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.CloudListener;
import edu.umd.cs.findbugs.cloud.Cloud.CloudTask;

/**
 * View which shows bug annotations.
 *
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 19.04.2004
 */
public class UserAnnotationsView extends AbstractFindbugsView {

    private String userAnnotation;

    private String firstVersionText;

    private String cloudText;

    private @CheckForNull
    BugCollectionAndInstance theBug;

    private Text userAnnotationTextField;

    private Text cloudTextField;

    private Label firstVersionLabel;

    private Combo designationComboBox;

    private SigninStatusBox signinStatusBox;

    private ISelectionListener selectionListener;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private Cloud lastCloud;

    private final CloudListener cloudListener = new CloudListener() {
        public void statusUpdated() {
            // noop
        }

        public void issueUpdated(BugInstance bug) {
            BugCollectionAndInstance bug2 = theBug;
            if (bug2 != null && bug.equals(bug2.getBugInstance())) {
                updateBugInfo();
            }
        }

        public void taskStarted(CloudTask task) {
            // noop
        }
    };

    private IWorkbenchPart contributingPart;

    public UserAnnotationsView() {
        userAnnotation = "";
        firstVersionText = "";
        cloudText = "";
    }

    @Override
    public Composite createRootControl(Composite parent) {
        Composite main = new Composite(parent, SWT.VERTICAL);
        main.setLayout(new GridLayout(2, false));

        // main.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));
        designationComboBox = new Combo(main, SWT.LEFT | SWT.DROP_DOWN | SWT.READ_ONLY);
        designationComboBox.setToolTipText("My bug designation");
        designationComboBox.setLayoutData(new GridData());
        for (String s : I18N.instance().getUserDesignationKeys(true)) {
            designationComboBox.add(I18N.instance().getUserDesignation(s));
        }
        designationComboBox.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                final BugCollectionAndInstance bug = theBug;
                if (bug == null) {
                    return;
                }
                final BugInstance bugInstance = bug.getBugInstance();
                if (bugInstance == null) {
                    return;
                }
                final int selectionIndex = designationComboBox.getSelectionIndex();
                executor.submit(new Runnable() {
                    public void run() {
                        bugInstance.setUserDesignationKeyIndex(selectionIndex, bug.getBugCollection());
                    }
                });
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        // designationComboBox.setSize(designationComboBox.computeSize(
        // SWT.DEFAULT, SWT.DEFAULT));
        designationComboBox.setEnabled(false);

        signinStatusBox = new SigninStatusBox(main, SWT.NONE);
        signinStatusBox.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));

        firstVersionLabel = new Label(main, SWT.LEFT);
        firstVersionLabel.setToolTipText("The earliest version in which the bug was present");
        firstVersionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

        userAnnotationTextField = new Text(main, SWT.LEFT | SWT.WRAP | SWT.BORDER);
        userAnnotationTextField.setToolTipText("My bug comments");
        userAnnotationTextField.setEnabled(false);
        GridData uatfData = new GridData(GridData.FILL_BOTH);
        uatfData.horizontalSpan = 2;
        userAnnotationTextField.setLayoutData(uatfData);
        userAnnotationTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                final BugCollectionAndInstance bug = theBug;
                if (bug != null && bug.getBugInstance() != null) {
                    final String txt = userAnnotationTextField.getText();
                    executor.submit(new Runnable() {
                        public void run() {
                            bug.getBugInstance().setAnnotationText(txt, bug.getBugCollection());
                        }
                    });
                }
            }
        });
        Label cloudLabel = new Label(main, SWT.LEFT);
        cloudLabel.setText("Cloud info:");
        GridData lData = new GridData(SWT.LEFT, SWT.TOP, true, false);
        lData.horizontalSpan = 2;
        cloudLabel.setLayoutData(lData);

        cloudTextField = new Text(main, SWT.LEFT | SWT.WRAP | SWT.BORDER | SWT.READ_ONLY);
        cloudTextField.setToolTipText("This information about this issue was retrieved from the cloud");
        GridData ctfData = new GridData(GridData.FILL_BOTH);
        ctfData.horizontalSpan = 2;
        cloudTextField.setLayoutData(ctfData);

        // Add selection listener to detect click in problems view or bug tree
        // view
        ISelectionService theService = getSite().getWorkbenchWindow().getSelectionService();
        selectionListener = new MarkerSelectionListener(this);
        theService.addSelectionListener(selectionListener);
        return main;
    }

    @Override
    public void dispose() {
        if (selectionListener != null) {
            getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
            selectionListener = null;
        }
        if (lastCloud != null) {
            lastCloud.removeListener(cloudListener);
        }
        signinStatusBox.dispose();
        contributingPart = null;
        super.dispose();
    }

    /**
     * Updates the control using the current window size and the contents of the
     * title and description fields.
     */
    private void updateDisplay() {
        firstVersionLabel.setSize(firstVersionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    /**
     * Set the content to be displayed
     */
    public void setContent(BugCollectionAndInstance bci) {
        this.theBug = bci;
        updateBugInfo();
    }

    private void updateBugInfo() {
        BugCollectionAndInstance theBug2 = theBug;
        if (theBug2 == null) {
            setCloud(null);
            this.userAnnotationTextField.setEnabled(false);
            this.designationComboBox.setEnabled(false);
            this.userAnnotation = "";
            this.firstVersionText = "";
            this.cloudText = "";

        } else {

            BugInstance bug = theBug2.getBugInstance();
            long timestamp = theBug2.getBugCollection().getAppVersionFromSequenceNumber(bug.getFirstVersion()).getTimestamp();

            String firstVersion = "First seen: " + convertTimestamp(timestamp);

            Cloud cloud = theBug2.getBugCollection().getCloud();

            setCloud(cloud);
            if (cloud.canStoreUserAnnotation(bug)) {

                String userDesignation = cloud.getUserEvaluation(bug);
                this.userAnnotation = (userDesignation == null) ? "" : userDesignation.trim();
                this.firstVersionText = firstVersion.trim();
                this.cloudText = cloud.getCloudReport(bug);
                this.userAnnotationTextField.setEnabled(true);
                this.designationComboBox.setEnabled(true);

                int comboIndex = bug.getUserDesignationKeyIndex();
                if (comboIndex == -1) {
                    FindbugsPlugin.getDefault().logError("Cannot find user designation");
                } else {
                    designationComboBox.select(comboIndex);
                }

            } else {
                this.userAnnotationTextField.setEnabled(false);
                this.designationComboBox.setEnabled(false);
                this.userAnnotation = "";
                this.firstVersionText = "";
                this.cloudText = "";
            }
        }
        userAnnotationTextField.setText(userAnnotation);
        firstVersionLabel.setText(firstVersionText);
        cloudTextField.setText(cloudText);
        updateDisplay();
    }

    private void setCloud(Cloud cloud) {
        signinStatusBox.setCloud(cloud);
        if (cloud != lastCloud) {
            if (lastCloud != null) {
                lastCloud.removeListener(cloudListener);
            }
            if (cloud != null) {
                cloud.addListener(cloudListener);
            }
            lastCloud = cloud;
        }
    }

    /**
     * Show the details of a FindBugs marker in the view. Brings the view to the
     * foreground.
     *
     * @param thePart
     *
     * @param marker
     *            may be null, or marker containing the bug pattern to show
     *            details for
     */
    private void showInView(IMarker marker) {
        BugCollectionAndInstance bci = marker == null ? null : MarkerUtil.findBugCollectionAndInstanceForMarker(marker);
        setContent(bci);
    }

    @Override
    public void markerSelected(IWorkbenchPart thePart, IMarker newMarker) {
        contributingPart = thePart;
        showInView(newMarker);
        if (!isVisible()) {
            activate();
        }
    }

    private static String convertTimestamp(long timestamp) {
        if (timestamp == -2) {
            return "ERROR - Timestamp not found";
        }
        if (timestamp == -1) {
            return "First version analyzed";
        }
        Calendar theCalendar = Calendar.getInstance();
        theCalendar.setTimeInMillis(System.currentTimeMillis());
        theCalendar.set(theCalendar.get(Calendar.YEAR), theCalendar.get(Calendar.MONTH), theCalendar.get(Calendar.DATE), 0, 0, 0);
        long beginningOfToday = theCalendar.getTimeInMillis();
        long beginningOfYesterday = beginningOfToday - 86400000;
        theCalendar.setTimeInMillis(timestamp);
        String timeString = theCalendar.getTime().toString();
        if (timestamp >= beginningOfToday) {
            return "Today " + timeString.substring(timeString.indexOf(":") - 2, timeString.indexOf(":") + 3);
        } else if (timestamp >= beginningOfYesterday) {
            return "Yesterday " + timeString.substring(timeString.indexOf(":") - 2, timeString.indexOf(":") + 3);
        } else {
            return timeString.substring(0, timeString.indexOf(":") + 3);
        }
    }

    @Override
    public void setFocus() {
        designationComboBox.setFocus();
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        // noop
    }

    public IWorkbenchPart getContributingPart() {
        return contributingPart;
    }
}
