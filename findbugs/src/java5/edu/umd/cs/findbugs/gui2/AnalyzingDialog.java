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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

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
	
	public AnalyzingDialog(@NonNull final Project project)
	{
		this(project, new AnalysisCallback()
		{
			public void analysisFinished(BugSet results)
				{
					ProjectSettings.newInstance();
					((BugTreeModel)MainFrame.getInstance().getTree().getModel()).getOffListenerList();
					MainFrame.getInstance().getTree().setModel(new BugTreeModel(MainFrame.getInstance().getTree(), MainFrame.getInstance().getSorter(), results));
					MainFrame.getInstance().setProject(project);
				}
			
			public void analysisInterrupted() {}
		}, false);
	}
	
	
	/**
	 * 
	 * @param project The Project to analyze
	 * @param callback contains what to do if the analysis is interrupted and what to do if it finishes normally
	 * @param joinThread Whether or not this constructor should return before the analysis is complete.  If true, the constructor does not return until the analysis is either finished or interrupted.
	 */
	public AnalyzingDialog(@NonNull Project project, AnalysisCallback callback, boolean joinThread)
	{
		this.project = project;
		this.callback = callback;
		initComponents();
		analysisThread.start();
		if (joinThread)
			try {analysisThread.join();} catch (InterruptedException e) {}
	}
	
	public void initComponents()
	{
		statusLabel = new JLabel(" ");
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		cancelButton = new JButton("Cancel");
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
				setModal(false);
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
		updateStage("Finishing analysis...");
	}

	public void reportNumberOfArchives(int numArchives)
	{
		updateStage("Scanning archives...");
		updateCount(0, numArchives);
	}

	public void startAnalysis(int numClasses)
	{
		updateStage("Analyzing classes...");
		updateCount(0, numClasses);
	}

	private class AnalysisThread extends Thread
	{
		{
			// Give the analysis thread lower priority than the UI
			setPriority(NORM_PRIORITY - 1);
		}
		
		public void run()
		{
			BugSet data = BugLoader.doAnalysis(project, AnalyzingDialog.this);
			if (data == null) // We were interrupted
			{
				callback.analysisInterrupted();
				return;
			}
			analysisFinished = true;
			AnalyzingDialog.this.dispose();
			callback.analysisFinished(data);
			MainFrame.getInstance().newProject();
		}
	}
}