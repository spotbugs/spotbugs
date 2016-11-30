/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package edu.umd.cs.findbugs;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;

import edu.umd.cs.findbugs.cloud.Cloud;

/**
 * Interface for any kind of GUI attached to the current FindBug analysis
 *
 * @author Andrei
 */
public interface IGuiCallback {
    /** If true, do not open windows or browsers */
    boolean isHeadless();

    void showMessageDialog(String message);

    public final static int YES_OPTION = 0;

    public final static int NO_OPTION = 1;

    public final static int CANCEL_OPTION = 2;

    public static final int YES_NO_OPTION = 0;

    public static final int YES_NO_CANCEL_OPTION = 1;

    public static final int OK_CANCEL_OPTION = 2;

    void invokeInGUIThread(Runnable r);

    int showConfirmDialog(String message, String title, String ok, String cancel);

    String showQuestionDialog(String message, String title, String defaultValue);

    List<String> showForm(String message, String title, List<FormItem> labels);

    InputStream getProgressMonitorInputStream(InputStream in, int length, String msg);

    void setErrorMessage(String errorMsg);

    void displayNonmodelMessage(String title, String message);

    boolean showDocument(URL u);

    /**
     * Called as soon as the cloud object is created, before it is initialized.
     * Useful for adding status msg listener.
     */
    void registerCloud(Project project, BugCollection collection, Cloud cloud);

    void unregisterCloud(Project project, BugCollection collection, Cloud cloud);

    /**
     * Use this executor to queue bug collection updates without interfering
     * with the GUI. Runs on the AWT event thread.
     */
    ExecutorService getBugUpdateExecutor();

    void showMessageDialogAndWait(String message) throws InterruptedException;

    public class FormItem {
        private final String label;

        private final String defaultValue;

        private boolean password = false;

        private final List<String> possibleValues;

        private JComponent field;

        private String currentValue;

        private List<FormItem> items;

        public FormItem(String label) {
            this(label, null, null);
        }

        public FormItem(String label, String defaultValue) {
            this(label, defaultValue, null);
        }

        public FormItem(String label, String defaultValue, List<String> possibleValues) {
            this.label = label;
            this.defaultValue = defaultValue;
            this.possibleValues = possibleValues;
        }

        public FormItem password() {
            password = true;
            return this;
        }

        public boolean isPassword() {
            return password;
        }

        public String getLabel() {
            return label;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public List<String> getPossibleValues() {
            return possibleValues;
        }

        public JComponent getField() {
            return field;
        }

        public void setField(JComponent field) {
            this.field = field;
        }

        public void setItems(List<FormItem> items) {
            this.items = items;
        }

        public List<FormItem> getItems() {
            return items;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void updated() {
        }
    }
}
