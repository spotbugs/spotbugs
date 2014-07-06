/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.Project;


@SuppressWarnings("serial")
// Note: Don't remove the final, if anyone extends this class, bad things could
// happen, since a thread is started in this class's constructor.
/**
 *Creating an instance of this class runs a FindBugs analysis, and pops up a nice progress window
 */
public final class AnalyzingDialog extends FBDialog implements FindBugsProgress {
    private volatile boolean analysisFinished = false;

    @Nonnull
    private final Project project;

    private final AnalysisCallback callback;

    private final AnalysisThread analysisThread = new AnalysisThread();

    private int count;

    private int goal;

    private final JLabel statusLabel;

    private final JProgressBar progressBar;

    private final JButton cancelButton;

    public static void show(@Nonnull final Project project) {
        AnalysisCallback callback = new AnalysisCallback() {
            @Override
            public void analysisFinished(BugCollection results) {
                MainFrame instance = MainFrame.getInstance();
                assert results.getProject() == project;
                instance.setBugCollection(results);
                try {
                    instance.releaseDisplayWait();
                } catch (Exception e) {
                    Logger.getLogger(AnalyzingDialog.class.getName()).log(Level.FINE, "", e);
                }
                results.reinitializeCloud();
            }

            @Override
            public void analysisInterrupted() {
                MainFrame instance = MainFrame.getInstance();
                instance.updateProjectAndBugCollection(null);
                instance.releaseDisplayWait();
            }
        };
        show(project, callback, false);

    }

    /**
     *
     * @param project
     *            The Project to analyze
     * @param callback
     *            contains what to do if the analysis is interrupted and what to
     *            do if it finishes normally
     * @param joinThread
     *            Whether or not this constructor should return before the
     *            analysis is complete. If true, the constructor does not return
     *            until the analysis is either finished or interrupted.
     */

    public static void show(@Nonnull
            Project project, AnalysisCallback callback, boolean joinThread) {
        AnalyzingDialog dialog = new AnalyzingDialog(project, callback, joinThread);
        MainFrame.getInstance().acquireDisplayWait();
        try {
            dialog.analysisThread.start();
            if (joinThread) {
                try {
                    dialog.analysisThread.join();
                } catch (InterruptedException e) {
                }
            }
        } finally {
            if (joinThread) {
                MainFrame.getInstance().releaseDisplayWait();
            }
        }
    }



    /**
     *
     * @param project
     *            The Project to analyze
     * @param callback
     *            contains what to do if the analysis is interrupted and what to
     *            do if it finishes normally
     * @param joinThread
     *            Whether or not this constructor should return before the
     *            analysis is complete. If true, the constructor does not return
     *            until the analysis is either finished or interrupted.
     */
    private AnalyzingDialog(@Nonnull Project project, AnalysisCallback callback, boolean joinThread) {
        if (project == null) {
            throw new NullPointerException("null project");
        }
        this.project = project;
        this.callback = callback;
        statusLabel = new JLabel(" ");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                cancel();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent evt) {
                cancel();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
                add(statusLabel);
                add(progressBar);
                add(cancelButton);
                statusLabel.setAlignmentX(CENTER_ALIGNMENT);
                progressBar.setAlignmentX(CENTER_ALIGNMENT);
                cancelButton.setAlignmentX(CENTER_ALIGNMENT);
                pack();
                setSize(300, getHeight());
                setLocationRelativeTo(MainFrame.getInstance());
                setResizable(false);
                setModal(true);// Why was this set to false before?
                try {
                    setVisible(true);
                } catch (Throwable e) {
                    AnalyzingDialog.this.project.getGuiCallback().showMessageDialog("ERROR DURING ANALYSIS:\n\n"
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        });

    }

    private void cancel() {
        if (!analysisFinished) {
            analysisThread.interrupt();
            setVisible(false);
            // TODO there should be a call to dispose() here, but it seems to
            // cause repainting issues
        }
    }

    private void updateStage(String stage) {
        statusLabel.setText(stage);
    }

    private void incrementCount() {
        count++;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setString(count + "/" + goal);
                progressBar.setValue(count);
            }
        });
    }

    private void updateCount(final int count, final int goal) {
        this.count = count;
        this.goal = goal;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setString(count + "/" + goal);
                progressBar.setValue(count);
                progressBar.setMaximum(goal);
            }
        });
    }

    @Override
    public void finishArchive() {
        incrementCount();
    }

    @Override
    public void finishClass() {
        incrementCount();
    }

    @Override
    public void finishPerClassAnalysis() {
        updateStage(edu.umd.cs.findbugs.L10N.getLocalString("progress.finishing_analysis", "Finishing analysis..."));
    }

    @Override
    public void reportNumberOfArchives(int numArchives) {
        updateStage(edu.umd.cs.findbugs.L10N.getLocalString("progress.scanning_archives", "Scanning archives..."));
        updateCount(0, numArchives);
    }

    int pass = 0;

    @Override
    public void startAnalysis(int numClasses) {
        pass++;
        String localString = edu.umd.cs.findbugs.L10N.getLocalString("progress.analyzing_classes", "Analyzing classes...");
        updateStage(localString + ", pass " + pass + "/" + classesPerPass.length);
        updateCount(0, numClasses);
    }

    private class AnalysisThread extends Thread {
        {
            // Give the analysis thread its (possibly user-defined) priority.
            // The default is a slightly lower priority than the UI.
            setPriority(Driver.getPriority());
            setName("Analysis Thread");
        }

        @Override
        public void run() {
            if (project == null) {
                throw new NullPointerException("null project");
            }

            BugCollection data;
            try {
                data = BugLoader.doAnalysis(project, AnalyzingDialog.this);
            } catch (InterruptedException e) {
                callback.analysisInterrupted();
                // We don't have to clean up the dialog because the
                // cancel button handler does this already.
                return;
            } catch (IOException e) {
                Logger.getLogger(AnalyzingDialog.class.getName()).log(Level.WARNING, "IO Error while performing analysis", e);
                callback.analysisInterrupted();
                scheduleDialogCleanup();
                scheduleErrorDialog("Analysis failed", e.getClass().getSimpleName()  + ": " + e.getMessage());
                return;
            } catch (Throwable e) {
                callback.analysisInterrupted();
                scheduleDialogCleanup();
                scheduleErrorDialog("Analysis failed", e.getClass().getSimpleName()  + ": " + e.getMessage());
                return;
            }

            // Analysis succeeded
            analysisFinished = true;
            scheduleDialogCleanup();
            callback.analysisFinished(data);
            MainFrame.getInstance().newProject();
        }

        private void scheduleDialogCleanup() {
            SwingUtilities.invokeLater(new Runnable() {
                /*
                 * (non-Javadoc)
                 *
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                    AnalyzingDialog.this.setVisible(false);
                }
            });
        }

        private void scheduleErrorDialog(final String title, final String message) {
            SwingUtilities.invokeLater(new Runnable() {
                /*
                 * (non-Javadoc)
                 *
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), message, title, JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    int[] classesPerPass;

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.FindBugsProgress#predictPassCount(int[])
     */
    @Override
    public void predictPassCount(int[] classesPerPass) {
        this.classesPerPass = classesPerPass;

    }

    @Override
    public void startArchive(String name) {
        // noop
    }
}
