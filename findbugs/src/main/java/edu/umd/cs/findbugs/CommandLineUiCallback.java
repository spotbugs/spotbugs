/*
 * Contributions to FindBugs
 * Copyright (C) 2009, University of Maryland
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.charsets.UserTextFile;
import edu.umd.cs.findbugs.cloud.Cloud;

/**
 * Implementation of the UI callback for command line sessions.
 *
 * @author andy.st
 */
public class CommandLineUiCallback implements IGuiCallback {
    private final CurrentThreadExecutorService bugUpdateExecutor = new CurrentThreadExecutorService();

    public CommandLineUiCallback() {
    }
    BufferedReader br = UserTextFile.bufferedReader(System.in);

    @Override
    public void showMessageDialogAndWait(String message) throws InterruptedException {
        System.out.println(message);
    }

    @Override
    public void showMessageDialog(String message) {
        System.out.println(message);
    }

    @Override
    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        String confirmStr = "Yes (Y) or No (N)?";

        System.out.println(String.format("Confirmation required: %s%n\t%s%n\t%s", title, message, confirmStr));
        String answer = null;
        while (true) {
            try {
                answer = br.readLine();
            } catch (IOException ioe) {
                throw new IllegalArgumentException("IO error trying to read System.in!");
            }
            int response = parseAnswer(answer);
            if (response < 0) {
                System.out.println(String.format("\t%s", confirmStr));
            } else {
                return response;
            }
        }
    }

    private int parseAnswer(String answer) {
        if (null == answer || answer.length() == 0) {
            System.out.println("You entered an empty string");

            return -1;
        }
        char option = answer.toLowerCase(Locale.ENGLISH).charAt(0);
        switch (option) {
        case 'o':
            return JOptionPane.OK_OPTION;
        case 'y':
            return JOptionPane.YES_OPTION;
        case 'n':
            return JOptionPane.NO_OPTION;
        case 'c':
            return JOptionPane.CANCEL_OPTION;
        default:
            System.out.println("You entered '" + option +"'");
            return -1;
        }
    }

    @Override
    public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
        return in;
    }

    @Override
    public void setErrorMessage(String errorMsg) {
        System.err.println(errorMsg);
    }

    @Override
    public void displayNonmodelMessage(String title, String message) {
        System.out.println(String.format("Message: %s%n%s", title, message));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.IGuiCallback#showQuestionDialog(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public String showQuestionDialog(String message, String title, String defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> showForm(String message, String title, List<FormItem> labels) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.IGuiCallback#showDocument(java.net.URL)
     */
    @Override
    public boolean showDocument(URL u) {
        return false;
    }

    @Override
    public void registerCloud(Project project, BugCollection collection, Cloud cloud) {
    }

    @Override
    public ExecutorService getBugUpdateExecutor() {
        return bugUpdateExecutor;
    }

    private static class CurrentThreadExecutorService extends AbstractExecutorService {
        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
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
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.IGuiCallback#isHeadless()
     */
    @Override
    public boolean isHeadless() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.IGuiCallback#unregisterCloud(edu.umd.cs.findbugs.
     * Project, edu.umd.cs.findbugs.BugCollection,
     * edu.umd.cs.findbugs.cloud.Cloud)
     */
    @Override
    public void unregisterCloud(Project project, BugCollection collection, Cloud cloud) {

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.IGuiCallback#invokeInGUIThread(java.lang.Runnable)
     */
    @Override
    public void invokeInGUIThread(Runnable r) {
        throw new UnsupportedOperationException();

    }
}
