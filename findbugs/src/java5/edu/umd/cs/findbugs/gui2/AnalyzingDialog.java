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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;

import sun.swing.SwingUtilities2;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.FindBugsProgress;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.annotations.NonNull;

@SuppressWarnings("serial")
//Note:  Don't remove the final, if anyone extends this class, bad things could happen, since a thread is started in this class's constructor.  
/**
 *Creating an instance of this class runs a FindBugs analysis, and pops up a nice progress window  
 */
public final class AnalyzingDialog extends FBDialog implements FindBugsProgress
{
	private volatile boolean analysisFinished = false;
	@NonNull private Project project;
	private AnalysisCallback callback;
	private AnalysisThread analysisThread = new AnalysisThread();

	private int count;
	private int goal;

	private JLabel statusLabel;
	private JProgressBar progressBar;
	private JButton cancelButton;

	public AnalyzingDialog(@NonNull final Project project, final boolean changeSettings)
	{
		this(project, new AnalysisCallback()
		{
			public void analysisFinished(BugCollection results)
			{
				if (changeSettings)
					ProjectSettings.newInstance();
				MainFrame instance = MainFrame.getInstance();
				instance.setProjectAndBugCollection(project, results);
			}

			public void analysisInterrupted() {}
		}, /*false*/true); // XXX - DHH, 7/13/2007 - why was this set to false?
	}


	/**
	 * 
	 * @param project The Project to analyze
	 * @param callback contains what to do if the analysis is interrupted and what to do if it finishes normally
	 * @param joinThread Whether or not this constructor should return before the analysis is complete.  If true, the constructor does not return until the analysis is either finished or interrupted.
	 */
	public AnalyzingDialog(@NonNull Project project, AnalysisCallback callback, boolean joinThread)
	{
		if (project == null) throw new NullPointerException("null project");
		this.project = project;
		this.callback = callback;
		initComponents();
		MainFrame.getInstance().showWaitCard();
		analysisThread.start();
		if (joinThread)
			try {analysisThread.join();} catch (InterruptedException e) {}
	}

	public void initComponents()
	{
		statusLabel = new JLabel(" ");
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				cancel();
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent evt)
			{
				cancel();
			}
		});

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
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
				setModal(true);//Why was this set to false before?
				setVisible(true);
			}
		});		
	}

	private void cancel()
	{
		if (!analysisFinished)
		{
			analysisThread.interrupt();
			setVisible(false);
			// TODO there should be a call to dispose() here, but it seems to cause repainting issues
		}
	}

	private void updateStage(String stage)
	{
		statusLabel.setText(stage);
	}

	private void incrementCount()
	{
		count++;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progressBar.setString(count + "/" + goal);
				progressBar.setValue(count);
			}
		});
	}

	private void updateCount(final int count, final int goal)
	{
		this.count = count;
		this.goal = goal;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				progressBar.setString(count + "/" + goal);
				progressBar.setValue(count);
				progressBar.setMaximum(goal);
			}
		});
	}

	public void finishArchive()
	{
		incrementCount();
	}

	public void finishClass()
	{
		incrementCount();
	}

	public void finishPerClassAnalysis()
	{
		updateStage(edu.umd.cs.findbugs.L10N.getLocalString("progress.finishing_analysis", "Finishing analysis..."));
	}

	public void reportNumberOfArchives(int numArchives)
	{
		updateStage(edu.umd.cs.findbugs.L10N.getLocalString("progress.scanning_archives", "Scanning archives..."));
		updateCount(0, numArchives);
	}

	public void startAnalysis(int numClasses)
	{
		updateStage(edu.umd.cs.findbugs.L10N.getLocalString("progress.analyzing_classes", "Analyzing classes..."));
		updateCount(0, numClasses);
	}

	private class AnalysisThread extends Thread
	{
		{
			// Give the analysis thread lower priority than the UI
			setPriority(NORM_PRIORITY - 1);
		}

		@Override
		public void run()
		{
			if (project == null) throw new NullPointerException("null project");

			BugCollection data;
			try {
				data = BugLoader.doAnalysis(project, AnalyzingDialog.this);
			} catch (InterruptedException e) {
				callback.analysisInterrupted();
				// We don't have to clean up the dialog because the
				// cancel button handler does this already.
				return;
			} catch (IOException e) {
				callback.analysisInterrupted();
				scheduleDialogCleanup();
				scheduleErrorDialog("Analysis failed", e.getMessage());
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
				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					AnalyzingDialog.this.setVisible(false);
				}
			});
		}

		private void scheduleErrorDialog(final String title, final String message) {
			SwingUtilities.invokeLater(new Runnable() {
				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					JOptionPane.showMessageDialog(
							MainFrame.getInstance(),
							message,
							title,
							JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.FindBugsProgress#predictPassCount(int[])
	 */
	public void predictPassCount(int[] classesPerPass) {
		// TODO Auto-generated method stub

	}
}
