/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

/*
 * RunAnalysisDialog.java
 *
 * Created on April 1, 2003, 3:22 PM
 */

package edu.umd.cs.findbugs.gui;

import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.FindBugsProgress;

/**
 * A modal dialog to run the actual FindBugs analysis on a project.
 * The analysis is done in a separate thread, so that the GUI can
 * still stay current while the analysis is running.  We provide support
 * for reporting the progress of the analysis, and for asynchronously
 * cancelling the analysis before it completes.
 *
 * @author David Hovemeyer
 */
public class RunAnalysisDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = 1L;

	private class RunAnalysisProgress implements FindBugsProgress {
		private int goal, count;

		private synchronized int getGoal() {
			return goal;
		}

		private synchronized int getCount() {
			return count;
		}

		public void reportNumberOfArchives(final int numArchives) {
			beginStage(L10N.getLocalString("msg.scanningarchives_txt", "Scanning archives"), numArchives);
		}

		public void finishArchive() {
			step();
		}

		public void startAnalysis(int numClasses) {
			beginStage(L10N.getLocalString("msg.analysingclasses_txt", "Analyzing classes"), numClasses);
		}

		public void finishClass() {
			step();
		}

		public void finishPerClassAnalysis() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					stageNameLabel.setText(L10N.getLocalString("msg.finishedanalysis_txt", "Finishing analysis"));
				}
			});
		}

		private void beginStage(final String stageName, final int goal) {
			synchronized (this) {
				this.count = 0;
				this.goal = goal;
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int goal = getGoal();
					stageNameLabel.setText(stageName);
					countValueLabel.setText("0/" + goal);
					progressBar.setMaximum(goal);
					progressBar.setValue(0);
				}
			});
		}

		private void step() {
			synchronized (this) {
				count++;
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int count = getCount();
					int goal = getGoal();
					countValueLabel.setText(count + "/" + goal);
					progressBar.setValue(count);
				}
			});
		}

	}

	private final AnalysisRun analysisRun;
	private Thread analysisThread;
	private boolean completed;
	private Exception fatalException;

	/**
	 * Creates new form RunAnalysisDialog
	 */
	public RunAnalysisDialog(java.awt.Frame parent, AnalysisRun analysisRun_) {
		super(parent, true);
		initComponents();
		this.analysisRun = analysisRun_;
		this.completed = false;

		// Create a progress callback to give the user feedback
		// about how far along we are.
		final FindBugsProgress progress = new RunAnalysisProgress();

		// This is the thread that will actually run the analysis.
		this.analysisThread = new Thread() {
			public void run() {
				try {
					analysisRun.execute(progress);
					setCompleted(true);
				} catch (java.io.IOException e) {
					setException(e);
				} catch (InterruptedException e) {
					// We don't need to do anything here.
					// The completed flag is not set, so the frame
					// will know that the analysis did not complete.
				} catch (Exception e) {
					setException(e);
				}

				// Send a message to the dialog that it should close
				// That way, it goes away without any need for user intervention
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						closeDialog(new WindowEvent(RunAnalysisDialog.this, WindowEvent.WINDOW_CLOSING));
					}
				});
			}
		};
	}

	public synchronized void setCompleted(boolean completed) {
		this.completed = completed;
	}

	/**
	 * The creator of the dialog may call this method to find out whether
	 * or not the analysis completed normally.
	 */
	public synchronized boolean isCompleted() {
		return completed;
	}

	public synchronized void setException(Exception e) {
		fatalException = e;
	}

	/**
	 * Determine whether or not a fatal exception occurred
	 * during analysis.
	 */
	public synchronized boolean exceptionOccurred() {
		return fatalException != null;
	}

	/**
	 * Get the exception that abnormally terminated the analysis.
	 */
	public synchronized Exception getException() {
		return fatalException;
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents() {//GEN-BEGIN:initComponents
		java.awt.GridBagConstraints gridBagConstraints;

		findBugsLabel = new javax.swing.JLabel();
		countLabel = new javax.swing.JLabel();
		progressLabel = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();
		cancelButton = new javax.swing.JButton();
		jSeparator1 = new javax.swing.JSeparator();
		stageLabel = new javax.swing.JLabel();
		stageNameLabel = new javax.swing.JLabel();
		topVerticalFiller = new javax.swing.JLabel();
		bottomVerticalFiller = new javax.swing.JLabel();
		countValueLabel = new javax.swing.JLabel();

		getContentPane().setLayout(new java.awt.GridBagLayout());

		setTitle("Run Analysis");
		this.setTitle(L10N.getLocalString("dlg.runanalysis_ttl", "Run Analysis"));
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog(evt);
			}
			public void windowOpened(java.awt.event.WindowEvent evt) {
				formWindowOpened(evt);
			}
		});

		findBugsLabel.setBackground(new java.awt.Color(0, 0, 204));
		findBugsLabel.setFont(new java.awt.Font("Dialog", 1, 24));
		findBugsLabel.setForeground(new java.awt.Color(255, 255, 255));
		findBugsLabel.setText("Find Bugs!");
		findBugsLabel.setOpaque(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 0);
		getContentPane().add(findBugsLabel, gridBagConstraints);

		countLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		countLabel.setText("Count:");
		countLabel.setText(L10N.getLocalString("dlg.count_lbl", "Count:"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		getContentPane().add(countLabel, gridBagConstraints);

		progressLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		progressLabel.setText("Progress:");
		progressLabel.setText(L10N.getLocalString("dlg.progress_lbl", "Progress:"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		getContentPane().add(progressLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
		getContentPane().add(progressBar, gridBagConstraints);

		cancelButton.setFont(new java.awt.Font("Dialog", 0, 12));
		cancelButton.setText("Cancel");
		cancelButton.setText(L10N.getLocalString("dlg.cancel_btn", "Cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
		getContentPane().add(cancelButton, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		getContentPane().add(jSeparator1, gridBagConstraints);

		stageLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		stageLabel.setText("Stage:");
		stageLabel.setText(L10N.getLocalString("dlg.stage_lbl", "Stage:"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
		getContentPane().add(stageLabel, gridBagConstraints);

		stageNameLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		getContentPane().add(stageNameLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.weighty = 0.5;
		getContentPane().add(topVerticalFiller, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.weighty = 0.5;
		getContentPane().add(bottomVerticalFiller, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
		getContentPane().add(countValueLabel, gridBagConstraints);

		pack();
	}//GEN-END:initComponents

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		int option = JOptionPane.showConfirmDialog(this, L10N.getLocalString("msg.cancelanalysis_txt", "Cancel analysis?"), L10N.getLocalString("msg.analyze_txt", "Analysis"),
		        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

		if (option == JOptionPane.YES_OPTION) {
			// All we need to do to cancel the analysis is to interrupt
			// the analysis thread.
			analysisThread.interrupt();
		}
	}//GEN-LAST:event_cancelButtonActionPerformed

	private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
		// Here is where we actually kick off the analysis thread.
		
		// Lower the priority of the analysis thread to leave more
		// CPU for interactive tasks.
		analysisThread.setPriority(Thread.NORM_PRIORITY - 1);
		
		analysisThread.start();
	}//GEN-LAST:event_formWindowOpened

	/**
	 * Closes the dialog
	 */
	private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel bottomVerticalFiller;
	private javax.swing.JButton cancelButton;
	private javax.swing.JLabel countLabel;
	private javax.swing.JLabel countValueLabel;
	private javax.swing.JLabel findBugsLabel;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JLabel progressLabel;
	private javax.swing.JLabel stageLabel;
	private javax.swing.JLabel stageNameLabel;
	private javax.swing.JLabel topVerticalFiller;
	// End of variables declaration//GEN-END:variables

}
